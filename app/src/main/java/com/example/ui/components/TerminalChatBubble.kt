package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.model.ChatMessageItem
import com.example.ui.model.CommandStatus
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.ShellError
import com.example.ui.theme.ShellSuccess
import com.example.ui.theme.ShellWarning
import kotlinx.coroutines.delay

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CommandBubble(
    item: ChatMessageItem,
    isPinned: Boolean,
    onTogglePin: () -> Unit,
    onCopyText: (String, String) -> Unit, // text, label
    onReRun: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.End
    ) {
        Box {
            Surface(
                modifier = Modifier
                    .widthIn(min = 90.dp, max = 560.dp)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 4.dp))
                    .combinedClickable(
                        onLongClick = { showMenu = true },
                        onClick = { /* Keep standard click for copy or non-intrusive action */ }
                    ),
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 4.dp),
                tonalElevation = 3.dp
            ) {
                Row(
                    modifier = Modifier.padding(start = 12.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = "$ ",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                        modifier = Modifier.padding(top = 1.dp)
                    )
                    SelectionContainer(modifier = Modifier.weight(1f, fill = false)) {
                        Text(
                            text = item.command,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = onTogglePin,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            if (isPinned) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "Pin command",
                            tint = if (isPinned) ShellWarning else MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Copy Command") },
                    leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    onClick = {
                        onCopyText(item.command, "Command copied")
                        showMenu = false
                    }
                )
                if (item.outputLines.isNotEmpty()) {
                    DropdownMenuItem(
                        text = { Text("Copy Output") },
                        leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        onClick = {
                            onCopyText(item.getFullOutputText(), "Output copied")
                            showMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Copy Command + Output") },
                        leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        onClick = {
                            val combined = "$ ${item.command}\n${item.getFullOutputText()}"
                            onCopyText(combined, "Command and output copied")
                            showMenu = false
                        }
                    )
                }
                DropdownMenuItem(
                    text = { Text(if (isPinned) "Unpin Command" else "Pin Command") },
                    leadingIcon = { Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    onClick = {
                        onTogglePin()
                        showMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Re-run Command") },
                    leadingIcon = { Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    onClick = {
                        onReRun()
                        showMenu = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OutputBubble(
    item: ChatMessageItem,
    fontSizeSp: Float,
    wrapLongLines: Boolean,
    onCopyText: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    // Live elapsed timer when running - accurately anchored to start timestamp
    val startedAt = remember(item.id) {
        (item.status as? CommandStatus.Running)?.startedAt ?: System.currentTimeMillis()
    }
    var elapsedSeconds by remember(item.id) {
        mutableLongStateOf((System.currentTimeMillis() - startedAt).coerceAtLeast(0L) / 1000L)
    }

    LaunchedEffect(item.status) {
        if (item.status is CommandStatus.Running) {
            while (true) {
                elapsedSeconds = (System.currentTimeMillis() - startedAt).coerceAtLeast(0L) / 1000L
                delay(500L)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Box {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.96f)
                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp))
                    .combinedClickable(
                        onLongClick = { showMenu = true },
                        onClick = { /* Non-intrusive click allows scrolling and selection */ }
                    ),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                shape = RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
                tonalElevation = 1.dp
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // Header metadata: exit code badge and duration / running state
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        when (val status = item.status) {
                            is CommandStatus.Running -> {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(12.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Running... ${elapsedSeconds}s",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontWeight = FontWeight.Medium
                                        ),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            is CommandStatus.Completed -> {
                                val isSuccess = status.exitCode == 0
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(
                                                if (isSuccess) ShellSuccess.copy(alpha = 0.15f)
                                                else ShellError.copy(alpha = 0.15f)
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "exit ${status.exitCode}",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontFamily = JetBrainsMonoFontFamily,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp
                                            ),
                                            color = if (isSuccess) ShellSuccess else ShellError
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = formatDuration(status.durationMs),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontSize = 11.sp
                                        ),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }
                            }
                            is CommandStatus.Error -> {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(ShellError.copy(alpha = 0.15f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "error",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontFamily = JetBrainsMonoFontFamily,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp
                                            ),
                                            color = ShellError
                                        )
                                    }
                                }
                            }
                        }

                        // Copy quick action
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "Output options",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Monospace output stream with selectable text and smooth rendering
                    if (item.outputLines.isEmpty() && item.isRunning) {
                        Text(
                            text = "Waiting for process output...",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = fontSizeSp.sp,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    } else if (item.outputLines.isEmpty() && !item.isRunning) {
                        Text(
                            text = "(No output)",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = fontSizeSp.sp,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    } else {
                        val textModifier = if (!wrapLongLines) {
                            Modifier.horizontalScroll(rememberScrollState())
                        } else {
                            Modifier.fillMaxWidth()
                        }

                        val defaultTextColor = MaterialTheme.colorScheme.onSurface
                        val annotatedOutput = remember(item.outputLines, defaultTextColor) {
                            buildAnnotatedString {
                                item.outputLines.forEachIndexed { index, lineItem ->
                                    if (index > 0) append("\n")
                                    withStyle(SpanStyle(color = if (lineItem.isError) ShellError else defaultTextColor)) {
                                        append(lineItem.text)
                                    }
                                }
                            }
                        }

                        Box(modifier = textModifier) {
                            SelectionContainer {
                                Text(
                                    text = annotatedOutput,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = fontSizeSp.sp,
                                        lineHeight = (fontSizeSp + 4).sp
                                    )
                                )
                            }
                        }
                    }
                }
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Copy Output") },
                    leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    onClick = {
                        onCopyText(item.getFullOutputText(), "Output copied")
                        showMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Copy Command + Output") },
                    leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    onClick = {
                        val combined = "$ ${item.command}\n${item.getFullOutputText()}"
                        onCopyText(combined, "Command and output copied")
                        showMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Copy Command") },
                    leadingIcon = { Icon(Icons.Default.Terminal, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    onClick = {
                        onCopyText(item.command, "Command copied")
                        showMenu = false
                    }
                )
            }
        }
    }
}

private fun formatDuration(ms: Long): String {
    return if (ms < 1000) {
        "${ms}ms"
    } else {
        String.format(java.util.Locale.US, "%.2fs", ms / 1000.0)
    }
}
