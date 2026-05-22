package org.acoustixaudio.opiqo.remotecontroloverssh.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "remote_profiles",
    foreignKeys = [
        ForeignKey(
            entity = SshProfile::class,
            parentColumns = ["id"],
            childColumns = ["sshProfileId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("sshProfileId")]
)
data class RemoteProfile(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val sshProfileId: Long?
)
