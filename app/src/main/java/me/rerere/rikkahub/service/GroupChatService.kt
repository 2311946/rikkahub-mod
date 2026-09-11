package me.rerere.rikkahub.service

import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.rerere.rikkahub.AppScope
import me.rerere.rikkahub.data.datastore.SettingsStore
import me.rerere.rikkahub.data.model.GroupActivationStrategy
import me.rerere.rikkahub.data.model.GroupChat
import me.rerere.rikkahub.data.model.GroupMessage
import me.rerere.rikkahub.data.model.GroupSpeakerSelector
import me.rerere.rikkahub.data.repository.GroupChatRepository
import kotlin.random.Random
import kotlin.uuid.Uuid

class GroupChatService(
    private val appScope: AppScope,
    private val settingsStore: SettingsStore,
    private val groupChatRepository: GroupChatRepository,
) {
    data class GeneratingInfo(
        val isGenerating: Boolean = false,
        val currentSpeaker: String = "",
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

            val allMembers = groupChat.memberIds.mapNotNull { memberId ->
                val assistant = settings.assistants.find { it.id == memberId }
                assistant?.let {
                    GroupSpeakerSelector.MemberInfo(
                        id = it.id,
                        name = it.name,
                        talkativeness = 0.5f,
                    )
                }
            }
            val enabledMembers = allMembers.filter { it.id !in groupChat.disabledMemberIds }

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
                    speakerQueue = groupChat.memberIds,
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

                    val messages = groupChatRepository.getMessages(groupChatId).first()
                    val reply = generateReply(speaker, groupChat, messages)

                    val colorIndex = groupChat.memberIds.indexOf(speaker.id).let {
                        if (it >= 0) it % 8 else 0
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
        delay(500 + Random.nextLong(1000))
        val lastContent = messages.lastOrNull()?.content?.take(20) ?: ""
        return "你好，我是${speaker.name}。这是对「${lastContent}」的回复。"
    }
}
