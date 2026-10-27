package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.viewmodel.ShellViewModel

private val PERMISSION_PRESETS = listOf(
    "android.permission.WRITE_SECURE_SETTINGS",
    "android.permission.READ_LOGS",
    "android.permission.DUMP",
    "android.permission.PACKAGE_USAGE_STATS",
    "android.permission.SYSTEM_ALERT_WINDOW",
    "android.permission.CHANGE_CONFIGURATION"
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PermissionGrantScreen(
    viewModel: ShellViewModel,
    snackbarHostState: SnackbarHostState,
    onNavigateToTerminal: () -> Unit
) {
    val installedApps by viewModel.installedApps.collectAsState()

    var selectedPackage by remember { mutableStateOf("") }
    var selectedPermission by remember { mutableStateOf(PERMISSION_PRESETS.first()) }
    var isAppDropdownExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadInstalledApps()
    }

    val exactCommand = remember(selectedPackage, selectedPermission) {
        val pkg = selectedPackage.trim().ifEmpty { "<package_name>" }
        val perm = selectedPermission.trim().ifEmpty { "<permission_name>" }
        "pm grant $pkg $perm"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Permission Grant Helper", style = MaterialTheme.typography.titleMedium) }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Grant sensitive Android permissions to any installed app via Shizuku shell without root or PC tethering.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Step 1: Target Package
            Text(
                text = "1. Target Application Package",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(8.dp))

            ExposedDropdownMenuBox(
                expanded = isAppDropdownExpanded,
                onExpandedChange = { isAppDropdownExpanded = it }
            ) {
                OutlinedTextField(
                    value = selectedPackage,
                    onValueChange = {
                        selectedPackage = it
                        isAppDropdownExpanded = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    label = { Text("Package Name") },
                    placeholder = { Text("e.g. com.example.myapp") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isAppDropdownExpanded) },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = JetBrainsMonoFontFamily)
                )

                val matchingApps = remember(selectedPackage, installedApps) {
                    if (selectedPackage.isBlank()) {
                        installedApps.take(25)
                    } else {
                        installedApps.filter {
                            it.packageName.contains(selectedPackage, ignoreCase = true) ||
                                    it.appName.contains(selectedPackage, ignoreCase = true)
                        }.take(25)
                    }
                }

                ExposedDropdownMenu(
                    expanded = isAppDropdownExpanded && matchingApps.isNotEmpty(),
                    onDismissRequest = { isAppDropdownExpanded = false }
                ) {
                    matchingApps.forEach { app ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(app.appName, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                                    Text(
                                        app.packageName,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontSize = 11.sp
                                        ),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            onClick = {
                                selectedPackage = app.packageName
                                isAppDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Step 2: Permission Selection
            Text(
                text = "2. Permission to Grant",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = selectedPermission,
                onValueChange = { selectedPermission = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Permission Name") },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = JetBrainsMonoFontFamily)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Common Permission Presets:",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                PERMISSION_PRESETS.forEach { preset ->
                    val shortName = preset.substringAfterLast(".")
                    FilterChip(
                        selected = selectedPermission == preset,
                        onClick = { selectedPermission = preset },
                        label = {
                            Text(
                                text = shortName,
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFontFamily)
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Step 3: Command Preview
            Text(
                text = "3. Exact Command Preview",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = exactCommand,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Run Button
            Button(
                onClick = {
                    if (selectedPackage.isNotBlank() && selectedPermission.isNotBlank()) {
                        viewModel.executeCommand("pm grant ${selectedPackage.trim()} ${selectedPermission.trim()}")
                        onNavigateToTerminal()
                    }
                },
                enabled = selectedPackage.isNotBlank() && selectedPermission.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Execute Grant via Shizuku")
            }
        }
    }
}
