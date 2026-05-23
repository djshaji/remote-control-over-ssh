package org.acoustixaudio.opiqo.remotecontroloverssh

import android.content.Context
import org.acoustixaudio.opiqo.remotecontroloverssh.data.AppDatabase
import org.acoustixaudio.opiqo.remotecontroloverssh.data.AppRepository
import org.acoustixaudio.opiqo.remotecontroloverssh.data.AssetBuiltInRemoteProfilesStore
import org.acoustixaudio.opiqo.remotecontroloverssh.data.BuiltInRemoteProfilesStore
import org.acoustixaudio.opiqo.remotecontroloverssh.data.RoomAppRepository
import org.acoustixaudio.opiqo.remotecontroloverssh.ssh.SshClient
import org.acoustixaudio.opiqo.remotecontroloverssh.ssh.SshManager

class AppContainer(context: Context) {
    private val database = AppDatabase.getDatabase(context)

    val repository: AppRepository = RoomAppRepository(database)
    val builtInRemoteProfilesStore: BuiltInRemoteProfilesStore =
        AssetBuiltInRemoteProfilesStore(context.assets)

    fun createSshClient(): SshClient = SshManager()
}
