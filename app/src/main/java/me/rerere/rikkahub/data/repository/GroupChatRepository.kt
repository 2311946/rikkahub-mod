package me.rerere.rikkahub.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.rerere.rikkahub.data.db.dao.GroupChatDAO
import me.rerere.rikkahub.data.db.dao.GroupMessageDAO
import me.rerere.rikkahub.data.db.entity.GroupChatEntity
import me.rerere.rikkahub.data.db.entity.GroupMessageEntity
import me.rerere.rikkahub.data.model.GroupActivationStrategy
import me.rerere.rikkahub.data.model.GroupChat
import me.rerere.rikkahub.data.model.GroupGenerationMode
import me.rerere.rikkahub.data.model.GroupMessage
import me.rerere.rikkahub.data.model.GroupPersona
import me.rerere.rikkahub.utils.JsonInstant
import kotlin.uuid.Uuid

class GroupChatRepository(
    private val groupChatDAO: GroupChatDAO,
    private val groupMessageDAO: GroupMessageDAO,
) {
    fun getAllGroupChats(): Flow<List<GroupChat>> {
        return groupChatDAO.getAll().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    fun getGroupChatFlow(id: Uuid): Flow<GroupChat?> {
        return groupChatDAO.getByIdFlow(id.toString()).map { it?.toDomain() }
    }

    suspend fun getGroupChat(id: Uuid): GroupChat? {
        return groupChatDAO.getById(id.toString())?.toDomain()
    }

    suspend fun insertGroupChat(groupChat: GroupChat) {
        groupChatDAO.insert(groupChat.toEntity())
    }

    suspend fun updateGroupChat(groupChat: GroupChat) {
        groupChatDAO.update(groupChat.toEntity())
    }

    suspend fun deleteGroupChat(id: Uuid) {
        groupMessageDAO.deleteByGroupChat(id.toString())
        groupChatDAO.deleteById(id.toString())
    }

    fun getMessages(groupChatId: Uuid): Flow<List<GroupMessage>> {
        return groupMessageDAO.getMessagesOfGroupChat(groupChatId.toString()).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun insertMessage(groupChatId: Uuid, message: GroupMessage) {
        groupMessageDAO.insert(
            GroupMessageEntity(
                id = message.id,
                groupChatId = groupChatId.toString(),
                content = message.content,
                isUser = message.isUser,
                speakerName = message.speakerName,
                speakerId = message.speakerId?.toString() ?: "",
                colorIndex = message.colorIndex,
                createAt = message.createAt,
            )
        )
    }

    suspend fun clearMessages(groupChatId: Uuid) {
        groupMessageDAO.deleteByGroupChat(groupChatId.toString())
    }
}

private fun GroupChatEntity.toDomain(): GroupChat {
    return GroupChat(
        id = Uuid.parse(id),
        name = name,
        memberIds = JsonInstant.decodeFromString<List<String>>(memberIds).map { Uuid.parse(it) },
        activationStrategy = runCatching {
            GroupActivationStrategy.valueOf(activationStrategy)
        }.getOrDefault(GroupActivationStrategy.NATURAL),
        generationMode = runCatching {
            GroupGenerationMode.valueOf(generationMode)
        }.getOrDefault(GroupGenerationMode.APPEND),
        disabledMemberIds = JsonInstant.decodeFromString<List<String>>(disabledMemberIds)
            .map { Uuid.parse(it) }.toSet(),
        speakerWeights = JsonInstant.decodeFromString<Map<String, Int>>(speakerWeights)
            .map { (k, v) -> Uuid.parse(k) to v }.toMap(),
        conversationId = conversationId.takeIf { it.isNotEmpty() }?.let { Uuid.parse(it) },
        allowSelfResponses = allowSelfResponses,
        autoModeDelay = autoModeDelay,
        autoChatRounds = autoChatRounds,
        chatModelId = chatModelId.takeIf { it.isNotEmpty() }?.let { Uuid.parse(it) },
        personas = runCatching {
            JsonInstant.decodeFromString<List<GroupPersona>>(personas)
        }.getOrDefault(emptyList()),
        createAt = createAt,
        updateAt = updateAt,
    )
}

private fun GroupChat.toEntity(): GroupChatEntity {
    return GroupChatEntity(
        id = id.toString(),
        name = name,
        memberIds = JsonInstant.encodeToString(memberIds.map { it.toString() }),
        activationStrategy = activationStrategy.name,
        generationMode = generationMode.name,
        disabledMemberIds = JsonInstant.encodeToString(disabledMemberIds.map { it.toString() }),
        speakerWeights = JsonInstant.encodeToString(
            speakerWeights.map { (k, v) -> k.toString() to v }.toMap()
        ),
        conversationId = conversationId?.toString() ?: "",
        allowSelfResponses = allowSelfResponses,
        autoModeDelay = autoModeDelay,
        autoChatRounds = autoChatRounds,
        chatModelId = chatModelId?.toString() ?: "",
        personas = JsonInstant.encodeToString(personas),
        createAt = createAt,
        updateAt = System.currentTimeMillis(),
    )
}

private fun GroupMessageEntity.toDomain(): GroupMessage {
    return GroupMessage(
        id = id,
        content = content,
        isUser = isUser,
        speakerName = speakerName,
        speakerId = speakerId.takeIf { it.isNotEmpty() }?.let { Uuid.parse(it) },
        colorIndex = colorIndex,
        createAt = createAt,
    )
}
