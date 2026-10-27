package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.CommandHistoryEntity
import com.example.data.model.PinnedCommandEntity
import com.example.data.model.QuickChipEntity
import com.example.data.model.SessionEntity
import com.example.data.model.SnippetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Query("SELECT * FROM command_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<CommandHistoryEntity>>

    @Query("SELECT * FROM command_history WHERE command LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchHistory(query: String): Flow<List<CommandHistoryEntity>>

    @Query("SELECT command FROM command_history GROUP BY command ORDER BY MAX(timestamp) DESC LIMIT :limit")
    suspend fun getRecentUniqueCommands(limit: Int = 100): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: CommandHistoryEntity): Long

    @Query("UPDATE command_history SET isPinned = :isPinned WHERE id = :id")
    suspend fun setPinned(id: Long, isPinned: Boolean)

    @Delete
    suspend fun delete(entry: CommandHistoryEntity)

    @Query("DELETE FROM command_history")
    suspend fun clearAll()
}

@Dao
interface PinnedDao {
    @Query("SELECT * FROM pinned_commands ORDER BY timestamp DESC")
    fun getAllPinned(): Flow<List<PinnedCommandEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(pinned: PinnedCommandEntity): Long

    @Delete
    suspend fun delete(pinned: PinnedCommandEntity)

    @Query("DELETE FROM pinned_commands WHERE command = :command")
    suspend fun deleteByCommand(command: String)
}

@Dao
interface SnippetDao {
    @Query("SELECT * FROM snippets ORDER BY id ASC")
    fun getAllSnippets(): Flow<List<SnippetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(snippet: SnippetEntity): Long

    @Update
    suspend fun update(snippet: SnippetEntity)

    @Delete
    suspend fun delete(snippet: SnippetEntity)
}

@Dao
interface QuickChipDao {
    @Query("SELECT * FROM quick_chips ORDER BY orderIndex ASC, id ASC")
    fun getAllChips(): Flow<List<QuickChipEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(chip: QuickChipEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(chips: List<QuickChipEntity>)

    @Delete
    suspend fun delete(chip: QuickChipEntity)

    @Query("DELETE FROM quick_chips")
    suspend fun clearAll()
}

@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions ORDER BY createdAt ASC")
    fun getAllSessions(): Flow<List<SessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: SessionEntity)

    @Update
    suspend fun update(session: SessionEntity)

    @Delete
    suspend fun delete(session: SessionEntity)
}
