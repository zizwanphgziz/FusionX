package com.fusionx.vpn.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.fusionx.vpn.model.ServerProfile
import kotlinx.coroutines.flow.Flow

@Dao
interface ServerProfileDao {

    @Query("SELECT * FROM server_profiles ORDER BY `group`, name")
    fun getAllProfiles(): Flow<List<ServerProfile>>

    @Query("SELECT * FROM server_profiles WHERE id = :id")
    suspend fun getProfileById(id: Long): ServerProfile?

    @Query("SELECT * FROM server_profiles WHERE isSelected = 1 LIMIT 1")
    suspend fun getSelectedProfile(): ServerProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profile: ServerProfile): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(profiles: List<ServerProfile>)

    @Update
    suspend fun update(profile: ServerProfile)

    @Delete
    suspend fun delete(profile: ServerProfile)

    @Query("DELETE FROM server_profiles WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE server_profiles SET isSelected = 0")
    suspend fun deselectAll()

    @Query("UPDATE server_profiles SET isSelected = 1 WHERE id = :id")
    suspend fun selectProfile(id: Long)

    @Query("UPDATE server_profiles SET latency = :latency WHERE id = :id")
    suspend fun updateLatency(id: Long, latency: Long)

    @Query("SELECT DISTINCT `group` FROM server_profiles ORDER BY `group`")
    fun getAllGroups(): Flow<List<String>>

    @Query("SELECT * FROM server_profiles WHERE `group` = :group ORDER BY name")
    fun getProfilesByGroup(group: String): Flow<List<ServerProfile>>

    @Query("DELETE FROM server_profiles WHERE subscriptionUrl = :url")
    suspend fun deleteBySubscription(url: String)
}
