package org.acoustixaudio.opiqo.remotecontroloverssh.ui.profiles

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.acoustixaudio.opiqo.remotecontroloverssh.data.AppRepository
import org.acoustixaudio.opiqo.remotecontroloverssh.data.SshProfile
import org.acoustixaudio.opiqo.remotecontroloverssh.ssh.SshClient
import org.acoustixaudio.opiqo.remotecontroloverssh.ssh.SshResult
import org.acoustixaudio.opiqo.remotecontroloverssh.util.KeyFileHelper

class SshProfilesViewModel(
    private val repository: AppRepository,
    private val sshClientFactory: () -> SshClient
) : ViewModel() {
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    private val _connectionState = MutableStateFlow(SshConnectionState())
    val connectionState = _connectionState.asStateFlow()

    private var connectedClient: SshClient? = null

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

    suspend fun fetchServerFingerprint(host: String, port: Int): SshResult<String> {
        val sshClient = sshClientFactory()
        return try {
            sshClient.fetchServerFingerprint(host, port)
        } finally {
            sshClient.disconnect()
        }
    }

    fun connectProfile(profile: SshProfile) {
        viewModelScope.launch {
            _connectionState.value = _connectionState.value.copy(isConnecting = true)
            connectedClient?.disconnect()
            connectedClient = null

            val sshClient = sshClientFactory()
            when (val result = sshClient.connect(profile)) {
                is SshResult.Success -> {
                    connectedClient = sshClient
                    _connectionState.value = SshConnectionState(
                        connectedProfileId = profile.id,
                        isConnecting = false
                    )
                    _messages.emit("Connected to ${profile.alias}.")
                }
                is SshResult.Error -> {
                    sshClient.disconnect()
                    val pendingReplacement = resolveFingerprintReplacementPrompt(profile, result.message)
                    _connectionState.value = SshConnectionState(
                        connectedProfileId = null,
                        isConnecting = false,
                        lastError = result.message,
                        pendingFingerprintReplacement = pendingReplacement
                    )
                    _messages.emit(result.message)
                }
            }
        }
    }

    fun replaceFingerprintAndReconnect() {
        viewModelScope.launch {
            val replacement = _connectionState.value.pendingFingerprintReplacement ?: return@launch
            val currentProfile = repository.getSshProfile(replacement.profileId)
            if (currentProfile == null) {
                _connectionState.value = _connectionState.value.copy(pendingFingerprintReplacement = null)
                _messages.emit("SSH profile no longer exists.")
                return@launch
            }

            val updatedProfile = currentProfile.copy(hostKeyFingerprint = replacement.fingerprint)
            repository.saveSshProfile(updatedProfile)
            _connectionState.value = _connectionState.value.copy(
                pendingFingerprintReplacement = null,
                lastError = null
            )
            _messages.emit("Fingerprint updated. Reconnecting...")
            connectProfile(updatedProfile)
        }
    }

    fun refetchFingerprintReplacement() {
        viewModelScope.launch {
            val replacement = _connectionState.value.pendingFingerprintReplacement ?: return@launch
            _connectionState.value = _connectionState.value.copy(isFetchingReplacementFingerprint = true)

            when (val result = fetchServerFingerprint(replacement.host, replacement.port)) {
                is SshResult.Success -> {
                    _connectionState.value = _connectionState.value.copy(
                        isFetchingReplacementFingerprint = false,
                        pendingFingerprintReplacement = replacement.copy(fingerprint = result.value)
                    )
                    _messages.emit("Fetched latest server fingerprint.")
                }

                is SshResult.Error -> {
                    _connectionState.value = _connectionState.value.copy(isFetchingReplacementFingerprint = false)
                    _messages.emit(result.message)
                }
            }
        }
    }

    fun dismissFingerprintReplacementPrompt() {
        _connectionState.value = _connectionState.value.copy(
            pendingFingerprintReplacement = null,
            isFetchingReplacementFingerprint = false
        )
    }

    fun disconnectProfile() {
        viewModelScope.launch {
            val client = connectedClient
            connectedClient = null
            _connectionState.value = _connectionState.value.copy(isConnecting = true)
            if (client != null) {
                when (val result = client.disconnect()) {
                    is SshResult.Success -> {
                        _connectionState.value = SshConnectionState()
                    }
                    is SshResult.Error -> {
                        _connectionState.value = SshConnectionState(lastError = result.message)
                        _messages.emit(result.message)
                    }
                }
            } else {
                _connectionState.value = SshConnectionState()
            }
        }
    }

    fun deleteProfile(profile: SshProfile) {
        viewModelScope.launch {
            if (_connectionState.value.connectedProfileId == profile.id) {
                disconnectProfile()
            }
            profile.privateKeyPath?.let { KeyFileHelper.deleteKey(it) }
            repository.deleteSshProfile(profile)
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            connectedClient?.disconnect()
            connectedClient = null
        }
    }

    private suspend fun resolveFingerprintReplacementPrompt(
        profile: SshProfile,
        connectErrorMessage: String
    ): FingerprintReplacementPrompt? {
        if (!isFingerprintVerificationFailure(connectErrorMessage)) {
            return null
        }

        return when (val fingerprintResult = fetchServerFingerprint(profile.host, profile.port)) {
            is SshResult.Success -> {
                if (fingerprintResult.value == profile.hostKeyFingerprint?.trim()) {
                    null
                } else {
                    FingerprintReplacementPrompt(
                        profileId = profile.id,
                        profileAlias = profile.alias,
                        host = profile.host,
                        port = profile.port,
                        savedFingerprint = profile.hostKeyFingerprint?.trim().orEmpty(),
                        fingerprint = fingerprintResult.value
                    )
                }
            }
            is SshResult.Error -> null
        }
    }

    private fun isFingerprintVerificationFailure(message: String): Boolean {
        val normalized = message.lowercase()
        if (normalized.contains("fingerprint mismatch")) return true
        if (normalized.contains("verify host key")) return true
        return normalized.contains("verify") && normalized.contains("fingerprint")
    }
}

data class SshConnectionState(
    val connectedProfileId: Long? = null,
    val isConnecting: Boolean = false,
    val lastError: String? = null,
    val pendingFingerprintReplacement: FingerprintReplacementPrompt? = null,
    val isFetchingReplacementFingerprint: Boolean = false
)

data class FingerprintReplacementPrompt(
    val profileId: Long,
    val profileAlias: String,
    val host: String,
    val port: Int,
    val savedFingerprint: String,
    val fingerprint: String
)
