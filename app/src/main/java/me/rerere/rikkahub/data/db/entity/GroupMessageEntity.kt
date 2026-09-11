package me.rerere.rikkahub.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "group_messages",
    indices = [Index(value = ["group_chat_id"])]
)
data class GroupMessageEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo("group_chat_id")
    val groupChatId: String,
    @ColumnInfo("content")
    val content: String,
    @ColumnInfo("is_user", defaultValue = "0")
    val isUser: Boolean = false,
    @ColumnInfo("speaker_name", defaultValue = "")
    val speakerName: String = "",
    @ColumnInfo("speaker_id", defaultValue = "")
    val speakerId: String = "",
    @ColumnInfo("color_index", defaultValue = "0")
    val colorIndex: Int = 0,
    @ColumnInfo("create_at")
    val createAt: Long,
)
