package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "command_history")
data class CommandHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val command: String,
    val exitCode: Int,
    val durationMs: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val sessionId: String = "default",
    val sessionName: String = "Main",
    val isPinned: Boolean = false
)

@Entity(tableName = "pinned_commands")
data class PinnedCommandEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val command: String,
    val label: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "snippets")
data class SnippetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val commandTemplate: String, // e.g. "pm grant {package} {permission}"
    val description: String = "",
    val category: String = "General",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "quick_chips")
data class QuickChipEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val command: String,
    val orderIndex: Int = 0
)

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey
    val sessionId: String,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)
