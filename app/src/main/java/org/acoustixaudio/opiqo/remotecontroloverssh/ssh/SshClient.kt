package org.acoustixaudio.opiqo.remotecontroloverssh.ssh

import org.acoustixaudio.opiqo.remotecontroloverssh.data.SshProfile

sealed interface SshResult<out T> {
    data class Success<T>(val value: T) : SshResult<T>
    data class Error(val message: String) : SshResult<Nothing>
}

interface SshClient {
    suspend fun fetchServerFingerprint(host: String, port: Int): SshResult<String>
    suspend fun connect(profile: SshProfile): SshResult<Unit>
    suspend fun executeCommand(command: String): SshResult<String>
    suspend fun disconnect(): SshResult<Unit>
    fun isConnected(): Boolean
}
