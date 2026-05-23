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
import kotlinx.coroutines.launch
import org.acoustixaudio.opiqo.remotecontroloverssh.data.BuiltInRemoteProfile
import org.acoustixaudio.opiqo.remotecontroloverssh.data.RemoteControlConfig
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
    val builtInProfiles by viewModel.builtInProfiles.collectAsState()
    val scope = rememberCoroutineScope()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingProfileData by remember { mutableStateOf<RemoteProfileEditorState?>(null) }
    var profilePendingDelete by remember { mutableStateOf<RemoteProfile?>(null) }

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
                    onEdit = {
                        scope.launch {
                            val editorData = viewModel.getRemoteProfileEditorData(profile.id) ?: return@launch
                            editingProfileData = RemoteProfileEditorState(
                                profile = editorData.remoteProfile,
                                commands = editorData.commands.associate { it.buttonIdentifier to it.commandString }
                            )
                            showAddDialog = true
                        }
                    },
                    onDelete = { profilePendingDelete = profile }
                )
            }
        }
    }

    if (showAddDialog) {
        AddRemoteProfileDialog(
            existingProfile = editingProfileData,
            sshProfiles = sshProfiles,
            builtInProfiles = builtInProfiles,
            onDismiss = {
                showAddDialog = false
                editingProfileData = null
            },
            onSave = { existingProfile, name, sshId, commands ->
                scope.launch {
                    if (viewModel.saveRemoteProfile(existingProfile?.profile, name, sshId, commands)) {
                        showAddDialog = false
                        editingProfileData = null
                    }
                }
            }
        )
    }

    profilePendingDelete?.let { profile ->
        ConfirmDeleteDialog(
            title = "Delete remote profile?",
            text = "Delete ${profile.name}?",
            onDismiss = { profilePendingDelete = null },
            onConfirm = {
                viewModel.deleteRemoteProfile(profile)
                profilePendingDelete = null
            }
        )
    }
}

