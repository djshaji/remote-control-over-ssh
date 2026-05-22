package org.acoustixaudio.opiqo.remotecontroloverssh.ui.profiles

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.acoustixaudio.opiqo.remotecontroloverssh.data.RemoteProfile
import org.acoustixaudio.opiqo.remotecontroloverssh.data.SshProfile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemoteProfilesScreen(
    viewModel: RemoteProfilesViewModel,
    onOpenDrawer: () -> Unit,
    onRemoteClick: (Long) -> Unit
) {
    val profiles by viewModel.remoteProfiles.collectAsState()
    val sshProfiles by viewModel.sshProfiles.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Remote Profiles") },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add Remote") }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(profiles) { profile ->
                RemoteProfileItem(
                    profile, 
                    sshProfiles.find { it.id == profile.sshProfileId },
                    onClick = { onRemoteClick(profile.id) },
                    onDelete = { viewModel.deleteRemoteProfile(profile) }
                )
            }
        }
    }

    if (showAddDialog) {
        AddRemoteProfileDialog(
            sshProfiles = sshProfiles,
            onDismiss = { showAddDialog = false },
            onSave = { name, sshId, commands ->
                viewModel.saveRemoteProfile(name, sshId, commands)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun RemoteProfileItem(
    profile: RemoteProfile, 
    sshProfile: SshProfile?, 
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF4CAF50)) // Green status dot
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(profile.name, style = MaterialTheme.typography.titleMedium)
                Text(sshProfile?.alias ?: "No Connection", style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.MoreVert, contentDescription = "Options")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRemoteProfileDialog(
    sshProfiles: List<SshProfile>,
    onDismiss: () -> Unit,
    onSave: (String, Long?, Map<String, String>) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedSshId by remember { mutableStateOf<Long?>(null) }
    var expanded by remember { mutableStateOf(false) }
    
    // Command mapping states
    var slider1Cmd by remember { mutableStateOf("") }
    var upCmd by remember { mutableStateOf("") }
    var downCmd by remember { mutableStateOf("") }
    var leftCmd by remember { mutableStateOf("") }
    var rightCmd by remember { mutableStateOf("") }
    var selectCmd by remember { mutableStateOf("") }
    var backCmd by remember { mutableStateOf("") }
    var homeCmd by remember { mutableStateOf("") }
    var slider2Cmd by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Remote Profile") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Remote Name") })
                }
                item {
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = it }
                    ) {
                        OutlinedTextField(
                            value = sshProfiles.find { it.id == selectedSshId }?.alias ?: "Select Connection",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("SSH Connection") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier.menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            sshProfiles.forEach { profile ->
                                DropdownMenuItem(
                                    text = { Text(profile.alias) },
                                    onClick = {
                                        selectedSshId = profile.id
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                item { Text("Button Mapping", style = MaterialTheme.typography.titleSmall) }
                item { OutlinedTextField(value = slider1Cmd, onValueChange = { slider1Cmd = it }, label = { Text("Slider 1 (%val%)") }) }
                item { OutlinedTextField(value = upCmd, onValueChange = { upCmd = it }, label = { Text("D-Pad Up") }) }
                item { OutlinedTextField(value = downCmd, onValueChange = { downCmd = it }, label = { Text("D-Pad Down") }) }
                item { OutlinedTextField(value = leftCmd, onValueChange = { leftCmd = it }, label = { Text("D-Pad Left") }) }
                item { OutlinedTextField(value = rightCmd, onValueChange = { rightCmd = it }, label = { Text("D-Pad Right") }) }
                item { OutlinedTextField(value = selectCmd, onValueChange = { selectCmd = it }, label = { Text("D-Pad Select") }) }
                item { OutlinedTextField(value = backCmd, onValueChange = { backCmd = it }, label = { Text("Back Button") }) }
                item { OutlinedTextField(value = homeCmd, onValueChange = { homeCmd = it }, label = { Text("Home Button") }) }
                item { OutlinedTextField(value = slider2Cmd, onValueChange = { slider2Cmd = it }, label = { Text("Slider 2 (%val%)") }) }
            }
        },
        confirmButton = {
            Button(onClick = {
                val commands = mapOf(
                    "SLIDER_1" to slider1Cmd,
                    "DPAD_UP" to upCmd,
                    "DPAD_DOWN" to downCmd,
                    "DPAD_LEFT" to leftCmd,
                    "DPAD_RIGHT" to rightCmd,
                    "DPAD_SELECT" to selectCmd,
                    "BTN_BACK" to backCmd,
                    "BTN_HOME" to homeCmd,
                    "SLIDER_2" to slider2Cmd
                )
                onSave(name, selectedSshId, commands)
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
