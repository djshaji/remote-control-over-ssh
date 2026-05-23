package org.acoustixaudio.opiqo.remotecontroloverssh.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private val builtInProfilesJson = Json { ignoreUnknownKeys = true }

@Serializable
data class BuiltInRemoteProfilesDocument(
    @SerialName("profiles") val profiles: List<BuiltInRemoteProfile> = emptyList()
)

@Serializable
data class BuiltInRemoteProfile(
    val id: String,
    val name: String,
    val commands: Map<String, String>
)

fun parseBuiltInRemoteProfiles(json: String): List<BuiltInRemoteProfile> {
    return builtInProfilesJson
        .decodeFromString<BuiltInRemoteProfilesDocument>(json)
        .profiles
}