@Composable
fun RemoteProfileItem(
    profile: RemoteProfile, 
    sshProfile: SshProfile?, 
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
fun AddRemoteProfileDialog(
    existingProfile: RemoteProfileEditorState? = null,
    sshProfiles: List<SshProfile>,
    builtInProfiles: List<BuiltInRemoteProfile>,
    onDismiss: () -> Unit,
    onSave: suspend (RemoteProfileEditorState?, String, Long?, Map<String, String>) -> Unit
) {
    var name by remember(existingProfile) { mutableStateOf(existingProfile?.profile?.name.orEmpty()) }
    var selectedSshId by remember(existingProfile) { mutableStateOf(existingProfile?.profile?.sshProfileId) }
    var expanded by remember(existingProfile) { mutableStateOf(false) }
    var builtInExpanded by remember(existingProfile) { mutableStateOf(false) }
    var selectedBuiltInId by remember(existingProfile) { mutableStateOf<String?>(null) }
    var showValidationErrors by remember(existingProfile) { mutableStateOf(false) }
    var isSaving by remember(existingProfile) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val slider1Commands = remember(existingProfile) {
        mutableStateListOf(
            *List(RemoteControlConfig.sliderSteps.count()) { step ->
                existingProfile?.commands?.get(
                    RemoteControlConfig.sliderStepIdentifier(RemoteControlConfig.SLIDER_1, step)
                ).orEmpty()
            }.toTypedArray()
        )
    }
    var upCmd by remember(existingProfile) { mutableStateOf(existingProfile?.commands?.get(RemoteControlConfig.DPAD_UP).orEmpty()) }
    var downCmd by remember(existingProfile) { mutableStateOf(existingProfile?.commands?.get(RemoteControlConfig.DPAD_DOWN).orEmpty()) }
    var leftCmd by remember(existingProfile) { mutableStateOf(existingProfile?.commands?.get(RemoteControlConfig.DPAD_LEFT).orEmpty()) }
    var rightCmd by remember(existingProfile) { mutableStateOf(existingProfile?.commands?.get(RemoteControlConfig.DPAD_RIGHT).orEmpty()) }
    var selectCmd by remember(existingProfile) { mutableStateOf(existingProfile?.commands?.get(RemoteControlConfig.DPAD_SELECT).orEmpty()) }
    var backCmd by remember(existingProfile) { mutableStateOf(existingProfile?.commands?.get(RemoteControlConfig.BTN_BACK).orEmpty()) }
    var homeCmd by remember(existingProfile) { mutableStateOf(existingProfile?.commands?.get(RemoteControlConfig.BTN_HOME).orEmpty()) }
    val slider2Commands = remember(existingProfile) {
        mutableStateListOf(
            *List(RemoteControlConfig.sliderSteps.count()) { step ->
                existingProfile?.commands?.get(
                    RemoteControlConfig.sliderStepIdentifier(RemoteControlConfig.SLIDER_2, step)
                ).orEmpty()
            }.toTypedArray()
        )
    }
    val validationErrors = validateRemoteProfileInput(name)

    fun applyBuiltInProfile(profile: BuiltInRemoteProfile) {
        selectedBuiltInId = profile.id
        name = profile.name
        upCmd = profile.commands[RemoteControlConfig.DPAD_UP].orEmpty()
        downCmd = profile.commands[RemoteControlConfig.DPAD_DOWN].orEmpty()
        leftCmd = profile.commands[RemoteControlConfig.DPAD_LEFT].orEmpty()
        rightCmd = profile.commands[RemoteControlConfig.DPAD_RIGHT].orEmpty()
        selectCmd = profile.commands[RemoteControlConfig.DPAD_SELECT].orEmpty()
        backCmd = profile.commands[RemoteControlConfig.BTN_BACK].orEmpty()
        homeCmd = profile.commands[RemoteControlConfig.BTN_HOME].orEmpty()
        RemoteControlConfig.sliderSteps.forEach { step ->
            slider1Commands[step] = profile.commands[
                RemoteControlConfig.sliderStepIdentifier(RemoteControlConfig.SLIDER_1, step)
            ].orEmpty()
            slider2Commands[step] = profile.commands[
                RemoteControlConfig.sliderStepIdentifier(RemoteControlConfig.SLIDER_2, step)
            ].orEmpty()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existingProfile == null) "Add Remote Profile" else "Edit Remote Profile") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Remote Name") },
                        isError = showValidationErrors && validationErrors.name != null,
                        supportingText = {
                            if (showValidationErrors && validationErrors.name != null) {
                                Text(validationErrors.name)
                            }
                        }
                    )
                }
                item {
                    ExposedDropdownMenuBox(
                        expanded = builtInExpanded,
                        onExpandedChange = { builtInExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = builtInProfiles.find { it.id == selectedBuiltInId }?.name ?: "None",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Built-in Template") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = builtInExpanded)
                            },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                        )
                        ExposedDropdownMenu(
                            expanded = builtInExpanded,
                            onDismissRequest = { builtInExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("None") },
                                onClick = {
                                    selectedBuiltInId = null
                                    builtInExpanded = false
                                }
                            )
                            builtInProfiles.forEach { profile ->
                                DropdownMenuItem(
                                    text = { Text(profile.name) },
                                    onClick = {
                                        applyBuiltInProfile(profile)
                                        builtInExpanded = false
                                    }
                                )
                            }
                        }
                    }
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
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
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
                item { Text("Slider 1 step commands", style = MaterialTheme.typography.titleSmall) }
                items(RemoteControlConfig.sliderSteps.toList()) { step ->
                    OutlinedTextField(
                        value = slider1Commands[step],
                        onValueChange = { slider1Commands[step] = it },
                        label = { Text("Slider 1 step $step") }
                    )
                }
                item { OutlinedTextField(value = upCmd, onValueChange = { upCmd = it }, label = { Text("D-Pad Up") }) }
                item { OutlinedTextField(value = downCmd, onValueChange = { downCmd = it }, label = { Text("D-Pad Down") }) }
                item { OutlinedTextField(value = leftCmd, onValueChange = { leftCmd = it }, label = { Text("D-Pad Left") }) }
                item { OutlinedTextField(value = rightCmd, onValueChange = { rightCmd = it }, label = { Text("D-Pad Right") }) }
                item { OutlinedTextField(value = selectCmd, onValueChange = { selectCmd = it }, label = { Text("D-Pad Select") }) }
                item { OutlinedTextField(value = backCmd, onValueChange = { backCmd = it }, label = { Text("Back Button") }) }
                item { OutlinedTextField(value = homeCmd, onValueChange = { homeCmd = it }, label = { Text("Home Button") }) }
                item { Text("Slider 2 step commands", style = MaterialTheme.typography.titleSmall) }
                items(RemoteControlConfig.sliderSteps.toList()) { step ->
                    OutlinedTextField(
                        value = slider2Commands[step],
                        onValueChange = { slider2Commands[step] = it },
                        label = { Text("Slider 2 step $step") }
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
                    val commands = buildMap {
                        RemoteControlConfig.sliderSteps.forEach { step ->
                            put(
                                RemoteControlConfig.sliderStepIdentifier(RemoteControlConfig.SLIDER_1, step),
                                slider1Commands[step]
                            )
                        }
                        put(RemoteControlConfig.DPAD_UP, upCmd)
                        put(RemoteControlConfig.DPAD_DOWN, downCmd)
                        put(RemoteControlConfig.DPAD_LEFT, leftCmd)
                        put(RemoteControlConfig.DPAD_RIGHT, rightCmd)
                        put(RemoteControlConfig.DPAD_SELECT, selectCmd)
                        put(RemoteControlConfig.BTN_BACK, backCmd)
                        put(RemoteControlConfig.BTN_HOME, homeCmd)
                        RemoteControlConfig.sliderSteps.forEach { step ->
                            put(
                                RemoteControlConfig.sliderStepIdentifier(RemoteControlConfig.SLIDER_2, step),
                                slider2Commands[step]
                            )
                        }
                    }
                    isSaving = true
                    scope.launch {
                        onSave(existingProfile, name.trim(), selectedSshId, commands)
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

data class RemoteProfileEditorState(
    val profile: RemoteProfile,
    val commands: Map<String, String>
)
