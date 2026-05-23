package org.acoustixaudio.opiqo.remotecontroloverssh.ui.profiles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.acoustixaudio.opiqo.remotecontroloverssh.data.AppRepository
import org.acoustixaudio.opiqo.remotecontroloverssh.data.BuiltInRemoteProfile
import org.acoustixaudio.opiqo.remotecontroloverssh.data.BuiltInRemoteProfilesStore
import org.acoustixaudio.opiqo.remotecontroloverssh.data.RemoteProfile
import org.acoustixaudio.opiqo.remotecontroloverssh.data.RemoteProfileEditorData
import org.acoustixaudio.opiqo.remotecontroloverssh.data.SshProfile

class RemoteProfilesViewModel(
    private val repository: AppRepository,
    private val builtInRemoteProfilesStore: BuiltInRemoteProfilesStore
) : ViewModel() {
    private val _builtInProfiles = MutableStateFlow<List<BuiltInRemoteProfile>>(emptyList())
    val builtInProfiles: StateFlow<List<BuiltInRemoteProfile>> = _builtInProfiles.asStateFlow()

    val remoteProfiles: StateFlow<List<RemoteProfile>> = repository.observeRemoteProfiles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sshProfiles: StateFlow<List<SshProfile>> = repository.observeSshProfiles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            _builtInProfiles.value = builtInRemoteProfilesStore.loadProfiles()
        }
    }

    suspend fun saveRemoteProfile(
        existingProfile: RemoteProfile?,
        name: String,
        sshProfileId: Long?,
        commands: Map<String, String>
    ): Boolean {
        repository.saveRemoteProfile(
            profile = RemoteProfile(
                id = existingProfile?.id ?: 0,
                name = name.trim(),
                sshProfileId = sshProfileId
            ),
            commands = commands
        )
        return true
    }

    suspend fun getRemoteProfileEditorData(remoteProfileId: Long): RemoteProfileEditorData? {
        return repository.getRemoteProfileEditorData(remoteProfileId)
    }

    fun deleteRemoteProfile(profile: RemoteProfile) {
        viewModelScope.launch {
            repository.deleteRemoteProfile(profile)
        }
    }
}
