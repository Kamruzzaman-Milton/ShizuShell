package com.example.ui.screens

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SessionEntity
import com.example.ui.components.CommandBubble
import com.example.ui.components.DangerousCommandDialog
import com.example.ui.components.OutputBubble
import com.example.ui.components.SetupGuideDialog
import com.example.ui.components.ShizukuDetailsBottomSheet
import com.example.ui.components.ShizukuStatusPill
import com.example.ui.components.SnippetVariableDialog
import com.example.ui.components.TerminalInputBar
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.ShellError
import com.example.ui.theme.ShellWarning
import com.example.ui.viewmodel.ShellViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalScreen(
    viewModel: ShellViewModel,
    snackbarHostState: SnackbarHostState
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    val shizukuStatus by viewModel.shizukuStatus.collectAsState()
    val preferences by viewModel.preferences.collectAsState()
    val activeSessionId by viewModel.activeSessionId.collectAsState()
    val sessions by viewModel.sessionsList.collectAsState()
    val allMessages by viewModel.sessionMessages.collectAsState()
    val inputCommand by viewModel.inputCommand.collectAsState()
    val isRunning by viewModel.isRunning.collectAsState()
    val quickChips by viewModel.quickChipsList.collectAsState()
    val pinnedList by viewModel.pinnedList.collectAsState()

    val pendingDangerousCommand by viewModel.pendingDangerousCommand.collectAsState()
    val pendingSnippet by viewModel.activeSnippetPrompt.collectAsState()

    val messages = allMessages[activeSessionId] ?: emptyList()

    var showShizukuSheet by remember { mutableStateOf(false) }
    var showGuideDialog by remember { mutableStateOf(false) }
    var showTopMenu by remember { mutableStateOf(false) }

    // Session tab rename dialog state
    var sessionToRename by remember { mutableStateOf<SessionEntity?>(null) }
    var renameText by remember { mutableStateOf("") }
    var showNewSessionDialog by remember { mutableStateOf(false) }
    var newSessionText by remember { mutableStateOf("") }

    val listState = rememberLazyListState()

    // Auto-scroll to newest message
    LaunchedEffect(messages.size, messages.lastOrNull()?.outputLines?.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size * 2 - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "ShizuShell",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = JetBrainsMonoFontFamily
                            )
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        ShizukuStatusPill(
                            status = shizukuStatus,
                            onClick = { showShizukuSheet = true },
                            onRequestPermission = { viewModel.shizukuManager.requestPermission() }
                        )
                    }
                },
                actions = {
                    // Copy full session action
                    IconButton(
                        onClick = {
                            val formatted = viewModel.formatSessionAsChat()
                            clipboardManager.setText(AnnotatedString(formatted))
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Entire session copied to clipboard")
                            }
                        }
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy full session")
                    }

                    // Share action
                    IconButton(
                        onClick = {
                            val text = viewModel.formatSessionAsChat()
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, text)
                                type = "text/plain"
                            }
                            val shareIntent = Intent.createChooser(sendIntent, "Share Terminal Session")
                            context.startActivity(shareIntent)
                        }
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share session")
                    }

                    // More options menu
                    Box {
                        IconButton(onClick = { showTopMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More options")
                        }

                        DropdownMenu(
                            expanded = showTopMenu,
                            onDismissRequest = { showTopMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Clear Active Session") },
                                leadingIcon = { Icon(Icons.Default.DeleteSweep, contentDescription = null) },
                                onClick = {
                                    viewModel.clearCurrentSession()
                                    showTopMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("New Session Tab") },
                                leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
                                onClick = {
                                    showNewSessionDialog = true
                                    showTopMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Shizuku Details") },
                                leadingIcon = { Icon(Icons.Default.Terminal, contentDescription = null) },
                                onClick = {
                                    showShizukuSheet = true
                                    showTopMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Setup Guide") },
                                leadingIcon = { Icon(Icons.Default.HelpOutline, contentDescription = null) },
                                onClick = {
                                    showGuideDialog = true
                                    showTopMenu = false
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            TerminalInputBar(
                commandText = inputCommand,
                onCommandChange = { viewModel.onInputChange(it) },
                onSendCommand = { cmd -> viewModel.executeCommand(cmd) },
                onStopCommand = { viewModel.stopRunningCommand() },
                onHistoryUp = { viewModel.recallHistoryUp() },
                onHistoryDown = { viewModel.recallHistoryDown() },
                isRunning = isRunning,
                quickChips = quickChips,
                onChipClick = { chipCmd ->
                    viewModel.onInputChange(chipCmd)
                },
                onAddChip = { newChip ->
                    viewModel.addQuickChip(newChip)
                },
                onDeleteChip = { chip ->
                    viewModel.removeQuickChip(chip)
                },
                modifier = Modifier.imePadding()
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Session Tabs row
            if (sessions.isNotEmpty()) {
                ScrollableTabRow(
                    selectedTabIndex = sessions.indexOfFirst { it.sessionId == activeSessionId }.coerceAtLeast(0),
                    edgePadding = 8.dp,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    divider = {}
                ) {
                    sessions.forEach { session ->
                        val isSelected = session.sessionId == activeSessionId
                        Tab(
                            selected = isSelected,
                            onClick = { viewModel.switchSession(session.sessionId) },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = session.name,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    )
                                    if (isSelected) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        IconButton(
                                            onClick = {
                                                sessionToRename = session
                                                renameText = session.name
                                            },
                                            modifier = Modifier.size(18.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Edit,
                                                contentDescription = "Rename session",
                                                modifier = Modifier.size(12.dp),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        if (sessions.size > 1) {
                                            IconButton(
                                                onClick = { viewModel.closeSession(session.sessionId) },
                                                modifier = Modifier.size(18.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Close,
                                                    contentDescription = "Close session",
                                                    modifier = Modifier.size(12.dp),
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        )
                    }

                    // Add session tab button
                    Tab(
                        selected = false,
                        onClick = { showNewSessionDialog = true },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(2.dp))
                                Text("New Tab", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    )
                }
            }

            // Connection banner if Shizuku not running
            if (!shizukuStatus.isAvailable) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ShellError.copy(alpha = 0.15f))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Shizuku is not running. Commands will fail until started.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 12.sp,
                                color = ShellError,
                                fontWeight = FontWeight.Medium
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(
                            onClick = { showGuideDialog = true }
                        ) {
                            Text("How to Start", color = ShellError, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            } else if (!shizukuStatus.hasPermission) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ShellWarning.copy(alpha = 0.15f))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Permission required to execute commands through Shizuku.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 12.sp,
                                color = ShellWarning,
                                fontWeight = FontWeight.Medium
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = { viewModel.shizukuManager.requestPermission() }
                        ) {
                            Text("Grant", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            // Chat Feed
            if (messages.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Terminal,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Welcome to ShizuShell",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Run privileged shell commands via Shizuku.\nTap any quick command chip below to get started.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedButton(onClick = { showGuideDialog = true }) {
                            Icon(Icons.Default.HelpOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Shizuku Setup Instructions")
                        }
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(messages, key = { it.id }) { item ->
                        val isPinned = pinnedList.any { it.command == item.command }
                        CommandBubble(
                            item = item,
                            isPinned = isPinned,
                            onTogglePin = { viewModel.togglePinCommand(item.command) },
                            onCopyText = { text, label ->
                                clipboardManager.setText(AnnotatedString(text))
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(label)
                                }
                            },
                            onReRun = { viewModel.executeCommand(item.command) }
                        )

                        OutputBubble(
                            item = item,
                            fontSizeSp = preferences.fontSizeSp,
                            wrapLongLines = preferences.wrapLongLines,
                            onCopyText = { text, label ->
                                clipboardManager.setText(AnnotatedString(text))
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(label)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // Shizuku details bottom sheet
    if (showShizukuSheet) {
        ShizukuDetailsBottomSheet(
            status = shizukuStatus,
            onDismiss = { showShizukuSheet = false },
            onRequestPermission = {
                viewModel.shizukuManager.requestPermission()
            },
            onRefresh = {
                viewModel.shizukuManager.checkStatus()
            },
            onOpenGuide = {
                showShizukuSheet = false
                showGuideDialog = true
            }
        )
    }

    // Setup guide dialog
    if (showGuideDialog) {
        SetupGuideDialog(onDismiss = { showGuideDialog = false })
    }

    // Dangerous command confirmation dialog
    pendingDangerousCommand?.let { dangerousCmd ->
        DangerousCommandDialog(
            command = dangerousCmd,
            onConfirm = { viewModel.confirmDangerousCommand() },
            onDismiss = { viewModel.dismissDangerousDialog() }
        )
    }

    // Snippet variable substitution dialog
    pendingSnippet?.let { snippet ->
        val variables = viewModel.extractSnippetVariables(snippet.commandTemplate)
        SnippetVariableDialog(
            snippet = snippet,
            variables = variables,
            onExecute = { values ->
                viewModel.runSnippetWithVariables(snippet, values)
            },
            onDismiss = { viewModel.dismissSnippetPrompt() }
        )
    }

    // Rename session dialog
    sessionToRename?.let { session ->
        AlertDialog(
            onDismissRequest = { sessionToRename = null },
            title = { Text("Rename Session Tab") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    singleLine = true,
                    label = { Text("Session Name") }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (renameText.isNotBlank()) {
                            viewModel.renameSession(session.sessionId, renameText)
                        }
                        sessionToRename = null
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { sessionToRename = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // New session dialog
    if (showNewSessionDialog) {
        AlertDialog(
            onDismissRequest = { showNewSessionDialog = false },
            title = { Text("Create New Session Tab") },
            text = {
                OutlinedTextField(
                    value = newSessionText,
                    onValueChange = { newSessionText = it },
                    singleLine = true,
                    placeholder = { Text("e.g. Logs, Package Tools") },
                    label = { Text("Session Name") }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.createNewSession(newSessionText)
                        newSessionText = ""
                        showNewSessionDialog = false
                    }
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewSessionDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
