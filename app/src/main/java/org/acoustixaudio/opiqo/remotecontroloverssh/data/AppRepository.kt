package org.acoustixaudio.opiqo.remotecontroloverssh.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow

data class DashboardData(
    val remoteProfile: RemoteProfile,
    val sshProfile: SshProfile?,
    val commands: List<RemoteCommand>
)

data class RemoteProfileEditorData(
    val remoteProfile: RemoteProfile,
    val commands: List<RemoteCommand>
)

interface AppRepository {
    fun observeSshProfiles(): Flow<List<SshProfile>>
    suspend fun saveSshProfile(profile: SshProfile): Long
    suspend fun deleteSshProfile(profile: SshProfile)
    suspend fun getSshProfile(id: Long): SshProfile?

    fun observeRemoteProfiles(): Flow<List<RemoteProfile>>
    suspend fun saveRemoteProfile(profile: RemoteProfile, commands: Map<String, String>): Long
    suspend fun deleteRemoteProfile(profile: RemoteProfile)
    suspend fun getRemoteProfileEditorData(remoteProfileId: Long): RemoteProfileEditorData?

    suspend fun getDashboardData(remoteProfileId: Long): DashboardData?
}

class RoomAppRepository(
    private val database: AppDatabase
) : AppRepository {
    private val sshDao = database.sshDao()
    private val remoteProfileDao = database.remoteProfileDao()
    private val remoteCommandDao = database.remoteCommandDao()

    override fun observeSshProfiles(): Flow<List<SshProfile>> = sshDao.getAllProfiles()

    override suspend fun saveSshProfile(profile: SshProfile): Long = sshDao.insertProfile(profile)

    override suspend fun deleteSshProfile(profile: SshProfile) {
        sshDao.deleteProfile(profile)
    }

    override suspend fun getSshProfile(id: Long): SshProfile? = sshDao.getProfileById(id)

    override fun observeRemoteProfiles(): Flow<List<RemoteProfile>> = remoteProfileDao.getAllRemoteProfiles()

    override suspend fun saveRemoteProfile(
        profile: RemoteProfile,
        commands: Map<String, String>
    ): Long = database.withTransaction {
        val profileId = if (profile.id == 0L) {
            remoteProfileDao.insertRemoteProfile(profile)
        } else {
            remoteProfileDao.updateRemoteProfile(profile)
            profile.id
        }

        remoteCommandDao.deleteCommandsForProfile(profileId)
        commands
            .filterValues { it.isNotBlank() }
            .forEach { (buttonIdentifier, commandString) ->
                remoteCommandDao.insertCommand(
                    RemoteCommand(
                        remoteProfileId = profileId,
                        buttonIdentifier = buttonIdentifier,
                        commandString = commandString
                    )
                )
            }

        profileId
    }

    override suspend fun deleteRemoteProfile(profile: RemoteProfile) {
        remoteProfileDao.deleteRemoteProfile(profile)
    }

    override suspend fun getRemoteProfileEditorData(remoteProfileId: Long): RemoteProfileEditorData? {
        val remoteProfile = remoteProfileDao.getRemoteProfileById(remoteProfileId) ?: return null
        val commands = remoteCommandDao.getCommandsForProfileOnce(remoteProfileId)
        return RemoteProfileEditorData(
            remoteProfile = remoteProfile,
            commands = commands
        )
    }

    override suspend fun getDashboardData(remoteProfileId: Long): DashboardData? {
        val remoteProfile = remoteProfileDao.getRemoteProfileById(remoteProfileId) ?: return null
        val sshProfile = remoteProfile.sshProfileId?.let { sshProfileId ->
            sshDao.getProfileById(sshProfileId)
        }
        val commands = remoteCommandDao.getCommandsForProfileOnce(remoteProfileId)
        return DashboardData(
            remoteProfile = remoteProfile,
            sshProfile = sshProfile,
            commands = commands
        )
    }
}
