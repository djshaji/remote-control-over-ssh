package org.acoustixaudio.opiqo.remotecontroloverssh.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDatabaseInstrumentedTest {
    private val database = Room.inMemoryDatabaseBuilder(
        ApplicationProvider.getApplicationContext(),
        AppDatabase::class.java
    ).build()

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun deletingRemoteProfile_removesItsCommands() = runBlocking {
        val sshProfileId = database.sshDao().insertProfile(
            SshProfile(
                alias = "Server",
                host = "192.168.1.10",
                port = 22,
                username = "tester",
                privateKeyPath = "/tmp/key",
                hostKeyFingerprint = "SHA256:abc"
            )
        )
        val remoteProfile = RemoteProfile(name = "Remote", sshProfileId = sshProfileId)
        val remoteProfileId = database.remoteProfileDao().insertRemoteProfile(remoteProfile)
        database.remoteCommandDao().insertCommand(
            RemoteCommand(
                remoteProfileId = remoteProfileId,
                buttonIdentifier = RemoteControlConfig.DPAD_UP,
                commandString = "up"
            )
        )

        database.remoteProfileDao().deleteRemoteProfile(remoteProfile.copy(id = remoteProfileId))

        assertEquals(emptyList<RemoteCommand>(), database.remoteCommandDao().getCommandsForProfileOnce(remoteProfileId))
    }

    @Test
    fun deletingSshProfile_nullsRemoteProfileReference() = runBlocking {
        val sshProfile = SshProfile(
            alias = "Server",
            host = "192.168.1.10",
            port = 22,
            username = "tester",
            privateKeyPath = "/tmp/key",
            hostKeyFingerprint = "SHA256:abc"
        )
        val sshProfileId = database.sshDao().insertProfile(sshProfile)
        val remoteProfileId = database.remoteProfileDao().insertRemoteProfile(
            RemoteProfile(name = "Remote", sshProfileId = sshProfileId)
        )

        database.sshDao().deleteProfile(sshProfile.copy(id = sshProfileId))

        assertNull(database.remoteProfileDao().getRemoteProfileById(remoteProfileId)?.sshProfileId)
    }
}
