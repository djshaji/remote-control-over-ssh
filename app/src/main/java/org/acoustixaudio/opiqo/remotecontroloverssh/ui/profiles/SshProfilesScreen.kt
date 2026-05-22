package org.acoustixaudio.opiqo.remotecontroloverssh.ui.profiles

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import org.acoustixaudio.opiqo.remotecontroloverssh.data.SshProfile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SshProfilesScreen(
    viewModel: SshProfilesViewModel,
    onOpenDrawer: () -> Unit
) {
    val profiles by viewModel.profiles.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SSH Connections") },
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
                text = { Text("Add Connection") }
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
                SshProfileItem(profile, onDelete = { viewModel.deleteProfile(profile) })
            }
        }
    }

    if (showAddDialog) {
        AddSshProfileDialog(
            onDismiss = { showAddDialog = false },
            onSave = { alias, host, port, username, keyUri, context ->
                viewModel.saveProfile(alias, host, port, username, keyUri, context)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun SshProfileItem(profile: SshProfile, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
                Text(profile.alias, style = MaterialTheme.typography.titleMedium)
                Text("${profile.username}@${profile.host}:${profile.port}", style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.MoreVert, contentDescription = "Options")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSshProfileDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, Int, String, Uri?, android.content.Context) -> Unit
) {
    var alias by remember { mutableStateOf("") }
    var host by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("22") }
    var username by remember { mutableStateOf("") }
    var keyUri by remember { mutableStateOf<Uri?>(null) }
    var keyName by remember { mutableStateOf("Browse...") }
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        keyUri = uri
        keyName = uri?.lastPathSegment ?: "Selected"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add SSH Connection") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = alias, onValueChange = { alias = it }, label = { Text("Profile Name") })
                OutlinedTextField(value = host, onValueChange = { host = it }, label = { Text("Host/IP") })
                OutlinedTextField(value = port, onValueChange = { port = it }, label = { Text("Port") })
                OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("Username") })
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("SSH Key: $keyName", modifier = Modifier.weight(1f))
                    TextButton(onClick = { launcher.launch("*/*") }) {
                        Text("Browse")
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(alias, host, port.toIntOrNull() ?: 22, username, keyUri, context) }) {
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
