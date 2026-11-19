package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface DetflixDao {
    // --- Users ---
    @Query("SELECT * FROM users WHERE LOWER(email) = LOWER(:email) LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE LOWER(displayName) = LOWER(:displayName) LIMIT 1")
    suspend fun getUserByDisplayName(displayName: String): UserEntity?

    @Query("SELECT * FROM users ORDER BY createdAt DESC")
    fun observeAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE uid = :uid LIMIT 1")
    suspend fun getUserById(uid: String): UserEntity?

    @Query("SELECT * FROM users WHERE uid = :uid LIMIT 1")
    fun observeUser(uid: String): Flow<UserEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Query("UPDATE users SET passwordHash = :newHash WHERE uid = :uid")
    suspend fun updatePasswordHash(uid: String, newHash: String)

    // --- Profiles ---
    @Query("SELECT * FROM profiles WHERE userId = :userId ORDER BY createdAt ASC")
    fun observeProfiles(userId: String): Flow<List<ProfileEntity>>

    @Query("SELECT * FROM profiles WHERE userId = :userId ORDER BY createdAt ASC")
    suspend fun getProfilesForUser(userId: String): List<ProfileEntity>

    @Query("SELECT * FROM profiles WHERE id = :profileId LIMIT 1")
    fun observeProfile(profileId: String): Flow<ProfileEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(profile: ProfileEntity)

    @Query("DELETE FROM profiles WHERE id = :profileId AND userId = :userId")
    suspend fun deleteProfile(profileId: String, userId: String)

    // --- Memberships & Access Codes ---
    @Query("SELECT * FROM memberships WHERE userId = :userId LIMIT 1")
    fun observeMembership(userId: String): Flow<MembershipEntity?>

    @Query("SELECT * FROM memberships WHERE userId = :userId LIMIT 1")
    suspend fun getMembership(userId: String): MembershipEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMembership(membership: MembershipEntity)

    @Query("SELECT * FROM access_codes WHERE code = :code LIMIT 1")
    suspend fun getAccessCode(code: String): AccessCodeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAccessCode(code: AccessCodeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRenewalRecord(record: RenewalRecordEntity)

    @Query("SELECT * FROM renewals WHERE userId = :userId ORDER BY activatedAt DESC")
    fun observeRenewals(userId: String): Flow<List<RenewalRecordEntity>>

    @Transaction
    suspend fun activateMembershipWithCode(
        userId: String,
        codeEntity: AccessCodeEntity,
        membership: MembershipEntity,
        renewal: RenewalRecordEntity
    ) {
        upsertAccessCode(codeEntity)
        upsertMembership(membership)
        insertRenewalRecord(renewal)
    }

    // --- Catalog & Episodes ---
    @Query("SELECT * FROM catalog_content WHERE isAvailable = 1 ORDER BY popularityScore DESC")
    fun observeAvailableCatalog(): Flow<List<CatalogContentEntity>>

    @Query("SELECT * FROM catalog_content WHERE id = :id LIMIT 1")
    suspend fun getContentById(id: String): CatalogContentEntity?

    @Query("SELECT COUNT(*) FROM catalog_content")
    suspend fun getCatalogCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCatalogItems(items: List<CatalogContentEntity>)

    @Query("SELECT * FROM episodes WHERE seriesId = :seriesId ORDER BY seasonNumber ASC, episodeNumber ASC")
    fun observeEpisodesForSeries(seriesId: String): Flow<List<EpisodeEntity>>

    @Query("SELECT * FROM episodes ORDER BY seriesId, seasonNumber ASC, episodeNumber ASC")
    fun observeAllEpisodes(): Flow<List<EpisodeEntity>>

    @Query("SELECT * FROM episodes WHERE seriesId = :seriesId ORDER BY seasonNumber ASC, episodeNumber ASC")
    suspend fun getEpisodesForSeries(seriesId: String): List<EpisodeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEpisodes(episodes: List<EpisodeEntity>)

    // --- Watch History ---
    @Query("SELECT * FROM watch_history WHERE profileId = :profileId ORDER BY lastWatchedAt DESC")
    fun observeWatchHistory(profileId: String): Flow<List<WatchHistoryEntity>>

    @Query("SELECT * FROM watch_history WHERE profileId = :profileId AND contentId = :contentId LIMIT 1")
    suspend fun getWatchHistoryEntry(profileId: String, contentId: String): WatchHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWatchHistory(entry: WatchHistoryEntity)

    @Query("DELETE FROM watch_history WHERE profileId = :profileId AND contentId = :contentId")
    suspend fun removeWatchHistory(profileId: String, contentId: String)

    // --- Favorites (Mi Lista) ---
    @Query("SELECT * FROM favorites WHERE profileId = :profileId ORDER BY addedAt DESC")
    fun observeFavorites(profileId: String): Flow<List<FavoriteEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE profileId = :profileId AND contentId = :contentId)")
    suspend fun isFavorite(profileId: String, contentId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE profileId = :profileId AND contentId = :contentId")
    suspend fun removeFavorite(profileId: String, contentId: String)

    // --- Notifications & Sync Logs ---
    @Query("SELECT * FROM notifications WHERE userId = :userId OR userId = 'GLOBAL' ORDER BY createdAt DESC LIMIT 50")
    fun observeNotifications(userId: String): Flow<List<NotificationEntity>>

    @Query("SELECT COUNT(*) FROM notifications WHERE (userId = :userId OR userId = 'GLOBAL') AND isRead = 0")
    fun observeUnreadNotificationCount(userId: String): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotifications(notifications: List<NotificationEntity>)

    @Query("UPDATE notifications SET isRead = 1 WHERE (userId = :userId OR userId = 'GLOBAL')")
    suspend fun markAllNotificationsRead(userId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSyncLog(log: SyncLogEntity)

    @Query("SELECT * FROM sync_logs ORDER BY syncedAt DESC LIMIT 10")
    fun observeSyncLogs(): Flow<List<SyncLogEntity>>
}
