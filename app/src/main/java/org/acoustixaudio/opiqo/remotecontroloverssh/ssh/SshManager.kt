package org.acoustixaudio.opiqo.remotecontroloverssh.ssh

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.transport.verification.HostKeyVerifier
import org.acoustixaudio.opiqo.remotecontroloverssh.data.SshProfile
import java.io.File
import java.io.IOException
import java.security.GeneralSecurityException
import java.security.MessageDigest
import java.security.PublicKey
import java.util.Base64

class SshManager : SshClient {
    private var client: SSHClient? = null

    override suspend fun fetchServerFingerprint(host: String, port: Int): SshResult<String> = withContext(Dispatchers.IO) {
        val sshClient = SSHClient()
        var serverKey: PublicKey? = null
        try {
            sshClient.addHostKeyVerifier(
                object : HostKeyVerifier {
                    override fun verify(hostname: String, port: Int, key: PublicKey): Boolean {
                        serverKey = key
                        return true
                    }

                    override fun findExistingAlgorithms(hostname: String, port: Int): MutableList<String> {
                        return mutableListOf()
                    }
                }
            )
            sshClient.connect(host, port)
            val capturedKey = serverKey ?: return@withContext SshResult.Error("The server did not present a host key.")
            sshClient.closeQuietly()
            SshResult.Success(calculateSha256Fingerprint(capturedKey))
        } catch (e: IOException) {
            sshClient.closeQuietly()
            SshResult.Error(e.message ?: "Unable to fetch the server fingerprint.")
        } catch (e: GeneralSecurityException) {
            sshClient.closeQuietly()
            SshResult.Error(e.message ?: "Unable to calculate the server fingerprint.")
        }
    }

    override suspend fun connect(profile: SshProfile): SshResult<Unit> = withContext(Dispatchers.IO) {
        disconnect()

        val fingerprint = profile.hostKeyFingerprint?.trim().orEmpty()
        if (fingerprint.isBlank()) {
            return@withContext SshResult.Error(
                "Add the server fingerprint to this SSH profile before connecting."
            )
        }
        if (!isSupportedFingerprintFormat(fingerprint)) {
            return@withContext SshResult.Error("Invalid host fingerprint format. Use SHA256:... or MD5:... .")
        }

        val keyPath = profile.privateKeyPath
            ?: return@withContext SshResult.Error("No private key is configured for this SSH profile.")
        val keyFile = File(keyPath)
        if (!keyFile.exists()) {
            return@withContext SshResult.Error("The configured private key file could not be found.")
        }

        val sshClient = SSHClient()
        try {
            sshClient.addHostKeyVerifier(createFingerprintVerifier(fingerprint))
            sshClient.connect(profile.host, profile.port)
            val keyProvider = sshClient.loadKeys(keyFile.absolutePath)
            sshClient.authPublickey(profile.username, keyProvider)
            client = sshClient
            SshResult.Success(Unit)
        } catch (e: IOException) {
            sshClient.closeQuietly()
            val message = e.message.orEmpty()
            if (isHostKeyVerificationFailure(message)) {
                val currentFingerprint = fetchCurrentServerFingerprint(profile.host, profile.port)
                val savedNormalized = normalizeFingerprintForComparison(fingerprint)
                val currentNormalized = currentFingerprint?.let(::normalizeFingerprintForComparison)
                val mismatchDetail = if (currentFingerprint != null) {
                    if (currentNormalized == savedNormalized) {
                        "Saved and fetched fingerprints match, but verification still failed. " +
                            "This can happen with DNS/load-balanced SSH endpoints or host key algorithm differences. " +
                            "Try connecting to a direct server IP and verify the host key on the server."
                    } else {
                        "Server now presents $currentFingerprint."
                    }
                } else {
                    "Could not fetch the current server fingerprint automatically."
                }
                SshResult.Error(
                    "Host key fingerprint mismatch. Saved fingerprint is $fingerprint. $mismatchDetail " +
                        "Update the SSH profile fingerprint to continue."
                )
            } else {
                SshResult.Error(message.ifBlank { "Unable to connect to the SSH server." })
            }
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

    private fun calculateSha256Fingerprint(publicKey: PublicKey): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(publicKey.encoded)
        val encoded = Base64.getEncoder().withoutPadding().encodeToString(digest)
        return "SHA256:$encoded"
    }

    private suspend fun fetchCurrentServerFingerprint(host: String, port: Int): String? {
        return when (val result = fetchServerFingerprint(host, port)) {
            is SshResult.Success -> result.value
            is SshResult.Error -> null
        }
    }

    private fun isHostKeyVerificationFailure(message: String): Boolean {
        val normalized = message.lowercase()
        if (normalized.contains("fingerprint mismatch")) return true
        if (normalized.contains("verify host key")) return true
        return normalized.contains("verify") && normalized.contains("fingerprint")
    }

    private fun normalizeFingerprintForComparison(fingerprint: String): String {
        val trimmed = fingerprint.trim()
        return when {
            trimmed.startsWith("SHA256:", ignoreCase = true) -> {
                val value = trimmed.substringAfter(':').trim().trimEnd('=')
                "SHA256:$value"
            }

            trimmed.startsWith("MD5:", ignoreCase = true) -> {
                val value = trimmed.substringAfter(':').trim().lowercase()
                "MD5:$value"
            }

            else -> trimmed
        }
    }

    private fun isSupportedFingerprintFormat(fingerprint: String): Boolean {
        val normalized = fingerprint.trim()
        return normalized.startsWith("SHA256:", ignoreCase = true) ||
            normalized.startsWith("MD5:", ignoreCase = true)
    }

    private fun createFingerprintVerifier(expectedFingerprint: String): HostKeyVerifier {
        val normalizedExpected = normalizeFingerprintForComparison(expectedFingerprint)
        return object : HostKeyVerifier {
            override fun verify(hostname: String, port: Int, key: PublicKey): Boolean {
                val actual = when {
                    normalizedExpected.startsWith("SHA256:", ignoreCase = true) -> {
                        normalizeFingerprintForComparison(calculateSha256Fingerprint(key))
                    }

                    normalizedExpected.startsWith("MD5:", ignoreCase = true) -> {
                        normalizeFingerprintForComparison(calculateMd5Fingerprint(key))
                    }

                    else -> return false
                }
                return actual == normalizedExpected
            }

            override fun findExistingAlgorithms(hostname: String, port: Int): MutableList<String> {
                return mutableListOf()
            }
        }
    }

    private fun calculateMd5Fingerprint(publicKey: PublicKey): String {
        val digest = MessageDigest.getInstance("MD5").digest(publicKey.encoded)
        val hex = digest.joinToString(":") { byte -> "%02x".format(byte) }
        return "MD5:$hex"
    }
}
