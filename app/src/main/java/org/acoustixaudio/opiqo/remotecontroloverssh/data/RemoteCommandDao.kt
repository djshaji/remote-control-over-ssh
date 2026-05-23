package org.acoustixaudio.opiqo.remotecontroloverssh.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RemoteCommandDao {
    @Query("SELECT * FROM remote_commands WHERE remoteProfileId = :remoteProfileId")
    fun getCommandsForProfile(remoteProfileId: Long): Flow<List<RemoteCommand>>

    @Query("SELECT * FROM remote_commands WHERE remoteProfileId = :remoteProfileId")
    suspend fun getCommandsForProfileOnce(remoteProfileId: Long): List<RemoteCommand>

    @Query("SELECT * FROM remote_commands WHERE remoteProfileId = :remoteProfileId AND buttonIdentifier = :buttonIdentifier")
    suspend fun getCommandByButton(remoteProfileId: Long, buttonIdentifier: String): RemoteCommand?

    @Query("DELETE FROM remote_commands WHERE remoteProfileId = :remoteProfileId")
    suspend fun deleteCommandsForProfile(remoteProfileId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCommand(command: RemoteCommand)

    @Update
    suspend fun updateCommand(command: RemoteCommand)

    @Delete
    suspend fun deleteCommand(command: RemoteCommand)
}
