package org.acoustixaudio.opiqo.remotecontroloverssh.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RemoteProfileDao {
    @Query("SELECT * FROM remote_profiles")
    fun getAllRemoteProfiles(): Flow<List<RemoteProfile>>

    @Query("SELECT * FROM remote_profiles WHERE id = :id")
    suspend fun getRemoteProfileById(id: Long): RemoteProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRemoteProfile(profile: RemoteProfile): Long

    @Update
    suspend fun updateRemoteProfile(profile: RemoteProfile)

    @Delete
    suspend fun deleteRemoteProfile(profile: RemoteProfile)
}
