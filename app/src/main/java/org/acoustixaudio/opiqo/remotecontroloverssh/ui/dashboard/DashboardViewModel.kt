package org.acoustixaudio.opiqo.remotecontroloverssh.ui.dashboard

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.acoustixaudio.opiqo.remotecontroloverssh.data.AppDatabase
import org.acoustixaudio.opiqo.remotecontroloverssh.data.RemoteCommand
import org.acoustixaudio.opiqo.remotecontroloverssh.data.RemoteProfile
import org.acoustixaudio.opiqo.remotecontroloverssh.data.SshProfile
import org.acoustixaudio.opiqo.remotecontroloverssh.ssh.SshManager

class DashboardViewModel(context: Context, private val remoteProfileId: Long) : ViewModel() {
    private val db = AppDatabase.getDatabase(context)
    private val remoteProfileDao = db.remoteProfileDao()
    private val sshDao = db.sshDao()
    private val commandDao = db.remoteCommandDao()
    private val sshManager = SshManager()

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private val slider1Flow = MutableSharedFlow<Int>()
    private val slider2Flow = MutableSharedFlow<Int>()

    init {
        loadData()
        setupSliderDebounce()
    }

    private fun loadData() {
        viewModelScope.launch {
            val remoteProfile = remoteProfileDao.getRemoteProfileById(remoteProfileId) ?: return@launch
            val sshProfile = remoteProfile.sshProfileId?.let { sshDao.getProfileById(it) }
            val commands = commandDao.getCommandsForProfile(remoteProfileId).first()

            _uiState.update { it.copy(
                remoteProfile = remoteProfile,
                sshProfile = sshProfile,
                commands = commands.associateBy { c -> c.buttonIdentifier }
            ) }

            sshProfile?.let { connectSsh(it) }
        }
    }

    private suspend fun connectSsh(profile: SshProfile) {
        _uiState.update { it.copy(connectionStatus = "Connecting...") }
        val success = sshManager.connect(profile)
        _uiState.update { it.copy(
            connectionStatus = if (success) "Connected" else "Connection Failed",
            isConnected = success
        ) }
    }

    @OptIn(FlowPreview::class)
    private fun setupSliderDebounce() {
        viewModelScope.launch {
            slider1Flow.debounce(100L).collect { value ->
                executeCommandWithVal("SLIDER_1", value)
            }
        }
        viewModelScope.launch {
            slider2Flow.debounce(100L).collect { value ->
                executeCommandWithVal("SLIDER_2", value)
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
            val cmdTemplate = _uiState.value.commands[buttonId]?.commandString ?: return@launch
            sshManager.executeCommand(cmdTemplate)
        }
    }

    private suspend fun executeCommandWithVal(buttonId: String, value: Int) {
        val cmdTemplate = _uiState.value.commands[buttonId]?.commandString ?: return
        val finalCmd = cmdTemplate.replace("%val%", value.toString())
        sshManager.executeCommand(finalCmd)
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            sshManager.disconnect()
        }
    }
}

data class DashboardUiState(
    val remoteProfile: RemoteProfile? = null,
    val sshProfile: SshProfile? = null,
    val commands: Map<String, RemoteCommand> = emptyMap(),
    val connectionStatus: String = "Disconnected",
    val isConnected: Boolean = false,
    val slider1Value: Int = 0,
    val slider2Value: Int = 0
)
