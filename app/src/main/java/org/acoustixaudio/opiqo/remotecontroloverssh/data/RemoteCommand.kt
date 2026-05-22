package org.acoustixaudio.opiqo.remotecontroloverssh.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "remote_commands",
    foreignKeys = [
        ForeignKey(
            entity = RemoteProfile::class,
            parentColumns = ["id"],
            childColumns = ["remoteProfileId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("remoteProfileId")]
)
data class RemoteCommand(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val remoteProfileId: Long,
    val buttonIdentifier: String, // e.g., "SLIDER_1", "DPAD_UP"
    val commandString: String // e.g., "amixer set Master %val%+"
)
