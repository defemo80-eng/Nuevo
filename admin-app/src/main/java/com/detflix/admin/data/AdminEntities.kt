package com.detflix.admin.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "admin_codes")
data class AdminCodeEntity(
    @PrimaryKey val code: String,
    val status: String, // AVAILABLE, ASSIGNED, REVOKED
    val createdAtMillis: Long,
    val activatedAtMillis: Long? = null,
    val expiresAtMillis: Long? = null,
    val assignedClientName: String? = null,
    val durationDays: Int = 30
)

@Entity(tableName = "admin_clients")
data class AdminClientEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val contact: String = "",
    val status: String = "ACTIVE", // ACTIVE, SUSPENDED
    val planDays: Int = 30,
    val expiresAtMillis: Long,
    val lastCode: String = "",
    val createdAtMillis: Long
)

@Entity(tableName = "admin_payments")
data class AdminPaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val clientName: String,
    val amountUsd: Double,
    val method: String,
    val referenceCode: String = "",
    val note: String = "",
    val paidAtMillis: Long
)

@Entity(tableName = "admin_contents")
data class AdminContentEntity(
    @PrimaryKey val id: String,
    val title: String,
    val type: String, // MOVIE, SERIES, NOVELA, DOCUMENTARY
    val categories: String,
    val year: Int,
    val posterUrl: String,
    val streamUrl: String,
    val description: String,
    val featured: Boolean = false,
    val available: Boolean = true,
    val updatedAtMillis: Long
)

@Entity(
    tableName = "admin_episodes",
    indices = [Index(value = ["seriesId"])]
)
data class AdminEpisodeEntity(
    @PrimaryKey val id: String,
    val seriesId: String,
    val seasonNumber: Int,
    val episodeNumber: Int,
    val title: String,
    val description: String,
    val thumbnailUrl: String,
    val streamUrl: String,
    val durationMinutes: Int
)

@Entity(tableName = "admin_logs")
data class AdminLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val message: String,
    val createdAtMillis: Long
)
