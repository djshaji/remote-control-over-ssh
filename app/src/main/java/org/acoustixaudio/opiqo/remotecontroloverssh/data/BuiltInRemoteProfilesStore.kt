package org.acoustixaudio.opiqo.remotecontroloverssh.data

import android.content.res.AssetManager
import java.io.IOException

interface BuiltInRemoteProfilesStore {
    suspend fun loadProfiles(): List<BuiltInRemoteProfile>
}

class AssetBuiltInRemoteProfilesStore(
    private val assetManager: AssetManager
) : BuiltInRemoteProfilesStore {
    override suspend fun loadProfiles(): List<BuiltInRemoteProfile> {
        return try {
            assetManager.open(BUILT_IN_REMOTE_PROFILES_ASSET).bufferedReader().use { reader ->
                parseBuiltInRemoteProfiles(reader.readText())
            }
        } catch (_: IOException) {
            emptyList()
        }
    }

    private companion object {
        const val BUILT_IN_REMOTE_PROFILES_ASSET = "builtin_remote_profiles.json"
    }
}
