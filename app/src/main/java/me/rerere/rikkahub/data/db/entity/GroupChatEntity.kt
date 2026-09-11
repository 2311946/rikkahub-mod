package me.rerere.rikkahub.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "group_chats")
data class GroupChatEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo("name", defaultValue = "")
    val name: String = "",
    @ColumnInfo("member_ids", defaultValue = "[]")
    val memberIds: String = "[]",
    @ColumnInfo("activation_strategy", defaultValue = "NATURAL")
    val activationStrategy: String = "NATURAL",
    @ColumnInfo("generation_mode", defaultValue = "APPEND")
    val generationMode: String = "APPEND",
    @ColumnInfo("disabled_member_ids", defaultValue = "[]")
    val disabledMemberIds: String = "[]",
    @ColumnInfo("speaker_weights", defaultValue = "{}")
    val speakerWeights: String = "{}",
    @ColumnInfo("conversation_id", defaultValue = "")
    val conversationId: String = "",
    @ColumnInfo("allow_self_responses", defaultValue = "0")
    val allowSelfResponses: Boolean = false,
    @ColumnInfo("auto_mode_delay", defaultValue = "3")
    val autoModeDelay: Int = 3,
    @ColumnInfo("auto_chat_rounds", defaultValue = "3")
    val autoChatRounds: Int = 3,
    @ColumnInfo("chat_model_id", defaultValue = "")
    val chatModelId: String = "",
    @ColumnInfo("create_at")
    val createAt: Long,
    @ColumnInfo("update_at")
    val updateAt: Long,
)
