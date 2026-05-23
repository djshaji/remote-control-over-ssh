package org.acoustixaudio.opiqo.remotecontroloverssh.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.acoustixaudio.opiqo.remotecontroloverssh.data.AppRepository
import org.acoustixaudio.opiqo.remotecontroloverssh.data.RemoteCommand
import org.acoustixaudio.opiqo.remotecontroloverssh.data.RemoteControlConfig
import org.acoustixaudio.opiqo.remotecontroloverssh.data.RemoteProfile
import org.acoustixaudio.opiqo.remotecontroloverssh.data.SshProfile
import org.acoustixaudio.opiqo.remotecontroloverssh.ssh.SshClient
import org.acoustixaudio.opiqo.remotecontroloverssh.ssh.SshResult

class DashboardViewModel(
    private val repository: AppRepository,
    private val sshClient: SshClient,
    private val remoteProfileId: Long
) : ViewModel() {
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    private val slider1Flow = MutableSharedFlow<Int>()
    private val slider2Flow = MutableSharedFlow<Int>()

    init {
        loadData()
        setupSliderDebounce()
    }

    private fun loadData() {
        viewModelScope.launch {
            val dashboardData = repository.getDashboardData(remoteProfileId)
            if (dashboardData == null) {
                _uiState.update {
                    it.copy(connectionStatus = "Remote profile unavailable")
                }
                _messages.emit("The selected remote profile could not be loaded.")
                return@launch
            }

            _uiState.update { it.copy(
                remoteProfile = dashboardData.remoteProfile,
                sshProfile = dashboardData.sshProfile,
                commands = dashboardData.commands.associateBy { command -> command.buttonIdentifier },
                connectionStatus = initialConnectionStatus(dashboardData.sshProfile),
                isConnected = false,
                isConnecting = false
            ) }
        }
    }

    private fun initialConnectionStatus(profile: SshProfile?): String {
        if (profile == null) return "No SSH profile selected"
        if (profile.hostKeyFingerprint.isNullOrBlank()) return "SSH profile missing host fingerprint"
        if (profile.privateKeyPath.isNullOrBlank()) return "SSH profile missing private key"
        return "Disconnected"
    }

    private suspend fun connectSsh(profile: SshProfile) {
        _uiState.update { it.copy(connectionStatus = "Connecting...", isConnected = false, isConnecting = true) }
        when (val result = sshClient.connect(profile)) {
            is SshResult.Success -> _uiState.update {
                it.copy(connectionStatus = "Connected", isConnected = true, isConnecting = false)
            }
            is SshResult.Error -> {
                _uiState.update {
                    it.copy(connectionStatus = result.message, isConnected = false, isConnecting = false)
                }
                _messages.emit(result.message)
            }
        }
    }

    fun onConnectClick() {
        viewModelScope.launch {
            val profile = _uiState.value.sshProfile
            if (profile == null) {
                _messages.emit("Select an SSH profile for this remote before connecting.")
                _uiState.update { it.copy(connectionStatus = "No SSH profile selected", isConnected = false) }
                return@launch
            }
            connectSsh(profile)
        }
    }

    fun onDisconnectClick() {
        viewModelScope.launch {
            when (val result = sshClient.disconnect()) {
                is SshResult.Success -> {
                    _uiState.update {
                        it.copy(connectionStatus = initialConnectionStatus(it.sshProfile), isConnected = false, isConnecting = false)
                    }
                }
                is SshResult.Error -> {
                    _messages.emit(result.message)
                }
            }
        }
    }

    @OptIn(FlowPreview::class)
    private fun setupSliderDebounce() {
        viewModelScope.launch {
            slider1Flow.debounce(100L).collect { value ->
                executeSliderCommand(RemoteControlConfig.SLIDER_1, value)
            }
        }
        viewModelScope.launch {
            slider2Flow.debounce(100L).collect { value ->
                executeSliderCommand(RemoteControlConfig.SLIDER_2, value)
            }
        }
    }

    fun onSlider1Change(value: Int) {
        _uiState.update { it.copy(slider1Value = value) }
        viewModelScope.launch { slider1Flow.emit(value) }
    }

    fun onSlider2Change(value: Int) {
        _uiState.update { it.copy(slider2Value = value) }
        viewModelScope.launch { slider2Flow.emit(value) }
    }

    fun onButtonClick(buttonId: String) {
        viewModelScope.launch {
            val command = _uiState.value.commands[buttonId]
                ?.commandString
                ?.takeIf { it.isNotBlank() }
                ?: return@launch
            executeCommand(command)
        }
    }

    private suspend fun executeSliderCommand(sliderId: String, value: Int) {
        val command = RemoteControlConfig.resolveSliderCommand(
            commands = _uiState.value.commands,
            sliderId = sliderId,
            step = value
        ) ?: return
        executeCommand(command)
    }

    private suspend fun executeCommand(command: String) {
        if (!_uiState.value.isConnected) {
            _messages.emit("SSH is disconnected. Connect first.")
            return
        }
        when (val result = sshClient.executeCommand(command)) {
            is SshResult.Success -> Unit
            is SshResult.Error -> _messages.emit(result.message)
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            sshClient.disconnect()
        }
    }
}

data class DashboardUiState(
    val remoteProfile: RemoteProfile? = null,
    val sshProfile: SshProfile? = null,
    val commands: Map<String, RemoteCommand> = emptyMap(),
    val connectionStatus: String = "Disconnected",
    val isConnected: Boolean = false,
    val isConnecting: Boolean = false,
    val slider1Value: Int = 0,
    val slider2Value: Int = 0
)
