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
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.acoustixaudio.opiqo.remotecontroloverssh.data.SshProfile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SshProfilesScreen(
    viewModel: SshProfilesViewModel,
    onOpenDrawer: () -> Unit
) {
    val profiles by viewModel.profiles.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingProfile by remember { mutableStateOf<SshProfile?>(null) }
    var profilePendingDelete by remember { mutableStateOf<SshProfile?>(null) }

    LaunchedEffect(viewModel) {
        viewModel.messages.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
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
                SshProfileItem(
                    profile = profile,
                    onEdit = {
                        editingProfile = profile
                        showAddDialog = true
                    },
                    onDelete = {
                        profilePendingDelete = profile
                    }
                )
            }
        }
    }

    if (showAddDialog) {
        AddSshProfileDialog(
            existingProfile = editingProfile,
            onDismiss = {
                showAddDialog = false
                editingProfile = null
            },
            onSave = { existingProfile, alias, host, port, username, keyUri, hostKeyFingerprint, context ->
                scope.launch {
                    if (viewModel.saveProfile(existingProfile, alias, host, port, username, keyUri, hostKeyFingerprint, context)) {
                        showAddDialog = false
                        editingProfile = null
                    }
                }
            }
        )
    }

    profilePendingDelete?.let { profile ->
        ConfirmDeleteDialog(
            title = "Delete SSH profile?",
            text = "Delete ${profile.alias}?",
            onDismiss = { profilePendingDelete = null },
            onConfirm = {
                viewModel.deleteProfile(profile)
                profilePendingDelete = null
            }
        )
    }
}

@Composable
fun SshProfileItem(
    profile: SshProfile,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

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
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Options")
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Edit") },
                        onClick = {
                            menuExpanded = false
                            onEdit()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSshProfileDialog(
    existingProfile: SshProfile? = null,
    onDismiss: () -> Unit,
    onSave: suspend (SshProfile?, String, String, Int, String, Uri?, String, android.content.Context) -> Unit
) {
    var alias by remember(existingProfile) { mutableStateOf(existingProfile?.alias.orEmpty()) }
    var host by remember(existingProfile) { mutableStateOf(existingProfile?.host.orEmpty()) }
    var port by remember(existingProfile) { mutableStateOf(existingProfile?.port?.toString() ?: "22") }
    var username by remember(existingProfile) { mutableStateOf(existingProfile?.username.orEmpty()) }
    var keyUri by remember(existingProfile) { mutableStateOf<Uri?>(null) }
    var hostKeyFingerprint by remember(existingProfile) { mutableStateOf(existingProfile?.hostKeyFingerprint.orEmpty()) }
    var keyName by remember(existingProfile) { mutableStateOf(if (existingProfile?.privateKeyPath != null) "Current key" else "Browse...") }
    var showValidationErrors by remember(existingProfile) { mutableStateOf(false) }
    var isSaving by remember(existingProfile) { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        keyUri = uri
        keyName = uri?.lastPathSegment ?: "Selected"
    }
    val validationErrors = validateSshProfileInput(
        alias = alias,
        host = host,
        port = port,
        username = username,
        hasPrivateKey = keyUri != null || existingProfile?.privateKeyPath != null,
        fingerprint = hostKeyFingerprint
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existingProfile == null) "Add SSH Connection" else "Edit SSH Connection") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = alias,
                    onValueChange = { alias = it },
                    label = { Text("Profile Name") },
                    isError = showValidationErrors && validationErrors.alias != null,
                    supportingText = {
                        if (showValidationErrors && validationErrors.alias != null) {
                            Text(validationErrors.alias)
                        }
                    }
                )
                OutlinedTextField(
                    value = host,
                    onValueChange = { host = it },
                    label = { Text("Host/IP") },
                    isError = showValidationErrors && validationErrors.host != null,
                    supportingText = {
                        if (showValidationErrors && validationErrors.host != null) {
                            Text(validationErrors.host)
                        }
                    }
                )
                OutlinedTextField(
                    value = port,
                    onValueChange = { port = it },
                    label = { Text("Port") },
                    isError = showValidationErrors && validationErrors.port != null,
                    supportingText = {
                        if (showValidationErrors && validationErrors.port != null) {
                            Text(validationErrors.port)
                        }
                    }
                )
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    isError = showValidationErrors && validationErrors.username != null,
                    supportingText = {
                        if (showValidationErrors && validationErrors.username != null) {
                            Text(validationErrors.username)
                        }
                    }
                )
                OutlinedTextField(
                    value = hostKeyFingerprint,
                    onValueChange = { hostKeyFingerprint = it },
                    label = { Text("Server Fingerprint") },
                    placeholder = { Text("SHA256:... or MD5:aa:bb:...") },
                    isError = showValidationErrors && validationErrors.fingerprint != null,
                    supportingText = {
                        if (showValidationErrors && validationErrors.fingerprint != null) {
                            Text(validationErrors.fingerprint)
                        }
                    }
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("SSH Key: $keyName", modifier = Modifier.weight(1f))
                    TextButton(onClick = { launcher.launch("*/*") }) {
                        Text("Browse")
                    }
                }
                if (showValidationErrors && validationErrors.privateKey != null) {
                    Text(
                        text = validationErrors.privateKey,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !isSaving,
                onClick = {
                    showValidationErrors = true
                    if (validationErrors.hasErrors()) {
                        return@Button
                    }
                    isSaving = true
                    scope.launch {
                        onSave(
                            existingProfile,
                            alias.trim(),
                            host.trim(),
                            port.toInt(),
                            username.trim(),
                            keyUri,
                            hostKeyFingerprint.trim(),
                            context
                        )
                        isSaving = false
                    }
                }
            ) {
                Text(if (existingProfile == null) "Save" else "Update")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ConfirmDeleteDialog(
    title: String,
    text: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
