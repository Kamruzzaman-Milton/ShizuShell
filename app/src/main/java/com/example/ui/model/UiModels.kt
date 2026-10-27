package com.example.ui.model

import java.util.UUID

data class OutputLineItem(
    val text: String,
    val isError: Boolean = false
)

sealed class CommandStatus {
    data class Running(val startedAt: Long = System.currentTimeMillis()) : CommandStatus()
    data class Completed(val exitCode: Int, val durationMs: Long) : CommandStatus()
    data class Error(val message: String) : CommandStatus()
}

data class ChatMessageItem(
    val id: String = UUID.randomUUID().toString(),
    val command: String,
    val timestamp: Long = System.currentTimeMillis(),
    val status: CommandStatus = CommandStatus.Running(),
    val outputLines: List<OutputLineItem> = emptyList(),
    val isPinned: Boolean = false
) {
    val isRunning: Boolean get() = status is CommandStatus.Running

    fun getFullOutputText(): String {
        return outputLines.joinToString("\n") { it.text }
    }
}

data class InstalledAppItem(
    val packageName: String,
    val appName: String,
    val isSystem: Boolean,
    val versionName: String = "",
    val isEnabled: Boolean = true
)
