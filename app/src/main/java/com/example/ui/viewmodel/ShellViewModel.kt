package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.ShizuDatabase
import com.example.data.model.CommandHistoryEntity
import com.example.data.model.PinnedCommandEntity
import com.example.data.model.QuickChipEntity
import com.example.data.model.SessionEntity
import com.example.data.model.SnippetEntity
import com.example.data.prefs.AppPreferencesManager
import com.example.data.prefs.ThemeMode
import com.example.data.prefs.UserPreferences
import com.example.shizuku.ExecutionEvent
import com.example.shizuku.ShizukuManager
import com.example.shizuku.ShizukuStatus
import com.example.ui.model.ChatMessageItem
import com.example.ui.model.CommandStatus
import com.example.ui.model.InstalledAppItem
import com.example.ui.model.OutputLineItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class ShellViewModel(application: Application) : AndroidViewModel(application) {

    val prefsManager = AppPreferencesManager(application)
    val preferences: StateFlow<UserPreferences> = prefsManager.preferences

    private val db = ShizuDatabase.getDatabase(application, viewModelScope)
    val historyDao = db.historyDao()
    val pinnedDao = db.pinnedDao()
    val snippetDao = db.snippetDao()
    val quickChipDao = db.quickChipDao()
    val sessionDao = db.sessionDao()

    val shizukuManager = ShizukuManager(application, viewModelScope)
    val shizukuStatus: StateFlow<ShizukuStatus> = shizukuManager.status

    val historyList: StateFlow<List<CommandHistoryEntity>> = historyDao.getAllHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pinnedList: StateFlow<List<PinnedCommandEntity>> = pinnedDao.getAllPinned()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val snippetsList: StateFlow<List<SnippetEntity>> = snippetDao.getAllSnippets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val quickChipsList: StateFlow<List<QuickChipEntity>> = quickChipDao.getAllChips()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sessionsList: StateFlow<List<SessionEntity>> = sessionDao.getAllSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active session ID
    private val _activeSessionId = MutableStateFlow("session_default")
    val activeSessionId: StateFlow<String> = _activeSessionId.asStateFlow()

    // Map of session ID to chat messages
    private val _sessionMessages = MutableStateFlow<Map<String, List<ChatMessageItem>>>(
        mapOf("session_default" to emptyList())
    )
    val sessionMessages: StateFlow<Map<String, List<ChatMessageItem>>> = _sessionMessages.asStateFlow()

    // Current input bar state
    private val _inputCommand = MutableStateFlow("")
    val inputCommand: StateFlow<String> = _inputCommand.asStateFlow()

    // History recall pointer
    private var historyRecallIndex = -1
    private var cachedHistoryCommands = listOf<String>()

    // Current running command execution job
    private var currentExecutionJob: Job? = null
    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    // Dangerous command guard state
    private val _pendingDangerousCommand = MutableStateFlow<String?>(null)
    val pendingDangerousCommand: StateFlow<String?> = _pendingDangerousCommand.asStateFlow()

    // Snippet variable prompt state
    private val _activeSnippetPrompt = MutableStateFlow<SnippetEntity?>(null)
    val activeSnippetPrompt: StateFlow<SnippetEntity?> = _activeSnippetPrompt.asStateFlow()

    // Installed apps for package manager & permission helper
    private val _installedApps = MutableStateFlow<List<InstalledAppItem>>(emptyList())
    val installedApps: StateFlow<List<InstalledAppItem>> = _installedApps.asStateFlow()
    private val _isLoadingApps = MutableStateFlow(false)
    val isLoadingApps: StateFlow<Boolean> = _isLoadingApps.asStateFlow()

    // Feedback snackbar messages
    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    init {
        shizukuManager.init()
        refreshHistoryCache()
    }

    override fun onCleared() {
        super.onCleared()
        shizukuManager.destroy()
    }

    fun showSnackbar(message: String) {
        _snackbarMessage.value = message
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun onInputChange(newText: String) {
        _inputCommand.value = newText
    }

    private fun refreshHistoryCache() {
        viewModelScope.launch(Dispatchers.IO) {
            cachedHistoryCommands = historyDao.getRecentUniqueCommands(50)
        }
    }

    fun recallHistoryUp() {
        if (cachedHistoryCommands.isEmpty()) return
        if (historyRecallIndex < cachedHistoryCommands.size - 1) {
            historyRecallIndex++
            _inputCommand.value = cachedHistoryCommands[historyRecallIndex]
        }
    }

    fun recallHistoryDown() {
        if (historyRecallIndex > 0) {
            historyRecallIndex--
            _inputCommand.value = cachedHistoryCommands[historyRecallIndex]
        } else if (historyRecallIndex == 0) {
            historyRecallIndex = -1
            _inputCommand.value = ""
        }
    }

    fun isDangerousCommand(cmd: String): Boolean {
        val trimmed = cmd.trim().lowercase()
        val dangerousPatterns = listOf(
            "rm -r",
            "rm -rf",
            "pm uninstall",
            "pm clear",
            "reboot",
            "wipe",
            "dd ",
            "mkfs",
            "format ",
            "setenforce 0",
            "kill -9 -1"
        )
        return dangerousPatterns.any { pattern ->
            trimmed.startsWith(pattern) || trimmed.contains(" $pattern") || trimmed.contains("&& $pattern") || trimmed.contains("; $pattern")
        }
    }

    fun executeCommand(rawCmd: String, forceSkipGuard: Boolean = false) {
        val cmd = rawCmd.trim()
        if (cmd.isEmpty()) return

        if (!forceSkipGuard && preferences.value.guardDangerousCommands && isDangerousCommand(cmd)) {
            _pendingDangerousCommand.value = cmd
            return
        }

        _pendingDangerousCommand.value = null
        _inputCommand.value = ""
        historyRecallIndex = -1

        val sessionId = _activeSessionId.value
        val messageId = UUID.randomUUID().toString()
        val newMessage = ChatMessageItem(
            id = messageId,
            command = cmd,
            status = CommandStatus.Running()
        )

        // Add to active session messages
        updateSessionMessageList(sessionId) { currentList ->
            currentList + newMessage
        }

        _isRunning.value = true

        currentExecutionJob = viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            var finalExitCode = 0
            var finalDuration = 0L

            shizukuManager.executeCommand(cmd) { event ->
                when (event) {
                    is ExecutionEvent.OutputLine -> {
                        updateSingleMessage(sessionId, messageId) { msg ->
                            val updatedLines = msg.outputLines + OutputLineItem(event.line, event.isError)
                            msg.copy(outputLines = updatedLines)
                        }
                    }
                    is ExecutionEvent.Finished -> {
                        finalExitCode = event.exitCode
                        finalDuration = event.durationMs
                        updateSingleMessage(sessionId, messageId) { msg ->
                            msg.copy(status = CommandStatus.Completed(event.exitCode, event.durationMs))
                        }
                    }
                    is ExecutionEvent.Error -> {
                        finalExitCode = -1
                        finalDuration = System.currentTimeMillis() - startTime
                        updateSingleMessage(sessionId, messageId) { msg ->
                            val errLine = OutputLineItem(event.message, isError = true)
                            msg.copy(
                                status = CommandStatus.Error(event.message),
                                outputLines = msg.outputLines + errLine
                            )
                        }
                    }
                }
            }

            // Save to database history
            val sessionName = sessionsList.value.find { it.sessionId == sessionId }?.name ?: "Main"
            historyDao.insert(
                CommandHistoryEntity(
                    command = cmd,
                    exitCode = finalExitCode,
                    durationMs = finalDuration,
                    sessionId = sessionId,
                    sessionName = sessionName
                )
            )
            refreshHistoryCache()

            _isRunning.value = false
            currentExecutionJob = null
        }
    }

    fun stopRunningCommand() {
        shizukuManager.stopCurrentProcess()
        currentExecutionJob?.cancel()
        currentExecutionJob = null
        _isRunning.value = false

        val sessionId = _activeSessionId.value
        val messages = _sessionMessages.value[sessionId] ?: return
        val runningMsg = messages.find { it.isRunning } ?: return
        updateSingleMessage(sessionId, runningMsg.id) { msg ->
            msg.copy(
                status = CommandStatus.Completed(130, 0),
                outputLines = msg.outputLines + OutputLineItem("[Process terminated by user]", isError = true)
            )
        }
    }

    fun dismissDangerousDialog() {
        _pendingDangerousCommand.value = null
    }

    fun confirmDangerousCommand() {
        val cmd = _pendingDangerousCommand.value ?: return
        _pendingDangerousCommand.value = null
        executeCommand(cmd, forceSkipGuard = true)
    }

    // Session Management
    fun createNewSession(name: String = "") {
        val count = sessionsList.value.size + 1
        val sessionName = name.ifBlank { "Session $count" }
        val newId = "session_${UUID.randomUUID().toString().take(8)}"
        val entity = SessionEntity(sessionId = newId, name = sessionName)

        viewModelScope.launch(Dispatchers.IO) {
            sessionDao.insert(entity)
            withContext(Dispatchers.Main) {
                _sessionMessages.value = _sessionMessages.value + (newId to emptyList())
                _activeSessionId.value = newId
            }
        }
    }

    fun switchSession(sessionId: String) {
        _activeSessionId.value = sessionId
        historyRecallIndex = -1
    }

    fun renameSession(sessionId: String, newName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val session = sessionsList.value.find { it.sessionId == sessionId }
            if (session != null) {
                sessionDao.update(session.copy(name = newName.trim()))
            }
        }
    }

    fun closeSession(sessionId: String) {
        val currentSessions = sessionsList.value
        if (currentSessions.size <= 1) {
            showSnackbar("Cannot close the only session")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val sessionToDelete = currentSessions.find { it.sessionId == sessionId }
            if (sessionToDelete != null) {
                sessionDao.delete(sessionToDelete)
            }
            withContext(Dispatchers.Main) {
                val updatedMessages = _sessionMessages.value.toMutableMap()
                updatedMessages.remove(sessionId)
                _sessionMessages.value = updatedMessages

                if (_activeSessionId.value == sessionId) {
                    val remaining = currentSessions.filter { it.sessionId != sessionId }
                    _activeSessionId.value = remaining.firstOrNull()?.sessionId ?: "session_default"
                }
            }
        }
    }

    fun clearCurrentSession() {
        val sessionId = _activeSessionId.value
        updateSessionMessageList(sessionId) { emptyList() }
        showSnackbar("Session cleared")
    }

    // Pinned Commands
    fun togglePinCommand(command: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val isPinned = pinnedList.value.any { it.command == command }
            if (isPinned) {
                pinnedDao.deleteByCommand(command)
                showSnackbar("Unpinned command")
            } else {
                pinnedDao.insert(PinnedCommandEntity(command = command))
                showSnackbar("Pinned command")
            }
        }
    }

    // Quick Chips
    fun addQuickChip(cmd: String) {
        val trimmed = cmd.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            val nextIndex = quickChipsList.value.size
            quickChipDao.insert(QuickChipEntity(command = trimmed, orderIndex = nextIndex))
            showSnackbar("Added quick chip: $trimmed")
        }
    }

    fun removeQuickChip(chip: QuickChipEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            quickChipDao.delete(chip)
        }
    }

    // Snippets
    fun openSnippetPrompt(snippet: SnippetEntity) {
        val variables = extractSnippetVariables(snippet.commandTemplate)
        if (variables.isEmpty()) {
            executeCommand(snippet.commandTemplate)
        } else {
            _activeSnippetPrompt.value = snippet
        }
    }

    fun dismissSnippetPrompt() {
        _activeSnippetPrompt.value = null
    }

    fun runSnippetWithVariables(snippet: SnippetEntity, variableValues: Map<String, String>) {
        var cmd = snippet.commandTemplate
        for ((key, value) in variableValues) {
            cmd = cmd.replace("{$key}", value.trim())
        }
        _activeSnippetPrompt.value = null
        executeCommand(cmd)
    }

    fun addSnippet(title: String, template: String, desc: String, category: String) {
        viewModelScope.launch(Dispatchers.IO) {
            snippetDao.insert(
                SnippetEntity(
                    title = title.trim(),
                    commandTemplate = template.trim(),
                    description = desc.trim(),
                    category = category.ifBlank { "General" }
                )
            )
            showSnackbar("Snippet created: $title")
        }
    }

    fun updateSnippet(snippet: SnippetEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            snippetDao.update(snippet)
            showSnackbar("Snippet updated")
        }
    }

    fun deleteSnippet(snippet: SnippetEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            snippetDao.delete(snippet)
            showSnackbar("Snippet deleted")
        }
    }

    fun extractSnippetVariables(template: String): List<String> {
        val regex = Regex("\\{([a-zA-Z0-9_-]+)\\}")
        return regex.findAll(template).map { it.groupValues[1] }.distinct().toList()
    }

    // Package Manager and Permissions
    fun loadInstalledApps() {
        if (_installedApps.value.isNotEmpty()) return
        _isLoadingApps.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val pm = getApplication<Application>().packageManager
                val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
                val items = apps.map { appInfo ->
                    val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                    val name = appInfo.loadLabel(pm).toString()
                    InstalledAppItem(
                        packageName = appInfo.packageName,
                        appName = name,
                        isSystem = isSystem,
                        isEnabled = appInfo.enabled
                    )
                }.sortedWith(compareBy({ it.isSystem }, { it.appName.lowercase() }))

                _installedApps.value = items
            } catch (e: Exception) {
                Log.e("ShellViewModel", "Error loading installed applications", e)
            } finally {
                _isLoadingApps.value = false
            }
        }
    }

    // Export Session
    fun formatSessionAsChat(): String {
        val sessionId = _activeSessionId.value
        val messages = _sessionMessages.value[sessionId] ?: emptyList()
        val sessionName = sessionsList.value.find { it.sessionId == sessionId }?.name ?: "Main"
        val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

        val sb = StringBuilder()
        sb.append("# ShizuShell Session: $sessionName\n")
        sb.append("# Exported: $dateStr\n")
        val status = shizukuStatus.value
        val mode = if (status.isRoot) "Root (UID 0)" else if (status.isAdb) "ADB (UID 2000)" else "Unknown"
        sb.append("# Shizuku Status: ${if (status.isAvailable) "Running ($mode, v${status.version})" else "Disconnected"}\n")
        sb.append("--------------------------------------------------\n\n")

        for (item in messages) {
            sb.append("$ ").append(item.command).append("\n")
            val output = item.getFullOutputText()
            if (output.isNotEmpty()) {
                sb.append(output).append("\n")
            }
            when (val st = item.status) {
                is CommandStatus.Completed -> {
                    sb.append("[Exit: ${st.exitCode} | Duration: ${st.durationMs}ms]\n")
                }
                is CommandStatus.Error -> {
                    sb.append("[Error: ${st.message}]\n")
                }
                is CommandStatus.Running -> {
                    sb.append("[Running...]\n")
                }
            }
            sb.append("\n")
        }

        return sb.toString()
    }

    suspend fun exportSessionToFolder(treeUriStr: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val context = getApplication<Application>()
            val treeUri = Uri.parse(treeUriStr)
            val docFile = DocumentFile.fromTreeUri(context, treeUri)
                ?: return@withContext Result.failure(Exception("Cannot access selected folder"))

            val dateStr = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.getDefault()).format(Date())
            val fileName = "shizushell_$dateStr.txt"

            val newFile = docFile.createFile("text/plain", fileName)
                ?: return@withContext Result.failure(Exception("Failed to create export file"))

            val content = formatSessionAsChat()
            context.contentResolver.openOutputStream(newFile.uri)?.use { stream ->
                stream.write(content.toByteArray())
            }

            Result.success(fileName)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            historyDao.clearAll()
            refreshHistoryCache()
            showSnackbar("All history cleared")
        }
    }

    // Helper message update functions
    private fun updateSessionMessageList(
        sessionId: String,
        transform: (List<ChatMessageItem>) -> List<ChatMessageItem>
    ) {
        val currentMap = _sessionMessages.value.toMutableMap()
        val list = currentMap[sessionId] ?: emptyList()
        currentMap[sessionId] = transform(list)
        _sessionMessages.value = currentMap
    }

    private fun updateSingleMessage(
        sessionId: String,
        messageId: String,
        transform: (ChatMessageItem) -> ChatMessageItem
    ) {
        val currentMap = _sessionMessages.value.toMutableMap()
        val list = (currentMap[sessionId] ?: emptyList()).map { msg ->
            if (msg.id == messageId) transform(msg) else msg
        }
        currentMap[sessionId] = list
        _sessionMessages.value = currentMap
    }
}
