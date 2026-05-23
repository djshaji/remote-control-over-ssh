package org.acoustixaudio.opiqo.remotecontroloverssh.ui.dashboard

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.acoustixaudio.opiqo.remotecontroloverssh.data.AppRepository
import org.acoustixaudio.opiqo.remotecontroloverssh.data.DashboardData
import org.acoustixaudio.opiqo.remotecontroloverssh.data.RemoteCommand
import org.acoustixaudio.opiqo.remotecontroloverssh.data.RemoteControlConfig
import org.acoustixaudio.opiqo.remotecontroloverssh.data.RemoteProfile
import org.acoustixaudio.opiqo.remotecontroloverssh.data.RemoteProfileEditorData
import org.acoustixaudio.opiqo.remotecontroloverssh.data.SshProfile
import org.acoustixaudio.opiqo.remotecontroloverssh.ssh.SshClient
import org.acoustixaudio.opiqo.remotecontroloverssh.ssh.SshResult
import org.acoustixaudio.opiqo.remotecontroloverssh.testutil.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun sliderChanges_areDebouncedAndExecuteLatestCommand_afterConnecting() = runTest {
        val fakeRepository = FakeAppRepository(
            dashboardData = DashboardData(
                remoteProfile = RemoteProfile(id = 1, name = "Remote", sshProfileId = 2),
                sshProfile = SshProfile(
                    id = 2,
                    alias = "Server",
                    host = "host",
                    port = 22,
                    username = "user",
                    privateKeyPath = "/tmp/key",
                    hostKeyFingerprint = "SHA256:abc"
                ),
                commands = listOf(
                    RemoteCommand(
                        remoteProfileId = 1,
                        buttonIdentifier = RemoteControlConfig.sliderStepIdentifier(RemoteControlConfig.SLIDER_1, 1),
                        commandString = "one"
                    ),
                    RemoteCommand(
                        remoteProfileId = 1,
                        buttonIdentifier = RemoteControlConfig.sliderStepIdentifier(RemoteControlConfig.SLIDER_1, 2),
                        commandString = "two"
                    )
                )
            )
        )
        val fakeSshClient = FakeSshClient()

        val viewModel = DashboardViewModel(
            repository = fakeRepository,
            sshClient = fakeSshClient,
            remoteProfileId = 1
        )

        advanceUntilIdle()
        assertTrue(!viewModel.uiState.value.isConnected)

        viewModel.onConnectClick()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isConnected)

        viewModel.onSlider1Change(1)
        viewModel.onSlider1Change(2)

        advanceTimeBy(99)
        assertEquals(emptyList<String>(), fakeSshClient.executedCommands)

        advanceTimeBy(1)
        advanceUntilIdle()

        assertEquals(listOf("two"), fakeSshClient.executedCommands)
    }

    private class FakeAppRepository(
        private val dashboardData: DashboardData
    ) : AppRepository {
        override fun observeSshProfiles(): Flow<List<SshProfile>> = emptyFlow()

        override suspend fun saveSshProfile(profile: SshProfile): Long = profile.id

        override suspend fun deleteSshProfile(profile: SshProfile) = Unit

        override suspend fun getSshProfile(id: Long): SshProfile? = dashboardData.sshProfile

        override fun observeRemoteProfiles(): Flow<List<RemoteProfile>> = emptyFlow()

        override suspend fun saveRemoteProfile(profile: RemoteProfile, commands: Map<String, String>): Long = profile.id

        override suspend fun deleteRemoteProfile(profile: RemoteProfile) = Unit

        override suspend fun getRemoteProfileEditorData(remoteProfileId: Long): RemoteProfileEditorData? {
            return RemoteProfileEditorData(
                remoteProfile = dashboardData.remoteProfile,
                commands = dashboardData.commands
            )
        }

        override suspend fun getDashboardData(remoteProfileId: Long): DashboardData? = dashboardData
    }

    private class FakeSshClient : SshClient {
        val executedCommands = mutableListOf<String>()

        override suspend fun connect(profile: SshProfile): SshResult<Unit> = SshResult.Success(Unit)

        override suspend fun fetchServerFingerprint(host: String, port: Int): SshResult<String> {
            return SshResult.Success("SHA256:test")
        }

        override suspend fun executeCommand(command: String): SshResult<String> {
            executedCommands += command
            return SshResult.Success("")
        }

        override suspend fun disconnect(): SshResult<Unit> = SshResult.Success(Unit)

        override fun isConnected(): Boolean = true
    }
}
