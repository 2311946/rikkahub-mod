package me.rerere.rikkahub.service

import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.rerere.ai.core.MessageRole
import me.rerere.ai.ui.UIMessage
import me.rerere.ai.ui.UIMessagePart
import me.rerere.rikkahub.AppScope
import me.rerere.rikkahub.data.ai.GenerationChunk
import me.rerere.rikkahub.data.ai.GenerationLoop
import me.rerere.rikkahub.data.datastore.SettingsStore
import me.rerere.rikkahub.data.datastore.findModelById
import me.rerere.rikkahub.data.datastore.getAssistantById
import me.rerere.rikkahub.data.datastore.getCurrentChatModel
import me.rerere.rikkahub.data.model.GroupActivationStrategy
import me.rerere.rikkahub.data.model.GroupChat
import me.rerere.rikkahub.data.model.GroupMessage
import me.rerere.rikkahub.data.model.GroupSpeakerSelector
import me.rerere.rikkahub.data.repository.GroupChatRepository
import me.rerere.rikkahub.data.repository.MemoryRepository
import kotlin.random.Random
import kotlin.uuid.Uuid

private const val TAG = "GroupChatService"

class GroupChatService(
    private val appScope: AppScope,
    private val settingsStore: SettingsStore,
    private val groupChatRepository: GroupChatRepository,
    private val generationLoop: GenerationLoop,
    private val memoryRepository: MemoryRepository,
) {
    data class GeneratingInfo(
        val isGenerating: Boolean = false,
        val currentSpeaker: String = "",
        val streamingContent: String = "",
    )

    private val _generatingState = MutableStateFlow<Map<Uuid, GeneratingInfo>>(emptyMap())
    val generatingState: StateFlow<Map<Uuid, GeneratingInfo>> = _generatingState.asStateFlow()

    private val activeJobs = mutableMapOf<Uuid, Job>()

    fun sendMessage(groupChatId: Uuid, text: String) {
        activeJobs[groupChatId]?.cancel()
        val job = appScope.launch {
            val groupChat = groupChatRepository.getGroupChat(groupChatId) ?: return@launch
            val settings = settingsStore.settingsFlow.value

            val userMessage = GroupMessage(
                content = text,
                isUser = true,
                speakerName = "You",
            )
            groupChatRepository.insertMessage(groupChatId, userMessage)

            val allMembers: List<GroupSpeakerSelector.MemberInfo>
            val enabledMembers: List<GroupSpeakerSelector.MemberInfo>

            if (groupChat.personas.isNotEmpty()) {
                allMembers = groupChat.personas.map { persona ->
                    GroupSpeakerSelector.MemberInfo(
                        id = persona.id,
                        name = persona.name,
                        talkativeness = persona.talkativeness,
                        assistantId = persona.assistantId,
                    )
                }
                enabledMembers = groupChat.personas
                    .filter { it.enabled }
                    .map { persona ->
                        GroupSpeakerSelector.MemberInfo(
                            id = persona.id,
                            name = persona.name,
                            talkativeness = persona.talkativeness,
                            assistantId = persona.assistantId,
                        )
                    }
            } else {
                allMembers = groupChat.memberIds.mapNotNull { memberId ->
                    val assistant = settings.assistants.find { it.id == memberId }
                    assistant?.let {
                        GroupSpeakerSelector.MemberInfo(
                            id = it.id,
                            name = it.name,
                            talkativeness = it.talkativeness,
                            assistantId = it.id,
                        )
                    }
                }
                enabledMembers = allMembers.filter { it.id !in groupChat.disabledMemberIds }
            }

            val speakerQueue = if (groupChat.personas.isNotEmpty()) {
                groupChat.personas.map { it.id }
            } else {
                groupChat.memberIds
            }

            var lastSpeakerId: Uuid? = null
            var queueIndex = 0

            for (round in 0 until groupChat.autoChatRounds) {
                val speakers = GroupSpeakerSelector.pick(
                    strategy = groupChat.activationStrategy,
                    members = allMembers,
                    enabledMembers = enabledMembers,
                    userInput = if (round == 0) text else "",
                    lastSpeakerId = lastSpeakerId,
                    allowSelfResponses = groupChat.allowSelfResponses,
                    speakerWeights = groupChat.speakerWeights,
                    speakerQueue = speakerQueue,
                    currentQueueIndex = queueIndex,
                )

                if (speakers.isEmpty()) break

                for (speaker in speakers) {
                    _generatingState.update { map ->
                        map + (groupChatId to GeneratingInfo(
                            isGenerating = true,
                            currentSpeaker = speaker.name,
                        ))
                    }

                    delay(randomTypingDelay())

                    val messages = groupChatRepository.getMessages(groupChatId).first()
                    val reply = generateReply(speaker, groupChat, messages)

                    val colorIndex = if (groupChat.personas.isNotEmpty()) {
                        groupChat.personas.indexOfFirst { it.id == speaker.id }.let {
                            if (it >= 0) it % 8 else 0
                        }
                    } else {
                        groupChat.memberIds.indexOf(speaker.id).let {
                            if (it >= 0) it % 8 else 0
                        }
                    }
                    val assistantMessage = GroupMessage(
                        content = reply,
                        isUser = false,
                        speakerName = speaker.name,
                        speakerId = speaker.id,
                        colorIndex = colorIndex,
                    )
                    groupChatRepository.insertMessage(groupChatId, assistantMessage)

                    lastSpeakerId = speaker.id
                    if (groupChat.activationStrategy == GroupActivationStrategy.LIST) {
                        queueIndex++
                    }
                }

                if (round < groupChat.autoChatRounds - 1) {
                    delay(groupChat.autoModeDelay * 1000L)
                }
            }

            // Auto-reply chain: if a persona is @mentioned in the reply, let them respond (max 2 rounds)
            handleAutoReplyChain(groupChatId, groupChat, allMembers, enabledMembers)

            _generatingState.update { it - groupChatId }
        }
        activeJobs[groupChatId] = job
        job.invokeOnCompletion {
            _generatingState.update { it - groupChatId }
            activeJobs.remove(groupChatId)
        }
    }

    fun stopGeneration(groupChatId: Uuid) {
        activeJobs[groupChatId]?.cancel()
    }

    private suspend fun generateReply(
        speaker: GroupSpeakerSelector.MemberInfo,
        groupChat: GroupChat,
        messages: List<GroupMessage>,
    ): String {
        val settings = settingsStore.settingsFlow.first()

        val assistantId = speaker.assistantId
        if (assistantId == null) {
            Log.w(TAG, "No assistantId for speaker ${speaker.name}, using fallback")
            return fallbackReply(speaker, messages)
        }
        val assistant = settings.getAssistantById(assistantId)
        if (assistant == null) {
            Log.w(TAG, "Assistant not found for speaker ${speaker.name} ($assistantId), using fallback")
            return fallbackReply(speaker, messages)
        }

        val modelId = groupChat.chatModelId ?: assistant.chatModelId ?: settings.chatModelId
        val model = settings.findModelById(modelId) ?: settings.getCurrentChatModel()
        if (model == null) {
            Log.w(TAG, "Model not found for speaker ${speaker.name}, using fallback")
            return fallbackReply(speaker, messages)
        }

        val persona = groupChat.personas.find { it.id == speaker.id }
        val effectiveSystemPrompt = when {
            persona != null && persona.systemPrompt.isNotBlank() -> persona.systemPrompt
            else -> assistant.systemPrompt
        }

        val groupContextPrompt = buildString {
            appendLine("你正在一个群聊中。你的角色名是「${speaker.name}」。")
            appendLine("请以${speaker.name}的身份回复，保持角色一致性。")
            appendLine("回复应该简洁自然，像真实群聊对话一样。不要使用角色名前缀。")
        }

        val effectiveAssistant = assistant.copy(
            systemPrompt = buildString {
                if (effectiveSystemPrompt.isNotBlank()) {
                    appendLine(effectiveSystemPrompt)
                    appendLine()
                }
                append(groupContextPrompt)
            },
            chatModelId = model.id,
            streamOutput = true,
        )

        val uiMessages = buildGroupChatContext(messages, speaker)

        val memories = if (assistant.useGlobalMemory) {
            memoryRepository.getGlobalMemories()
        } else {
            memoryRepository.getMemoriesOfAssistant(assistant.id.toString())
        }

        return try {
            val result = StringBuilder()
            generationLoop.generateText(
                settings = settings,
                model = model,
                messages = uiMessages,
                assistant = effectiveAssistant,
                memories = memories,
                maxSteps = 1,
            ).collect { chunk ->
                when (chunk) {
                    is GenerationChunk.Messages -> {
                        val lastMsg = chunk.messages.lastOrNull()
                        if (lastMsg != null && lastMsg.role == MessageRole.ASSISTANT) {
                            val text = lastMsg.toText()
                            result.clear()
                            result.append(text)
                            _generatingState.update { map ->
                                val current = map[groupChat.id] ?: GeneratingInfo()
                                map + (groupChat.id to current.copy(
                                    streamingContent = text,
                                ))
                            }
                        }
                    }
                }
            }
            result.toString().trim().ifEmpty {
                fallbackReply(speaker, messages)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Generation failed for ${speaker.name}", e)
            fallbackReply(speaker, messages)
        }
    }

    private fun buildGroupChatContext(
        messages: List<GroupMessage>,
        currentSpeaker: GroupSpeakerSelector.MemberInfo,
    ): List<UIMessage> {
        return messages.takeLast(50).map { msg ->
            if (msg.isUser) {
                UIMessage(
                    role = MessageRole.USER,
                    parts = listOf(UIMessagePart.Text(msg.content)),
                )
            } else {
                if (msg.speakerId == currentSpeaker.id) {
                    UIMessage(
                        role = MessageRole.ASSISTANT,
                        parts = listOf(UIMessagePart.Text(msg.content)),
                    )
                } else {
                    UIMessage(
                        role = MessageRole.USER,
                        parts = listOf(UIMessagePart.Text("[${msg.speakerName}]: ${msg.content}")),
                    )
                }
            }
        }
    }

    private suspend fun fallbackReply(
        speaker: GroupSpeakerSelector.MemberInfo,
        messages: List<GroupMessage>,
    ): String {
        delay(500 + Random.nextLong(1000))
        val lastContent = messages.lastOrNull()?.content?.take(20) ?: ""
        return "[API未配置] 你好，我是${speaker.name}。这是对「${lastContent}」的回复。"
    }

    private suspend fun handleAutoReplyChain(
        groupChatId: Uuid,
        groupChat: GroupChat,
        allMembers: List<GroupSpeakerSelector.MemberInfo>,
        enabledMembers: List<GroupSpeakerSelector.MemberInfo>,
    ) {
        var chainsLeft = 2
        while (chainsLeft > 0) {
            val currentMessages = groupChatRepository.getMessages(groupChatId).first()
            val lastMsg = currentMessages.lastOrNull() ?: break
            if (lastMsg.isUser) break

            val mentionedNames = parseMentions(lastMsg.content)
            if (mentionedNames.isEmpty()) break

            val mentionedMembers = enabledMembers.filter { member ->
                mentionedNames.any { it.equals(member.name, ignoreCase = true) }
            }.filter { it.id != lastMsg.speakerId }

            if (mentionedMembers.isEmpty()) break

            for (member in mentionedMembers) {
                _generatingState.update { map ->
                    map + (groupChatId to GeneratingInfo(
                        isGenerating = true,
                        currentSpeaker = member.name,
                    ))
                }

                delay(randomTypingDelay())

                val msgs = groupChatRepository.getMessages(groupChatId).first()
                val reply = generateReply(member, groupChat, msgs)

                val colorIndex = if (groupChat.personas.isNotEmpty()) {
                    groupChat.personas.indexOfFirst { it.id == member.id }.let {
                        if (it >= 0) it % 8 else 0
                    }
                } else {
                    groupChat.memberIds.indexOf(member.id).let {
                        if (it >= 0) it % 8 else 0
                    }
                }
                val assistantMessage = GroupMessage(
                    content = reply,
                    isUser = false,
                    speakerName = member.name,
                    speakerId = member.id,
                    colorIndex = colorIndex,
                )
                groupChatRepository.insertMessage(groupChatId, assistantMessage)
            }
            chainsLeft--
        }
    }

    private fun parseMentions(text: String): List<String> {
        val regex = Regex("@(\\S+)")
        return regex.findAll(text).map { it.groupValues[1] }.toList()
    }

    private fun randomTypingDelay(): Long {
        return 800L + Random.nextLong(1200)
    }
}
