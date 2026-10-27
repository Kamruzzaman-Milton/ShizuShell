package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedAssistChip
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.QuickChipEntity
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.ShellError

@Composable
fun TerminalInputBar(
    commandText: String,
    onCommandChange: (String) -> Unit,
    onSendCommand: (String) -> Unit,
    onStopCommand: () -> Unit,
    onHistoryUp: () -> Unit,
    onHistoryDown: () -> Unit,
    isRunning: Boolean,
    quickChips: List<QuickChipEntity>,
    onChipClick: (String) -> Unit,
    onAddChip: (String) -> Unit,
    onDeleteChip: (QuickChipEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddChipDialog by remember { mutableStateOf(false) }
    var newChipText by remember { mutableStateOf("") }
    var chipToDelete by remember { mutableStateOf<QuickChipEntity?>(null) }

    val focusManager = LocalFocusManager.current

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            // Quick chips row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                quickChips.forEach { chip ->
                    InputChip(
                        selected = false,
                        onClick = { onChipClick(chip.command) },
                        label = {
                            Text(
                                text = chip.command,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 11.sp
                                ),
                                maxLines = 1
                            )
                        },
                        trailingIcon = {
                            IconButton(
                                onClick = { chipToDelete = chip },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Delete chip",
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        },
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // Add Chip Button
                ElevatedAssistChip(
                    onClick = { showAddChipDialog = true },
                    label = { Text("+ Quick", style = MaterialTheme.typography.labelSmall) },
                    leadingIcon = {
                        Icon(Icons.Default.Add, contentDescription = "Add quick chip", modifier = Modifier.size(14.dp))
                    },
                    shape = RoundedCornerShape(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Command input row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // History Up/Down navigation buttons
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    IconButton(
                        onClick = onHistoryUp,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.ArrowUpward,
                            contentDescription = "History previous",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = onHistoryDown,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.ArrowDownward,
                            contentDescription = "History next",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Text field
                OutlinedTextField(
                    value = commandText,
                    onValueChange = onCommandChange,
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text(
                            "Enter shell command...",
                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = JetBrainsMonoFontFamily)
                        )
                    },
                    leadingIcon = {
                        Text(
                            text = "$",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingIcon = {
                        if (commandText.isNotEmpty()) {
                            IconButton(
                                onClick = { onCommandChange("") },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Clear input",
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (!isRunning && commandText.isNotBlank()) {
                                onSendCommand(commandText)
                            }
                        }
                    ),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = JetBrainsMonoFontFamily
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Send or Stop button
                if (isRunning) {
                    FilledIconButton(
                        onClick = onStopCommand,
                        modifier = Modifier.size(46.dp),
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = ShellError,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = "Stop execution")
                    }
                } else {
                    FilledIconButton(
                        onClick = {
                            if (commandText.isNotBlank()) {
                                onSendCommand(commandText)
                            }
                        },
                        modifier = Modifier.size(46.dp),
                        shape = CircleShape,
                        enabled = commandText.isNotBlank(),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Send command")
                    }
                }
            }
        }
    }

    // Delete chip confirmation dialog
    chipToDelete?.let { chip ->
        AlertDialog(
            onDismissRequest = { chipToDelete = null },
            title = { Text("Delete Quick Command") },
            text = { Text("Remove \"${chip.command}\" from quick access chips?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteChip(chip)
                        chipToDelete = null
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { chipToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Add quick chip dialog
    if (showAddChipDialog) {
        AlertDialog(
            onDismissRequest = { showAddChipDialog = false },
            title = { Text("Add Quick Command") },
            text = {
                Column {
                    Text("Enter a shell command to pin to the quick access bar:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newChipText,
                        onValueChange = { newChipText = it },
                        singleLine = true,
                        placeholder = { Text("e.g. logcat -c") },
                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = JetBrainsMonoFontFamily)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newChipText.isNotBlank()) {
                            onAddChip(newChipText.trim())
                            newChipText = ""
                            showAddChipDialog = false
                        }
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddChipDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
