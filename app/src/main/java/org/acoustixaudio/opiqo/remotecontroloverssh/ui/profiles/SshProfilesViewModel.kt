package org.acoustixaudio.opiqo.remotecontroloverssh.ui.profiles

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.acoustixaudio.opiqo.remotecontroloverssh.data.AppRepository
import org.acoustixaudio.opiqo.remotecontroloverssh.data.SshProfile
import org.acoustixaudio.opiqo.remotecontroloverssh.util.KeyFileHelper

class SshProfilesViewModel(
    private val repository: AppRepository
) : ViewModel() {
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    val profiles: StateFlow<List<SshProfile>> = repository.observeSshProfiles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    suspend fun saveProfile(
        existingProfile: SshProfile?,
        alias: String,
        host: String,
        port: Int,
        username: String,
        privateKeyUri: Uri?,
        hostKeyFingerprint: String,
        context: Context
    ): Boolean {
        val keyPath = privateKeyUri?.let { uri ->
            val result = KeyFileHelper.copyKeyToInternal(context, uri, "key_${System.currentTimeMillis()}")
            if (result.isFailure) {
                _messages.emit(result.exceptionOrNull()?.message ?: "Unable to copy the selected private key.")
                return false
            }
            result.getOrThrow()
        } ?: existingProfile?.privateKeyPath

        val profile = SshProfile(
            id = existingProfile?.id ?: 0,
            alias = alias.trim(),
            host = host.trim(),
            port = port,
            username = username.trim(),
            privateKeyPath = keyPath,
            hostKeyFingerprint = hostKeyFingerprint.trim()
        )
        if (privateKeyUri != null && existingProfile?.privateKeyPath != null && existingProfile.privateKeyPath != keyPath) {
            KeyFileHelper.deleteKey(existingProfile.privateKeyPath)
        }
        repository.saveSshProfile(profile)
        return true
    }

    fun deleteProfile(profile: SshProfile) {
        viewModelScope.launch {
            profile.privateKeyPath?.let { KeyFileHelper.deleteKey(it) }
            repository.deleteSshProfile(profile)
        }
    }
}
