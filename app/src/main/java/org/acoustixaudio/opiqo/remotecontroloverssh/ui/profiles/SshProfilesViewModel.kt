package org.acoustixaudio.opiqo.remotecontroloverssh.ui.profiles

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.acoustixaudio.opiqo.remotecontroloverssh.data.AppDatabase
import org.acoustixaudio.opiqo.remotecontroloverssh.data.SshProfile
import org.acoustixaudio.opiqo.remotecontroloverssh.util.KeyFileHelper

class SshProfilesViewModel(context: Context) : ViewModel() {
    private val db = AppDatabase.getDatabase(context)
    private val sshDao = db.sshDao()

    val profiles: StateFlow<List<SshProfile>> = sshDao.getAllProfiles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun saveProfile(
        alias: String,
        host: String,
        port: Int,
        username: String,
        privateKeyUri: Uri?,
        context: Context
    ) {
        viewModelScope.launch {
            var keyPath: String? = null
            privateKeyUri?.let { uri ->
                keyPath = KeyFileHelper.copyKeyToInternal(context, uri, "key_${System.currentTimeMillis()}")
            }
            val profile = SshProfile(
                alias = alias,
                host = host,
                port = port,
                username = username,
                privateKeyPath = keyPath
            )
            sshDao.insertProfile(profile)
        }
    }

    fun deleteProfile(profile: SshProfile) {
        viewModelScope.launch {
            profile.privateKeyPath?.let { KeyFileHelper.deleteKey(it) }
            sshDao.deleteProfile(profile)
        }
    }
}
