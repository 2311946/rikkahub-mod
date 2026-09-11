package me.rerere.rikkahub.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import me.rerere.rikkahub.data.db.entity.GroupChatEntity

@Dao
interface GroupChatDAO {
    @Query("SELECT * FROM group_chats ORDER BY update_at DESC")
    fun getAll(): Flow<List<GroupChatEntity>>

    @Query("SELECT * FROM group_chats WHERE id = :id")
    suspend fun getById(id: String): GroupChatEntity?

    @Query("SELECT * FROM group_chats WHERE id = :id")
    fun getByIdFlow(id: String): Flow<GroupChatEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(groupChat: GroupChatEntity)

    @Update
    suspend fun update(groupChat: GroupChatEntity)

    @Query("DELETE FROM group_chats WHERE id = :id")
    suspend fun deleteById(id: String)
}
