package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val uid: String,
    val email: String = "",
    val displayName: String,
    val passwordHash: String = "",
    val recoveryQuestion: String = "",
    val recoveryAnswerHash: String = "",
    val isSuspended: Boolean = false,
    val maxProfiles: Int = 5,
    val createdAt: Long
)

@Entity(
    tableName = "profiles",
    indices = [Index(value = ["userId"])]
)
data class ProfileEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val name: String,
    val avatarId: String,
    val isKids: Boolean = false,
    val parentalPin: String? = null,
    val preferredGenres: String = "",
    val createdAt: Long
)

@Entity(tableName = "memberships")
data class MembershipEntity(
    @PrimaryKey val userId: String,
    val status: String, // ACTIVE, EXPIRED, SUSPENDED, NONE
    val planName: String = "Mensual DETFLIX 30 Días",
    val activatedAt: Long? = null,
    val expiresAt: Long? = null,
    val lastCodeUsed: String? = null,
    val updatedAt: Long
)

@Entity(tableName = "access_codes")
data class AccessCodeEntity(
    @PrimaryKey val code: String,
    val status: String, // AVAILABLE, USED, EXPIRED, CANCELLED
    val durationDays: Int = 30,
    val createdAt: Long,
    val activatedAt: Long? = null,
    val expiresAt: Long? = null,
    val usedByUserId: String? = null
)

@Entity(
    tableName = "renewals",
    indices = [Index(value = ["userId"])]
)
data class RenewalRecordEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val code: String,
    val durationDays: Int,
    val activatedAt: Long,
    val expiresAt: Long,
    val source: String // LOCAL_SIGNED, FIREBASE_CLOUD_FUNCTION
)

@Entity(tableName = "catalog_content")
data class CatalogContentEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val posterUrl: String,
    val backdropUrl: String,
    val streamUrl: String,
    val type: String, // MOVIE, SERIES, DOCUMENTARY
    val categoriesCsv: String,
    val year: Int,
    val durationMinutes: Int,
    val language: String,
    val availableLanguagesCsv: String,
    val subtitlesCsv: String,
    val ageRating: String,
    val castCsv: String,
    val director: String,
    val rating: Double,
    val popularityScore: Int,
    val isFeatured: Boolean,
    val isNewRelease: Boolean,
    val isTrending: Boolean,
    val isNewAddition: Boolean,
    val isAvailable: Boolean = true,
    val sourceName: String,
    val seasonsCount: Int = 1,
    val subtitleUrl: String? = null,
    val updatedAt: Long
)

@Entity(
    tableName = "episodes",
    indices = [Index(value = ["seriesId"])]
)
data class EpisodeEntity(
    @PrimaryKey val id: String,
    val seriesId: String,
    val seasonNumber: Int,
    val episodeNumber: Int,
    val title: String,
    val description: String,
    val thumbnailUrl: String,
    val streamUrl: String,
    val durationMinutes: Int,
    val subtitleUrl: String? = null
)

@Entity(
    tableName = "watch_history",
    primaryKeys = ["profileId", "contentId"],
    indices = [Index(value = ["userId", "profileId"])]
)
data class WatchHistoryEntity(
    val profileId: String,
    val userId: String,
    val contentId: String,
    val episodeId: String? = null,
    val positionMs: Long,
    val durationMs: Long,
    val progressPercent: Float,
    val lastWatchedAt: Long
)

@Entity(
    tableName = "favorites",
    primaryKeys = ["profileId", "contentId"],
    indices = [Index(value = ["userId", "profileId"])]
)
data class FavoriteEntity(
    val profileId: String,
    val userId: String,
    val contentId: String,
    val addedAt: Long
)

@Entity(
    tableName = "notifications",
    indices = [Index(value = ["userId"])]
)
data class NotificationEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val title: String,
    val message: String,
    val type: String, // NEW_MOVIE, NEW_SEASON, NEW_EPISODE, PREMIERE, MEMBERSHIP_EXPIRING, MEMBERSHIP_EXPIRED
    val contentId: String? = null,
    val isRead: Boolean = false,
    val createdAt: Long
)

@Entity(tableName = "sync_logs")
data class SyncLogEntity(
    @PrimaryKey val id: String,
    val sourceName: String,
    val addedCount: Int,
    val updatedCount: Int,
    val removedCount: Int,
    val status: String,
    val syncedAt: Long
)
