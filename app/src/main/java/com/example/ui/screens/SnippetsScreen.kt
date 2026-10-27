package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PinnedCommandEntity
import com.example.data.model.SnippetEntity
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.ShellWarning
import com.example.ui.viewmodel.ShellViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SnippetsScreen(
    viewModel: ShellViewModel,
    snackbarHostState: SnackbarHostState,
    onNavigateToTerminal: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    val snippets by viewModel.snippetsList.collectAsState()
    val pinnedCommands by viewModel.pinnedList.collectAsState()

    var selectedTabIndex by remember { mutableIntStateOf(0) }

    // Dialog state for adding or editing snippet
    var showSnippetDialog by remember { mutableStateOf(false) }
    var editingSnippet by remember { mutableStateOf<SnippetEntity?>(null) }
    var snippetTitle by remember { mutableStateOf("") }
    var snippetTemplate by remember { mutableStateOf("") }
    var snippetDescription by remember { mutableStateOf("") }
    var snippetCategory by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Snippets & Pinned", style = MaterialTheme.typography.titleMedium) }
            )
        },
        floatingActionButton = {
            if (selectedTabIndex == 0) {
                FloatingActionButton(
                    onClick = {
                        editingSnippet = null
                        snippetTitle = ""
                        snippetTemplate = ""
                        snippetDescription = ""
                        snippetCategory = "Custom"
                        showSnippetDialog = true
                    }
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Snippet")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            TabRow(selectedTabIndex = selectedTabIndex) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text("Templates (${snippets.size})") }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text("Pinned (${pinnedCommands.size})") }
                )
            }

            if (selectedTabIndex == 0) {
                // Snippet templates list
                if (snippets.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No snippet templates available", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(snippets, key = { it.id }) { snippet ->
                            SnippetCard(
                                snippet = snippet,
                                onRun = {
                                    viewModel.openSnippetPrompt(snippet)
                                    onNavigateToTerminal()
                                },
                                onPaste = {
                                    viewModel.onInputChange(snippet.commandTemplate)
                                    onNavigateToTerminal()
                                },
                                onEdit = {
                                    editingSnippet = snippet
                                    snippetTitle = snippet.title
                                    snippetTemplate = snippet.commandTemplate
                                    snippetDescription = snippet.description
                                    snippetCategory = snippet.category
                                    showSnippetDialog = true
                                },
                                onDelete = {
                                    viewModel.deleteSnippet(snippet)
                                }
                            )
                        }
                    }
                }
            } else {
                // Pinned commands list
                if (pinnedCommands.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No pinned commands yet",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Star any command in the terminal to pin it here",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(pinnedCommands, key = { it.id }) { pinned ->
                            PinnedItemCard(
                                item = pinned,
                                onRun = {
                                    viewModel.executeCommand(pinned.command)
                                    onNavigateToTerminal()
                                },
                                onPaste = {
                                    viewModel.onInputChange(pinned.command)
                                    onNavigateToTerminal()
                                },
                                onUnpin = {
                                    viewModel.togglePinCommand(pinned.command)
                                },
                                onCopy = {
                                    clipboardManager.setText(AnnotatedString(pinned.command))
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Command copied")
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Add/Edit Snippet Dialog
    if (showSnippetDialog) {
        AlertDialog(
            onDismissRequest = { showSnippetDialog = false },
            title = { Text(if (editingSnippet == null) "New Snippet" else "Edit Snippet") },
            text = {
                Column {
                    OutlinedTextField(
                        value = snippetTitle,
                        onValueChange = { snippetTitle = it },
                        label = { Text("Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = snippetTemplate,
                        onValueChange = { snippetTemplate = it },
                        label = { Text("Command Template") },
                        placeholder = { Text("e.g. pm grant {package} {permission}") },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = JetBrainsMonoFontFamily)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Use {variable} syntax for placeholders",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = snippetDescription,
                        onValueChange = { snippetDescription = it },
                        label = { Text("Description (Optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = snippetCategory,
                        onValueChange = { snippetCategory = it },
                        label = { Text("Category") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (snippetTitle.isNotBlank() && snippetTemplate.isNotBlank()) {
                            val current = editingSnippet
                            if (current == null) {
                                viewModel.addSnippet(
                                    snippetTitle,
                                    snippetTemplate,
                                    snippetDescription,
                                    snippetCategory
                                )
                            } else {
                                viewModel.updateSnippet(
                                    current.copy(
                                        title = snippetTitle.trim(),
                                        commandTemplate = snippetTemplate.trim(),
                                        description = snippetDescription.trim(),
                                        category = snippetCategory.trim()
                                    )
                                )
                            }
                            showSnippetDialog = false
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSnippetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SnippetCard(
    snippet: SnippetEntity,
    onRun: () -> Unit,
    onPaste: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = snippet.title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    if (snippet.category.isNotEmpty()) {
                        Text(
                            text = snippet.category,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp))
                    }
                }
            }

            if (snippet.description.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = snippet.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Template code card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = snippet.commandTemplate,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(8.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(onClick = onPaste, shape = RoundedCornerShape(8.dp)) {
                    Icon(Icons.Default.Terminal, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Paste", style = MaterialTheme.typography.labelSmall)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = onRun, shape = RoundedCornerShape(8.dp)) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Run", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun PinnedItemCard(
    item: PinnedCommandEntity,
    onRun: () -> Unit,
    onPaste: () -> Unit,
    onUnpin: () -> Unit,
    onCopy: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.command,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Row {
                IconButton(onClick = onUnpin, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Star, contentDescription = "Unpin", tint = ShellWarning, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onCopy, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = onPaste, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Terminal, contentDescription = "Paste", modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = onRun, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Run", modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
