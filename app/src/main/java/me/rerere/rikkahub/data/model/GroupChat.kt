package me.rerere.rikkahub.data.model

import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

@Serializable
enum class GroupActivationStrategy {
    NATURAL,
    LIST,
    MANUAL,
    POOLED
}

@Serializable
enum class GroupGenerationMode {
    SWAP,
    APPEND
}

@Serializable
data class GroupPersona(
    val id: Uuid = Uuid.random(),
    val assistantId: Uuid,
    val name: String,
    val systemPrompt: String = "",
    val talkativeness: Float = 0.5f,
    val enabled: Boolean = true,
)

@Serializable
data class GroupChat(
    val id: Uuid = Uuid.random(),
    val name: String = "",
    val memberIds: List<Uuid> = emptyList(),
    val activationStrategy: GroupActivationStrategy = GroupActivationStrategy.NATURAL,
    val generationMode: GroupGenerationMode = GroupGenerationMode.APPEND,
    val disabledMemberIds: Set<Uuid> = emptySet(),
    val speakerWeights: Map<Uuid, Int> = emptyMap(),
    val conversationId: Uuid? = null,
    val allowSelfResponses: Boolean = false,
    val autoModeDelay: Int = 3,
    val autoChatRounds: Int = 3,
    val chatModelId: Uuid? = null,
    val personas: List<GroupPersona> = emptyList(),
)

@Serializable
data class SpeakerRecord(
    val nodeId: String,
    val assistantId: String,
    val speakerName: String
)

data class GroupMessage(
    val id: String = Uuid.random().toString(),
    val content: String,
    val isUser: Boolean,
    val speakerName: String = "",
    val speakerId: Uuid? = null,
    val colorIndex: Int = 0,
    val isGenerating: Boolean = false,
    val createAt: Long = System.currentTimeMillis(),
)