package org.acoustixaudio.opiqo.remotecontroloverssh.ssh

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.transport.verification.FingerprintVerifier
import org.acoustixaudio.opiqo.remotecontroloverssh.data.SshProfile
import java.io.File
import java.io.IOException
import java.security.GeneralSecurityException

class SshManager : SshClient {
    private var client: SSHClient? = null

    override suspend fun connect(profile: SshProfile): SshResult<Unit> = withContext(Dispatchers.IO) {
        disconnect()

        val fingerprint = profile.hostKeyFingerprint?.trim().orEmpty()
        if (fingerprint.isBlank()) {
            return@withContext SshResult.Error(
                "Add the server fingerprint to this SSH profile before connecting."
            )
        }

        val keyPath = profile.privateKeyPath
            ?: return@withContext SshResult.Error("No private key is configured for this SSH profile.")
        val keyFile = File(keyPath)
        if (!keyFile.exists()) {
            return@withContext SshResult.Error("The configured private key file could not be found.")
        }

        val sshClient = SSHClient()
        try {
            sshClient.addHostKeyVerifier(FingerprintVerifier.getInstance(fingerprint))
            sshClient.connect(profile.host, profile.port)
            val keyProvider = sshClient.loadKeys(keyFile.absolutePath)
            sshClient.authPublickey(profile.username, keyProvider)
            client = sshClient
            SshResult.Success(Unit)
        } catch (e: IllegalArgumentException) {
            sshClient.closeQuietly()
            SshResult.Error("Invalid host fingerprint format. Use SHA256:... or MD5:... .")
        } catch (e: IOException) {
            sshClient.closeQuietly()
            SshResult.Error(e.message ?: "Unable to connect to the SSH server.")
        } catch (e: GeneralSecurityException) {
            sshClient.closeQuietly()
            SshResult.Error(e.message ?: "SSH authentication failed.")
        }
    }

    override suspend fun executeCommand(command: String): SshResult<String> = withContext(Dispatchers.IO) {
        val ssh = client ?: return@withContext SshResult.Error("SSH session is not connected.")
        if (command.isBlank()) {
            return@withContext SshResult.Error("Cannot execute an empty command.")
        }
        if (!ssh.isConnected || !ssh.isAuthenticated) {
            return@withContext SshResult.Error("SSH session is not connected.")
        }

        try {
            ssh.startSession().use { session ->
                val remoteCommand = session.exec(command)
                val output = remoteCommand.inputStream.bufferedReader().readText()
                remoteCommand.join()
                SshResult.Success(output)
            }
        } catch (e: IOException) {
            SshResult.Error(e.message ?: "SSH command execution failed.")
        }
    }

    override suspend fun disconnect(): SshResult<Unit> = withContext(Dispatchers.IO) {
        val ssh = client ?: return@withContext SshResult.Success(Unit)
        client = null

        try {
            ssh.disconnect()
            SshResult.Success(Unit)
        } catch (e: IOException) {
            SshResult.Error(e.message ?: "Failed to close the SSH connection.")
        }
    }

    override fun isConnected(): Boolean {
        return client?.isConnected == true && client?.isAuthenticated == true
    }

    private fun SSHClient.closeQuietly() {
        try {
            disconnect()
        } catch (_: IOException) {
        }
    }
}
