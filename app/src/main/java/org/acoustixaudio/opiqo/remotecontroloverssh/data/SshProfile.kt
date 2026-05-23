package org.acoustixaudio.opiqo.remotecontroloverssh.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ssh_profiles")
data class SshProfile(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val alias: String,
    val host: String,
    val port: Int = 22,
    val username: String,
    val privateKeyPath: String? = null,
    val hostKeyFingerprint: String? = null
)
