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
import org.acoustixaudio.opiqo.remotecontroloverssh.ssh.SshResult
import org.acoustixaudio.opiqo.remotecontroloverssh.ui.components.ConnectionStatusChip

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SshProfilesScreen(
    viewModel: SshProfilesViewModel,
    onOpenDrawer: () -> Unit
) {
    val profiles by viewModel.profiles.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val connectedAlias = profiles
        .firstOrNull { it.id == connectionState.connectedProfileId }
        ?.alias
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
                actions = {
                    ConnectionStatusChip(isConnected = connectedAlias != null)
                    Spacer(Modifier.width(8.dp))
                },
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
            connectedAlias?.let { alias ->
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text(
                            text = "Connected to: $alias",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                        )
                    }
                }
            }

            if (connectionState.connectedProfileId == null) {
                item {
                    Text(
                        text = "Tap Connect on an SSH profile to start a session.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            items(profiles) { profile ->
                SshProfileItem(
                    profile = profile,
                    isConnected = connectionState.connectedProfileId == profile.id,
                    isConnecting = connectionState.isConnecting,
                    onConnect = { viewModel.connectProfile(profile) },
                    onDisconnect = { viewModel.disconnectProfile() },
                    onClick = {
                        editingProfile = profile
                        showAddDialog = true
                    },
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
            onFetchFingerprint = { host, port ->
                viewModel.fetchServerFingerprint(host, port)
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

    connectionState.pendingFingerprintReplacement?.let { replacement ->
        AlertDialog(
            onDismissRequest = viewModel::dismissFingerprintReplacementPrompt,
            title = { Text("Replace fingerprint?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Connection to ${replacement.profileAlias} failed due to host key mismatch.")
                    Text(
                        text = "Host: ${replacement.host}:${replacement.port}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Saved fingerprint:",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Text(
                        text = replacement.savedFingerprint,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Server presented:",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Text(
                        text = replacement.fingerprint,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text("Replace saved fingerprint and reconnect now?")
                }
            },
            confirmButton = {
                Button(onClick = viewModel::replaceFingerprintAndReconnect) {
                    Text("Replace & Connect")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        enabled = !connectionState.isFetchingReplacementFingerprint,
                        onClick = viewModel::refetchFingerprintReplacement
                    ) {
                        Text(
                            if (connectionState.isFetchingReplacementFingerprint) {
                                "Fetching..."
                            } else {
                                "Fetch Again"
                            }
                        )
                    }
                    TextButton(onClick = viewModel::dismissFingerprintReplacementPrompt) {
                        Text("Cancel")
                    }
                }
            }
        )
    }
}

@Composable
fun SshProfileItem(
    profile: SshProfile,
    isConnected: Boolean,
    isConnecting: Boolean,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(if (isConnected) Color(0xFF4CAF50) else Color(0xFF9E9E9E))
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(profile.alias, style = MaterialTheme.typography.titleMedium)
                    Text("${profile.username}@${profile.host}:${profile.port}", style = MaterialTheme.typography.bodySmall)
                    Text(
                        if (isConnected) "Connected" else "Disconnected",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                if (isConnected) {
                    OutlinedButton(
                        enabled = !isConnecting,
                        onClick = onDisconnect
                    ) {
                        Text("Disconnect")
                    }
                } else {
                    Button(
                        enabled = !isConnecting,
                        onClick = onConnect
                    ) {
                        Text(if (isConnecting) "Connecting..." else "Connect")
                    }
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
    onFetchFingerprint: suspend (String, Int) -> SshResult<String>,
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
    var isFetchingFingerprint by remember(existingProfile) { mutableStateOf(false) }
    var fetchFingerprintError by remember(existingProfile) { mutableStateOf<String?>(null) }
    var pendingFetchedFingerprint by remember(existingProfile) { mutableStateOf<String?>(null) }
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
                        when {
                            fetchFingerprintError != null -> Text(fetchFingerprintError!!)
                            showValidationErrors && validationErrors.fingerprint != null -> {
                                Text(validationErrors.fingerprint)
                            }
                        }
                    }
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        enabled = !isFetchingFingerprint,
                        onClick = {
                            fetchFingerprintError = null
                            val trimmedHost = host.trim()
                            val parsedPort = port.toIntOrNull()
                            if (trimmedHost.isBlank()) {
                                fetchFingerprintError = "Enter a host before fetching the fingerprint."
                                return@TextButton
                            }
                            if (parsedPort == null || parsedPort !in 1..65535) {
                                fetchFingerprintError = "Enter a valid port before fetching the fingerprint."
                                return@TextButton
                            }

                            isFetchingFingerprint = true
                            scope.launch {
                                when (val result = onFetchFingerprint(trimmedHost, parsedPort)) {
                                    is SshResult.Success -> {
                                        pendingFetchedFingerprint = result.value
                                    }
                                    is SshResult.Error -> {
                                        fetchFingerprintError = result.message
                                    }
                                }
                                isFetchingFingerprint = false
                            }
                        }
                    ) {
                        Text(if (isFetchingFingerprint) "Fetching..." else "Fetch Fingerprint")
                    }
                }

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

    pendingFetchedFingerprint?.let { fingerprint ->
        AlertDialog(
            onDismissRequest = { pendingFetchedFingerprint = null },
            title = { Text("Use fetched fingerprint?") },
            text = { Text(fingerprint) },
            confirmButton = {
                Button(
                    onClick = {
                        hostKeyFingerprint = fingerprint
                        fetchFingerprintError = null
                        pendingFetchedFingerprint = null
                    }
                ) {
                    Text("Use Fingerprint")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingFetchedFingerprint = null }) {
                    Text("Cancel")
                }
            }
        )
    }
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
