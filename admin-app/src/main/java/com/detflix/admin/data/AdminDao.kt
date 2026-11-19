package com.detflix.admin.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AdminDao {
    @Query("SELECT * FROM admin_codes ORDER BY createdAtMillis DESC")
    fun observeCodes(): Flow<List<AdminCodeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCode(code: AdminCodeEntity)

    @Query("SELECT * FROM admin_clients ORDER BY createdAtMillis DESC")
    fun observeClients(): Flow<List<AdminClientEntity>>

    @Query("SELECT * FROM admin_clients WHERE LOWER(name) = LOWER(:name) LIMIT 1")
    suspend fun findClientByName(name: String): AdminClientEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertClient(client: AdminClientEntity): Long

    @Delete
    suspend fun deleteClient(client: AdminClientEntity)

    @Query("SELECT * FROM admin_payments ORDER BY paidAtMillis DESC")
    fun observePayments(): Flow<List<AdminPaymentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: AdminPaymentEntity)

    @Query("SELECT * FROM admin_contents ORDER BY updatedAtMillis DESC")
    fun observeContents(): Flow<List<AdminContentEntity>>

    @Query("SELECT COUNT(*) FROM admin_contents")
    suspend fun countContents(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertContent(content: AdminContentEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertContentsIgnore(contents: List<AdminContentEntity>)

    @Delete
    suspend fun deleteContent(content: AdminContentEntity)

    @Query("SELECT * FROM admin_episodes ORDER BY seriesId, seasonNumber ASC, episodeNumber ASC")
    fun observeEpisodes(): Flow<List<AdminEpisodeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEpisode(episode: AdminEpisodeEntity)

    @Delete
    suspend fun deleteEpisode(episode: AdminEpisodeEntity)

    @Query("DELETE FROM admin_episodes WHERE seriesId = :seriesId")
    suspend fun deleteEpisodesForSeries(seriesId: String)

    @Query("SELECT * FROM admin_logs ORDER BY createdAtMillis DESC LIMIT 40")
    fun observeLogs(): Flow<List<AdminLogEntity>>

    @Insert
    suspend fun insertLog(log: AdminLogEntity)
}
