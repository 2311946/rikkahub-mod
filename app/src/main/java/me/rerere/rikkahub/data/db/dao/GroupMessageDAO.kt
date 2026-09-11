package me.rerere.rikkahub.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import me.rerere.rikkahub.data.db.entity.GroupMessageEntity

@Dao
interface GroupMessageDAO {
    @Query("SELECT * FROM group_messages WHERE group_chat_id = :groupChatId ORDER BY create_at ASC")
    fun getMessagesOfGroupChat(groupChatId: String): Flow<List<GroupMessageEntity>>

    @Insert
    suspend fun insert(message: GroupMessageEntity)

    @Insert
    suspend fun insertAll(messages: List<GroupMessageEntity>)

    @Query("DELETE FROM group_messages WHERE group_chat_id = :groupChatId")
    suspend fun deleteByGroupChat(groupChatId: String)

    @Query("DELETE FROM group_messages WHERE id = :id")
    suspend fun deleteById(id: String)
}
