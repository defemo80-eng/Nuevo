package com.example.data.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.local.AppDatabase
import com.example.data.local.CatalogContentEntity
import com.example.data.local.NotificationEntity
import com.example.data.local.SessionPreferences
import com.example.data.local.SyncLogEntity
import com.example.data.remote.ArchiveOrgApi
import com.example.data.remote.AuthorizedCatalogSeed
import com.example.notifications.DetflixNotifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit

class CatalogSyncEngine(
    private val context: Context,
    private val db: AppDatabase = AppDatabase.getInstance(context),
    private val api: ArchiveOrgApi = ArchiveOrgApi.create(),
    private val prefs: SessionPreferences = SessionPreferences(context)
) {
    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    suspend fun ensureSeeded() = withContext(Dispatchers.IO) {
        val dao = db.detflixDao()
        val seedItems = AuthorizedCatalogSeed.initialCatalog()
        val missingItems = seedItems.filter { dao.getContentById(it.id) == null }
        if (missingItems.isNotEmpty()) {
            dao.upsertCatalogItems(missingItems)
        }
        val seedEpisodes = AuthorizedCatalogSeed.initialEpisodes()
        if (missingItems.isNotEmpty() || dao.getCatalogCount() == 0) {
            dao.upsertEpisodes(seedEpisodes)
        }
        if (dao.getCatalogCount() == seedItems.size && missingItems.size == seedItems.size) {
            dao.insertNotifications(
                listOf(
                    NotificationEntity(
                        id = "welcome_premiere",
                        userId = "GLOBAL",
                        title = "Nuevo estreno destacado: Sintel y Corazón en París",
                        message = "Disfruta películas, series, novelas y documentales listos en el catálogo de DETFLIX.",
                        type = "PREMIERE",
                        contentId = "blender_sintel",
                        isRead = false,
                        createdAt = System.currentTimeMillis()
                    ),
                    NotificationEntity(
                        id = "new_season_flash",
                        userId = "GLOBAL",
                        title = "Nuevas temporadas y capítulos disponibles",
                        message = "Temporada 2 de Flash Gordon y capítulos de Corazón en París listos para ver.",
                        type = "NEW_SEASON",
                        contentId = "novela_corazon_paris",
                        isRead = false,
                        createdAt = System.currentTimeMillis() - 60_000L
                    )
                )
            )
        }
    }

    suspend fun syncFromAuthorizedSources(notifyUser: Boolean = true): Result<Int> = withContext(Dispatchers.IO) {
        ensureSeeded()
        val dao = db.detflixDao()
        val now = System.currentTimeMillis()
        var addedCount = 0
        var updatedCount = 0

        return@withContext try {
            val discoveredItems = mutableListOf<CatalogContentEntity>()

            // 1. Sync optional Admin Remote Catalog JSON (published from DETFLIX ADMIN WEB / Firebase Hosting)
            val remoteUrl = prefs.remoteCatalogUrl.first()
            if (remoteUrl.startsWith("https://")) {
                runCatching {
                    val req = Request.Builder().url(remoteUrl).get().build()
                    httpClient.newCall(req).execute().use { resp ->
                        if (resp.isSuccessful) {
                            val body = resp.body?.string().orEmpty()
                            val arr = if (body.trim().startsWith("[")) JSONArray(body)
                            else JSONObject(body).optJSONArray("contents") ?: JSONArray()
                            for (i in 0 until arr.length()) {
                                val obj = arr.getJSONObject(i)
                                val id = obj.optString("id").ifBlank { continue }
                                val streamUrl = obj.optString("streamUrl").ifBlank { continue }
                                val existing = dao.getContentById(id)
                                if (existing == null) addedCount++ else updatedCount++
                                discoveredItems.add(
                                    CatalogContentEntity(
                                        id = id,
                                        title = obj.optString("title", "Sin título"),
                                        description = obj.optString("description", ""),
                                        posterUrl = obj.optString("posterUrl", ""),
                                        backdropUrl = obj.optString("backdropUrl", obj.optString("posterUrl", "")),
                                        streamUrl = streamUrl,
                                        type = obj.optString("type", "MOVIE"),
                                        categoriesCsv = obj.optString("categories", "Acción"),
                                        year = obj.optInt("year", 2025),
                                        durationMinutes = obj.optInt("durationMinutes", 90),
                                        language = obj.optString("language", "Español"),
                                        availableLanguagesCsv = obj.optString("availableLanguages", "Español,Inglés"),
                                        subtitlesCsv = obj.optString("subtitles", "Español,Inglés"),
                                        ageRating = obj.optString("ageRating", "+12"),
                                        castCsv = obj.optString("cast", "Elenco DETFLIX"),
                                        director = obj.optString("director", "DETFLIX"),
                                        rating = obj.optDouble("rating", 8.2),
                                        popularityScore = obj.optInt("popularityScore", 90),
                                        isFeatured = obj.optBoolean("featured", false),
                                        isNewRelease = true,
                                        isTrending = true,
                                        isNewAddition = true,
                                        isAvailable = obj.optBoolean("available", true),
                                        sourceName = "DETFLIX Admin Feed",
                                        seasonsCount = obj.optInt("seasonsCount", 1),
                                        updatedAt = now
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // 2. Live sync across 6 Internet Archive public-domain collections (movies, series, novelas, docs, cartoons)
            val queries = listOf(
                Triple("collection:feature_films AND mediatype:movies", "Drama,Suspenso,Acción", "MOVIE"),
                Triple("collection:feature_films AND (subject:romance OR subject:drama) AND mediatype:movies", "Novelas,Romance,Drama", "NOVELA"),
                Triple("collection:classic_cartoons AND mediatype:movies", "Animación,Infantil,Comedia", "MOVIE"),
                Triple("collection:SciFi_Horror AND mediatype:movies", "Ciencia ficción,Terror", "MOVIE"),
                Triple("collection:classic_tv AND mediatype:movies", "Comedia,Aventura,Romance", "SERIES"),
                Triple("collection:prelinger AND mediatype:movies", "Documentales,Deportes", "DOCUMENTARY")
            )

            for ((query, defaultCats, contentType) in queries) {
                val searchResp = runCatching { api.searchPublicDomainMedia(query = query, rows = 6) }.getOrNull() ?: continue
                val docs = searchResp.response?.docs.orEmpty()
                for (doc in docs) {
                    if (doc.identifier.isBlank() || doc.title.isNullOrBlank()) continue
                    val id = "ia_${doc.identifier}"
                    val meta = runCatching { api.getMetadata(doc.identifier) }.getOrNull()
                    val mp4File = meta?.files?.firstOrNull {
                        it.name.endsWith(".mp4", ignoreCase = true) && !it.name.contains("_sample", ignoreCase = true)
                    } ?: continue

                    val streamUrl = "https://archive.org/download/${doc.identifier}/${mp4File.name}"
                    val posterUrl = "https://archive.org/services/img/${doc.identifier}"
                    val descText = when (val d = doc.description) {
                        is String -> d.replace(Regex("<[^>]*>"), "").trim()
                        is List<*> -> d.firstOrNull()?.toString()?.replace(Regex("<[^>]*>"), "")?.trim().orEmpty()
                        else -> ""
                    }.ifBlank { "Estreno autorizado sincronizado automáticamente en DETFLIX." }

                    val parsedYear = doc.year?.toString()?.take(4)?.toIntOrNull() ?: 1965
                    val creator = when (val c = doc.creator) {
                        is String -> c
                        is List<*> -> c.firstOrNull()?.toString() ?: "Dominio Público"
                        else -> "Dominio Público"
                    }

                    val existing = dao.getContentById(id)
                    if (existing == null) addedCount++ else updatedCount++

                    discoveredItems.add(
                        CatalogContentEntity(
                            id = id,
                            title = doc.title.take(80),
                            description = descText.take(360),
                            posterUrl = posterUrl,
                            backdropUrl = posterUrl,
                            streamUrl = streamUrl,
                            type = contentType,
                            categoriesCsv = defaultCats,
                            year = parsedYear.coerceIn(1910, 2026),
                            durationMinutes = if (contentType == "SERIES") 25 else 78,
                            language = "Inglés",
                            availableLanguagesCsv = "Inglés,Español (Sub)",
                            subtitlesCsv = "Español,Inglés",
                            ageRating = if (defaultCats.contains("Infantil")) "TP" else "+12",
                            castCsv = creator,
                            director = creator,
                            rating = 7.9,
                            popularityScore = 78 + ((doc.downloads ?: 0).coerceAtMost(20000) / 1000),
                            isFeatured = false,
                            isNewRelease = existing == null,
                            isTrending = (doc.downloads ?: 0) > 4000,
                            isNewAddition = true,
                            isAvailable = true,
                            sourceName = "Internet Archive Live Sync",
                            seasonsCount = 1,
                            updatedAt = now
                        )
                    )
                }
            }

            if (discoveredItems.isNotEmpty()) {
                dao.upsertCatalogItems(discoveredItems)
            }
            prefs.setLastSyncAt(now)
            dao.insertSyncLog(
                SyncLogEntity(
                    id = UUID.randomUUID().toString(),
                    sourceName = "Sincronización Automática DETFLIX",
                    addedCount = addedCount,
                    updatedCount = updatedCount,
                    removedCount = 0,
                    status = "SUCCESS",
                    syncedAt = now
                )
            )
            if (addedCount > 0 && notifyUser) {
                val title = "Nuevos estrenos y actualizaciones en DETFLIX"
                val body = "Se incorporaron $addedCount nuevos títulos y $updatedCount actualizaciones al catálogo."
                dao.insertNotification(
                    NotificationEntity(
                        id = UUID.randomUUID().toString(),
                        userId = "GLOBAL",
                        title = title,
                        message = body,
                        type = "NEW_MOVIE",
                        contentId = discoveredItems.firstOrNull()?.id,
                        isRead = false,
                        createdAt = now
                    )
                )
                DetflixNotifier.postSystemNotification(context, 1002, title, body)
            }
            Result.success(addedCount + updatedCount)
        } catch (e: Exception) {
            dao.insertSyncLog(
                SyncLogEntity(
                    id = UUID.randomUUID().toString(),
                    sourceName = "Sincronización Automática DETFLIX",
                    addedCount = 0,
                    updatedCount = 0,
                    removedCount = 0,
                    status = "OFFLINE_CACHED",
                    syncedAt = now
                )
            )
            Result.failure(e)
        }
    }
}

class CatalogSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val engine = CatalogSyncEngine(applicationContext)
        engine.syncFromAuthorizedSources(notifyUser = true)
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "detflix_periodic_catalog_sync"

        fun schedulePeriodicSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val request = PeriodicWorkRequestBuilder<CatalogSyncWorker>(6, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
