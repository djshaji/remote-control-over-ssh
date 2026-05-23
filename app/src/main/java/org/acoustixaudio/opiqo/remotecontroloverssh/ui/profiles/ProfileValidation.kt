package org.acoustixaudio.opiqo.remotecontroloverssh.ui.profiles

private val supportedFingerprintRegex = Regex(
    pattern = "(?i)(SHA256:[A-Za-z0-9+/]+={0,2}|MD5:([0-9a-f]{2}:){15}[0-9a-f]{2})"
)

data class SshProfileValidationErrors(
    val alias: String? = null,
    val host: String? = null,
    val port: String? = null,
    val username: String? = null,
    val privateKey: String? = null,
    val fingerprint: String? = null
) {
    fun hasErrors(): Boolean = listOf(alias, host, port, username, privateKey, fingerprint).any { it != null }
}

fun validateSshProfileInput(
    alias: String,
    host: String,
    port: String,
    username: String,
    hasPrivateKey: Boolean,
    fingerprint: String
): SshProfileValidationErrors {
    val parsedPort = port.toIntOrNull()
    return SshProfileValidationErrors(
        alias = if (alias.isBlank()) "Profile name is required." else null,
        host = if (host.isBlank()) "Host is required." else null,
        port = when {
            port.isBlank() -> "Port is required."
            parsedPort == null -> "Port must be a number."
            parsedPort !in 1..65535 -> "Port must be between 1 and 65535."
            else -> null
        },
        username = if (username.isBlank()) "Username is required." else null,
        privateKey = if (!hasPrivateKey) "Select a private key file." else null,
        fingerprint = when {
            fingerprint.isBlank() -> "Server fingerprint is required."
            !supportedFingerprintRegex.matches(fingerprint.trim()) ->
                "Use SHA256:... or MD5:aa:bb:... fingerprint format."
            else -> null
        }
    )
}

data class RemoteProfileValidationErrors(
    val name: String? = null
) {
    fun hasErrors(): Boolean = name != null
}

fun validateRemoteProfileInput(name: String): RemoteProfileValidationErrors {
    return RemoteProfileValidationErrors(
        name = if (name.isBlank()) "Remote name is required." else null
    )
}
