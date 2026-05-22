package org.acoustixaudio.opiqo.remotecontroloverssh.ui.profiles

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.acoustixaudio.opiqo.remotecontroloverssh.data.AppDatabase
import org.acoustixaudio.opiqo.remotecontroloverssh.data.RemoteCommand
import org.acoustixaudio.opiqo.remotecontroloverssh.data.RemoteProfile
import org.acoustixaudio.opiqo.remotecontroloverssh.data.SshProfile

class RemoteProfilesViewModel(context: Context) : ViewModel() {
    private val db = AppDatabase.getDatabase(context)
    private val sshDao = db.sshDao()
    private val remoteProfileDao = db.remoteProfileDao()
    private val commandDao = db.remoteCommandDao()

    val remoteProfiles: StateFlow<List<RemoteProfile>> = remoteProfileDao.getAllRemoteProfiles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sshProfiles: StateFlow<List<SshProfile>> = sshDao.getAllProfiles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun saveRemoteProfile(
        name: String,
        sshProfileId: Long?,
        commands: Map<String, String>
    ) {
        viewModelScope.launch {
            val profileId = remoteProfileDao.insertRemoteProfile(RemoteProfile(name = name, sshProfileId = sshProfileId))
            commands.forEach { (btn, cmd) ->
                commandDao.insertCommand(RemoteCommand(remoteProfileId = profileId, buttonIdentifier = btn, commandString = cmd))
            }
        }
    }

    fun deleteRemoteProfile(profile: RemoteProfile) {
        viewModelScope.launch {
            remoteProfileDao.deleteRemoteProfile(profile)
        }
    }
}
