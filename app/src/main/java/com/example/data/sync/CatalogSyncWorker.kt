package com.example.data.sync

import android.content.Context
import android.net.Uri
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.local.AppDatabase
import com.example.data.local.CatalogContentEntity
import com.example.data.local.EpisodeEntity
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
    companion object {
        private val ADMIN_CATALOG_URI: Uri =
            Uri.parse("content://com.aistudio.detflix.admin.revocations/catalog")
        private val ADMIN_EPISODES_URI: Uri =
            Uri.parse("content://com.aistudio.detflix.admin.revocations/episodes")
    }

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    suspend fun ensureSeeded() = withContext(Dispatchers.IO) {
        val dao = db.detflixDao()
        val seedItems = AuthorizedCatalogSeed.initialCatalog()
        val hadAnyBefore = dao.getCatalogCount() > 0
        dao.upsertCatalogItems(seedItems)
        dao.upsertEpisodes(AuthorizedCatalogSeed.initialEpisodes())

        if (!hadAnyBefore) {
            dao.insertNotifications(
                listOf(
                    NotificationEntity(
                        id = "welcome_premiere",
                        userId = "GLOBAL",
                        title = "Nuevo estreno destacado: Sintel y Corazón en París",
                        message = "Disfruta películas, series, dramas, novelas y documentales listos en el catálogo de DETFLIX.",
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

    private fun themedPosterFor(categories: String, contentType: String): String {
        val lower = categories.lowercase()
        return when {
            contentType == "NOVELA" || lower.contains("novela") || lower.contains("romance") -> "res://img_poster_novela"
            lower.contains("terror") || lower.contains("suspenso") -> "res://img_poster_horror"
            lower.contains("animación") || lower.contains("animacion") || lower.contains("infantil") || lower.contains("comedia") -> "res://img_poster_animation"
            lower.contains("ciencia") || lower.contains("documental") -> "res://img_poster_scifi"
            else -> "res://img_poster_fantasy"
        }
    }

    private fun readLocalAdminCatalog(): List<CatalogContentEntity> {
        val items = mutableListOf<CatalogContentEntity>()
        val now = System.currentTimeMillis()
        runCatching {
            context.contentResolver.query(ADMIN_CATALOG_URI, null, null, null, null)?.use { c ->
                val idIdx = c.getColumnIndex("id")
                val titleIdx = c.getColumnIndex("title")
                val typeIdx = c.getColumnIndex("type")
                val catsIdx = c.getColumnIndex("categories")
                val yearIdx = c.getColumnIndex("year")
                val posterIdx = c.getColumnIndex("posterUrl")
                val streamIdx = c.getColumnIndex("streamUrl")
                val descIdx = c.getColumnIndex("description")
                val featIdx = c.getColumnIndex("featured")
                val updIdx = c.getColumnIndex("updatedAtMillis")
                if (idIdx >= 0 && titleIdx >= 0 && streamIdx >= 0) {
                    while (c.moveToNext()) {
                        val id = c.getString(idIdx).orEmpty()
                        val streamUrl = c.getString(streamIdx).orEmpty()
                        if (id.isBlank() || streamUrl.isBlank()) continue
                        val cats = c.getString(catsIdx).orEmpty().ifBlank { "Acción,Drama" }
                        val type = c.getString(typeIdx).orEmpty().ifBlank { "MOVIE" }
                        val poster = c.getString(posterIdx).orEmpty().ifBlank { themedPosterFor(cats, type) }
                        items.add(
                            CatalogContentEntity(
                                id = "adm_$id",
                                title = c.getString(titleIdx).orEmpty().ifBlank { "Título DETFLIX" },
                                description = c.getString(descIdx).orEmpty().ifBlank { "Agregado desde DETFLIX Admin." },
                                posterUrl = poster,
                                backdropUrl = poster,
                                streamUrl = streamUrl,
                                type = type,
                                categoriesCsv = cats,
                                year = c.getInt(yearIdx).takeIf { it > 1900 } ?: 2025,
                                durationMinutes = if (type == "SERIES" || type == "NOVELA") 45 else 95,
                                language = "Español",
                                availableLanguagesCsv = "Español,Inglés",
                                subtitlesCsv = "Español,Inglés",
                                ageRating = "+12",
                                castCsv = "Catálogo DETFLIX Admin",
                                director = "DETFLIX Admin",
                                rating = 8.8,
                                popularityScore = 96,
                                isFeatured = c.getInt(featIdx) == 1,
                                isNewRelease = true,
                                isTrending = true,
                                isNewAddition = true,
                                isAvailable = true,
                                sourceName = "DETFLIX Admin APK",
                                seasonsCount = 1,
                                updatedAt = c.getLong(updIdx).takeIf { it > 0L } ?: now
                            )
                        )
                    }
                }
            }
        }
        return items
    }

    private fun readLocalAdminEpisodes(): List<EpisodeEntity> {
        val episodes = mutableListOf<EpisodeEntity>()
        runCatching {
            context.contentResolver.query(ADMIN_EPISODES_URI, null, null, null, null)?.use { c ->
                val idIdx = c.getColumnIndex("id")
                val seriesIdx = c.getColumnIndex("seriesId")
                val seasonIdx = c.getColumnIndex("seasonNumber")
                val epIdx = c.getColumnIndex("episodeNumber")
                val titleIdx = c.getColumnIndex("title")
                val descIdx = c.getColumnIndex("description")
                val thumbIdx = c.getColumnIndex("thumbnailUrl")
                val streamIdx = c.getColumnIndex("streamUrl")
                val durIdx = c.getColumnIndex("durationMinutes")
                if (idIdx >= 0 && seriesIdx >= 0 && streamIdx >= 0) {
                    while (c.moveToNext()) {
                        val id = c.getString(idIdx).orEmpty()
                        val rawSeriesId = c.getString(seriesIdx).orEmpty()
                        val streamUrl = c.getString(streamIdx).orEmpty()
                        if (id.isBlank() || rawSeriesId.isBlank() || streamUrl.isBlank()) continue
                        val thumb = c.getString(thumbIdx).orEmpty().ifBlank { "res://img_poster_fantasy" }
                        episodes.add(
                            EpisodeEntity(
                                id = "adm_ep_$id",
                                seriesId = "adm_$rawSeriesId",
                                seasonNumber = c.getInt(seasonIdx).coerceAtLeast(1),
                                episodeNumber = c.getInt(epIdx).coerceAtLeast(1),
                                title = c.getString(titleIdx).orEmpty().ifBlank { "Episodio" },
                                description = c.getString(descIdx).orEmpty().ifBlank { "Episodio agregado en DETFLIX Admin." },
                                thumbnailUrl = thumb,
                                streamUrl = streamUrl,
                                durationMinutes = c.getInt(durIdx).coerceAtLeast(1)
                            )
                        )
                    }
                }
            }
        }
        return episodes
    }

    suspend fun searchLivePublicCatalog(query: String): Int = withContext(Dispatchers.IO) {
        val clean = query.trim().replace("\"", "")
        if (clean.length < 2) return@withContext 0
        val dao = db.detflixDao()
        val now = System.currentTimeMillis()
        val discovered = mutableListOf<CatalogContentEntity>()

        val archiveQuery = "mediatype:movies AND (title:($clean) OR subject:($clean) OR description:($clean))"
        val searchResp = runCatching {
            api.searchPublicDomainMedia(query = archiveQuery, rows = 14)
        }.getOrNull() ?: return@withContext 0

        for (doc in searchResp.response?.docs.orEmpty()) {
            if (doc.identifier.isBlank() || doc.title.isNullOrBlank()) continue
            val id = "ia_${doc.identifier}"
            if (dao.getContentById(id) != null) continue
            val meta = runCatching { api.getMetadata(doc.identifier) }.getOrNull()
            val mp4File = meta?.files?.firstOrNull {
                it.name.endsWith(".mp4", ignoreCase = true) && !it.name.contains("_sample", ignoreCase = true)
            } ?: continue

            val streamUrl = "https://archive.org/download/${doc.identifier}/${mp4File.name}"
            val rawSubjects = when (val s = doc.subject) {
                is String -> s
                is List<*> -> s.joinToString(",")
                else -> ""
            }.lowercase()

            val contentType = when {
                rawSubjects.contains("series") || rawSubjects.contains("television") -> "SERIES"
                rawSubjects.contains("documentary") || rawSubjects.contains("documental") -> "DOCUMENTARY"
                rawSubjects.contains("soap") || rawSubjects.contains("telenovela") -> "NOVELA"
                else -> "MOVIE"
            }
            val cats = buildList {
                if (rawSubjects.contains("drama")) add("Drama")
                if (rawSubjects.contains("comedy") || rawSubjects.contains("comedia")) add("Comedia")
                if (rawSubjects.contains("horror") || rawSubjects.contains("terror")) add("Terror")
                if (rawSubjects.contains("sci-fi") || rawSubjects.contains("science")) add("Ciencia ficción")
                if (rawSubjects.contains("romance")) add("Romance")
                if (rawSubjects.contains("adventure") || rawSubjects.contains("aventura")) add("Aventura")
                if (rawSubjects.contains("animation") || rawSubjects.contains("cartoon")) add("Animación")
                if (rawSubjects.contains("thriller") || rawSubjects.contains("mystery")) add("Suspenso")
                if (isEmpty()) add("Drama,Acción")
            }.joinToString(",")

            val descText = when (val d = doc.description) {
                is String -> d.replace(Regex("<[^>]*>"), "").trim()
                is List<*> -> d.firstOrNull()?.toString()?.replace(Regex("<[^>]*>"), "")?.trim().orEmpty()
                else -> ""
            }.ifBlank { "Título encontrado en línea en catálogo público DETFLIX." }

            val parsedYear = doc.year?.toString()?.take(4)?.toIntOrNull() ?: 1980
            val creator = when (val c = doc.creator) {
                is String -> c
                is List<*> -> c.firstOrNull()?.toString() ?: "Archivo Público"
                else -> "Archivo Público"
            }

            val poster = themedPosterFor(cats, contentType)
            discovered.add(
                CatalogContentEntity(
                    id = id,
                    title = doc.title.take(80),
                    description = descText.take(360),
                    posterUrl = poster,
                    backdropUrl = poster,
                    streamUrl = streamUrl,
                    type = contentType,
                    categoriesCsv = cats,
                    year = parsedYear.coerceIn(1910, 2026),
                    durationMinutes = if (contentType == "SERIES") 30 else 85,
                    language = "Inglés",
                    availableLanguagesCsv = "Inglés,Español (Sub)",
                    subtitlesCsv = "Español,Inglés",
                    ageRating = "+12",
                    castCsv = creator,
                    director = creator,
                    rating = 7.9,
                    popularityScore = 76 + ((doc.downloads ?: 0).coerceAtMost(15000) / 1000),
                    isFeatured = false,
                    isNewRelease = false,
                    isTrending = false,
                    isNewAddition = true,
                    isAvailable = true,
                    sourceName = "Búsqueda en línea (Dominio Público)",
                    seasonsCount = 1,
                    updatedAt = now
                )
            )
            if (discovered.size >= 8) break
        }

        if (discovered.isNotEmpty()) {
            dao.upsertCatalogItems(discovered)
        }
        discovered.size
    }

    suspend fun syncFromAuthorizedSources(notifyUser: Boolean = true): Result<Int> = withContext(Dispatchers.IO) {
        ensureSeeded()
        val dao = db.detflixDao()
        val now = System.currentTimeMillis()
        var addedCount = 0
        var updatedCount = 0

        return@withContext try {
            val discoveredItems = mutableListOf<CatalogContentEntity>()
            val discoveredEpisodes = mutableListOf<EpisodeEntity>()

            // 0. Same-device DETFLIX Admin APK catalog + episodes bridge
            val localAdminEpisodes = readLocalAdminEpisodes()
            discoveredEpisodes.addAll(localAdminEpisodes)
            val seasonsByLocalSeries = localAdminEpisodes.groupBy { it.seriesId }
                .mapValues { (_, list) -> list.maxOfOrNull { it.seasonNumber } ?: 1 }

            val localAdminItems = readLocalAdminCatalog()
            for (item in localAdminItems) {
                if (dao.getContentById(item.id) == null) addedCount++ else updatedCount++
                val maxSeasons = seasonsByLocalSeries[item.id] ?: item.seasonsCount
                discoveredItems.add(item.copy(seasonsCount = maxSeasons))
            }

            // 1. Sync optional Admin Remote Catalog JSON (published from DETFLIX ADMIN WEB / Firebase Hosting)
            val remoteUrl = prefs.remoteCatalogUrl.first()
            if (remoteUrl.startsWith("https://")) {
                runCatching {
                    val req = Request.Builder().url(remoteUrl).get().build()
                    httpClient.newCall(req).execute().use { resp ->
                        if (resp.isSuccessful) {
                            val body = resp.body?.string().orEmpty()
                            val rootObj = if (!body.trim().startsWith("[")) runCatching { JSONObject(body) }.getOrNull() else null
                            val arr = if (body.trim().startsWith("[")) JSONArray(body)
                            else rootObj?.optJSONArray("contents") ?: JSONArray()
                            for (i in 0 until arr.length()) {
                                val obj = arr.getJSONObject(i)
                                val id = obj.optString("id").ifBlank { continue }
                                val streamUrl = obj.optString("streamUrl").ifBlank { continue }
                                val existing = dao.getContentById(id)
                                if (existing == null) addedCount++ else updatedCount++
                                val cats = obj.optString("categories", "Acción")
                                val type = obj.optString("type", "MOVIE")
                                val rawPoster = obj.optString("posterUrl", "").ifBlank { themedPosterFor(cats, type) }
                                val rawBackdrop = obj.optString("backdropUrl", rawPoster).ifBlank { rawPoster }
                                val epsArr = obj.optJSONArray("episodes")
                                var maxEpSeason = obj.optInt("seasonsCount", 1).coerceAtLeast(1)
                                if (epsArr != null) {
                                    for (j in 0 until epsArr.length()) {
                                        val epObj = epsArr.optJSONObject(j) ?: continue
                                        val epId = epObj.optString("id").ifBlank { "${id}_ep_$j" }
                                        val epStream = epObj.optString("streamUrl").ifBlank { continue }
                                        val sNum = epObj.optInt("seasonNumber", 1).coerceAtLeast(1)
                                        if (sNum > maxEpSeason) maxEpSeason = sNum
                                        discoveredEpisodes.add(
                                            EpisodeEntity(
                                                id = epId,
                                                seriesId = id,
                                                seasonNumber = sNum,
                                                episodeNumber = epObj.optInt("episodeNumber", j + 1).coerceAtLeast(1),
                                                title = epObj.optString("title", "Episodio ${j + 1}"),
                                                description = epObj.optString("description", ""),
                                                thumbnailUrl = epObj.optString("thumbnailUrl", rawPoster).ifBlank { rawPoster },
                                                streamUrl = epStream,
                                                durationMinutes = epObj.optInt("durationMinutes", 40).coerceAtLeast(1)
                                            )
                                        )
                                    }
                                }
                                discoveredItems.add(
                                    CatalogContentEntity(
                                        id = id,
                                        title = obj.optString("title", "Sin título"),
                                        description = obj.optString("description", ""),
                                        posterUrl = rawPoster,
                                        backdropUrl = rawBackdrop,
                                        streamUrl = streamUrl,
                                        type = type,
                                        categoriesCsv = cats,
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
                                        seasonsCount = maxEpSeason,
                                        updatedAt = now
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // 2. Live sync across 8 Internet Archive public-domain collections (movies, film noir dramas, comedies, series, novelas, sci-fi/horror, cartoons, docs & trailers)
            val queries = listOf(
                Triple("collection:feature_films AND mediatype:movies", "Drama,Suspenso,Acción", "MOVIE"),
                Triple("collection:film_noir AND mediatype:movies", "Drama,Suspenso,Acción", "MOVIE"),
                Triple("collection:comedy_films AND mediatype:movies", "Comedia,Romance,Drama", "MOVIE"),
                Triple("collection:feature_films AND (subject:romance OR subject:drama) AND mediatype:movies", "Novelas,Romance,Drama", "NOVELA"),
                Triple("collection:classic_tv AND mediatype:movies", "Comedia,Aventura,Romance", "SERIES"),
                Triple("collection:SciFi_Horror AND mediatype:movies", "Ciencia ficción,Terror", "MOVIE"),
                Triple("collection:classic_cartoons AND mediatype:movies", "Animación,Infantil,Comedia", "MOVIE"),
                Triple("(collection:prelinger OR collection:movie_trailers) AND mediatype:movies", "Documentales,Deportes,Aventura", "DOCUMENTARY")
            )

            for ((query, defaultCats, contentType) in queries) {
                val searchResp = runCatching { api.searchPublicDomainMedia(query = query, rows = 10) }.getOrNull() ?: continue
                val docs = searchResp.response?.docs.orEmpty()
                for (doc in docs) {
                    if (doc.identifier.isBlank() || doc.title.isNullOrBlank()) continue
                    val id = "ia_${doc.identifier}"
                    val meta = runCatching { api.getMetadata(doc.identifier) }.getOrNull()
                    val mp4File = meta?.files?.firstOrNull {
                        it.name.endsWith(".mp4", ignoreCase = true) && !it.name.contains("_sample", ignoreCase = true)
                    } ?: continue

                    val streamUrl = "https://archive.org/download/${doc.identifier}/${mp4File.name}"
                    val posterUrl = themedPosterFor(defaultCats, contentType)
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
                            durationMinutes = if (contentType == "SERIES") 28 else 82,
                            language = "Inglés",
                            availableLanguagesCsv = "Inglés,Español (Sub)",
                            subtitlesCsv = "Español,Inglés",
                            ageRating = if (defaultCats.contains("Infantil")) "TP" else "+12",
                            castCsv = creator,
                            director = creator,
                            rating = 7.9,
                            popularityScore = 72 + ((doc.downloads ?: 0).coerceAtMost(14000) / 1000),
                            isFeatured = false,
                            isNewRelease = existing == null,
                            isTrending = (doc.downloads ?: 0) > 6000,
                            isNewAddition = true,
                            isAvailable = true,
                            sourceName = "Archivo Clásico DETFLIX",
                            seasonsCount = 1,
                            updatedAt = now
                        )
                    )
                }
            }

            if (discoveredItems.isNotEmpty()) {
                dao.upsertCatalogItems(discoveredItems)
            }
            if (discoveredEpisodes.isNotEmpty()) {
                dao.upsertEpisodes(discoveredEpisodes)
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
