package org.acoustixaudio.opiqo.remotecontroloverssh.ui

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed class NavRoute : NavKey {
    @Serializable
    data object SshProfiles : NavRoute()
    
    @Serializable
    data object RemoteProfiles : NavRoute()
    
    @Serializable
    data class Dashboard(val remoteProfileId: Long) : NavRoute()
}
