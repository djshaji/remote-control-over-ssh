package org.acoustixaudio.opiqo.remotecontroloverssh.ssh

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.transport.verification.PromiscuousVerifier
import net.schmizz.sshj.userauth.keyprovider.KeyProvider
import org.acoustixaudio.opiqo.remotecontroloverssh.data.SshProfile
import java.io.File
import java.io.IOException

class SshManager {
    private var client: SSHClient? = null

    suspend fun connect(profile: SshProfile): Boolean = withContext(Dispatchers.IO) {
        try {
            disconnect()
            client = SSHClient().apply {
                addHostKeyVerifier(PromiscuousVerifier())
                connect(profile.host, profile.port)
                
                if (profile.privateKeyPath != null) {
                    val keyFile = File(profile.privateKeyPath)
                    if (keyFile.exists()) {
                        val kp: KeyProvider = loadKeys(keyFile.absolutePath)
                        authPublickey(profile.username, kp)
                    } else {
                        // Fallback or error
                        return@withContext false
                    }
                } else {
                    // This app brief says private key storage, but maybe password too?
                    // For now, let's assume private key is mandatory or handle password if added later.
                    return@withContext false
                }
            }
            client?.isAuthenticated ?: false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun executeCommand(command: String): String? = withContext(Dispatchers.IO) {
        client?.let { ssh ->
            try {
                if (!ssh.isConnected || !ssh.isAuthenticated) {
                    return@withContext null
                }
                ssh.startSession().use { session ->
                    val cmd = session.exec(command)
                    val result = cmd.inputStream.bufferedReader().readText()
                    cmd.join()
                    result
                }
            } catch (e: IOException) {
                e.printStackTrace()
                null
            }
        }
    }

    suspend fun disconnect() = withContext(Dispatchers.IO) {
        try {
            client?.disconnect()
            client = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    fun isConnected(): Boolean {
        return client?.isConnected == true && client?.isAuthenticated == true
    }
}
