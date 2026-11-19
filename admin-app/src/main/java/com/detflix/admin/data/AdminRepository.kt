package com.detflix.admin.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

class AdminRepository(
    private val dao: AdminDao,
    private val prefs: AdminPreferences
) {
    val codesFlow: Flow<List<AdminCodeEntity>> = dao.observeCodes()
    val clientsFlow: Flow<List<AdminClientEntity>> = dao.observeClients()
    val paymentsFlow: Flow<List<AdminPaymentEntity>> = dao.observePayments()
    val contentsFlow: Flow<List<AdminContentEntity>> = dao.observeContents()
    val episodesFlow: Flow<List<AdminEpisodeEntity>> = dao.observeEpisodes()
    val logsFlow: Flow<List<AdminLogEntity>> = dao.observeLogs()
    val settingsFlow: Flow<AdminSettings> = prefs.settingsFlow

    suspend fun ensureSeeded() {
        if (dao.countContents() == 0) {
            val now = System.currentTimeMillis()
            dao.insertContentsIgnore(
                listOf(
                    AdminContentEntity(
                        id = "blender_sintel",
                        title = "Sintel",
                        type = "MOVIE",
                        categories = "Acción, Aventura, Animación",
                        year = 2010,
                        posterUrl = "",
                        streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4",
                        description = "Producción abierta de Blender Foundation. Una joven guerrera busca a su pequeño dragón Scales.",
                        featured = true,
                        available = true,
                        updatedAtMillis = now
                    ),
                    AdminContentEntity(
                        id = "series_flash_gordon",
                        title = "Flash Gordon: Crónicas del Planeta Mongo",
                        type = "SERIES",
                        categories = "Ciencia ficción, Aventura",
                        year = 1936,
                        posterUrl = "",
                        streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
                        description = "Serie clásica de ciencia ficción y aventura espacial.",
                        featured = true,
                        available = true,
                        updatedAtMillis = now - 1000
                    ),
                    AdminContentEntity(
                        id = "novela_corazon_paris",
                        title = "Corazón en París",
                        type = "NOVELA",
                        categories = "Novelas, Romance, Drama",
                        year = 2025,
                        posterUrl = "",
                        streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
                        description = "Novela de amor, intriga y secretos familiares entre París y Madrid.",
                        featured = true,
                        available = true,
                        updatedAtMillis = now - 2000
                    )
                )
            )
            dao.insertLog(AdminLogEntity(message = "Consola DETFLIX Admin lista. Códigos compatibles con DETFLIX APP.", createdAtMillis = now))
        }
    }

    suspend fun generateCode(
        clientName: String? = null,
        recordPayment: Boolean = false,
        paymentMethod: String = "Transferencia",
        customAmountUsd: Double? = null
    ): AdminCodeEntity {
        val now = System.currentTimeMillis()
        val settings = prefs.settingsFlow.first()
        val duration = settings.defaultDurationDays.coerceAtLeast(1)
        val cleanName = clientName?.trim()?.takeIf { it.isNotEmpty() }
        val code = AdminCrypto.generateSignedCode()
        val expiresAt = if (cleanName != null) now + duration * 86_400_000L else null

        val entity = AdminCodeEntity(
            code = code,
            status = if (cleanName != null) "ASSIGNED" else "AVAILABLE",
            createdAtMillis = now,
            activatedAtMillis = if (cleanName != null) now else null,
            expiresAtMillis = expiresAt,
            assignedClientName = cleanName,
            durationDays = duration
        )
        dao.upsertCode(entity)

        if (cleanName != null) {
            val existingClient = dao.findClientByName(cleanName)
            val newExpiry = maxOf(existingClient?.expiresAtMillis ?: now, now) + duration * 86_400_000L
            dao.upsertClient(
                AdminClientEntity(
                    id = existingClient?.id ?: 0,
                    name = cleanName,
                    contact = existingClient?.contact ?: "",
                    status = "ACTIVE",
                    planDays = duration,
                    expiresAtMillis = newExpiry,
                    lastCode = code,
                    createdAtMillis = existingClient?.createdAtMillis ?: now
                )
            )
            if (recordPayment) {
                val amount = customAmountUsd ?: settings.monthlyPriceUsd
                dao.insertPayment(
                    AdminPaymentEntity(
                        clientName = cleanName,
                        amountUsd = amount,
                        method = paymentMethod,
                        referenceCode = code,
                        note = "Activación / renovación $duration días",
                        paidAtMillis = now
                    )
                )
            }
            dao.insertLog(AdminLogEntity(message = "Código $code asignado a $cleanName ($duration días).", createdAtMillis = now))
        } else {
            dao.insertLog(AdminLogEntity(message = "Código disponible generado: $code ($duration días).", createdAtMillis = now))
        }
        return entity
    }

    suspend fun assignExistingCode(codeEntity: AdminCodeEntity, clientName: String) {
        val cleanName = clientName.trim()
        if (cleanName.isEmpty()) return
        val now = System.currentTimeMillis()
        val expires = now + codeEntity.durationDays * 86_400_000L
        dao.upsertCode(
            codeEntity.copy(
                status = "ASSIGNED",
                activatedAtMillis = now,
                expiresAtMillis = expires,
                assignedClientName = cleanName
            )
        )
        val existing = dao.findClientByName(cleanName)
        val base = maxOf(existing?.expiresAtMillis ?: now, now)
        dao.upsertClient(
            AdminClientEntity(
                id = existing?.id ?: 0,
                name = cleanName,
                contact = existing?.contact ?: "",
                status = "ACTIVE",
                planDays = codeEntity.durationDays,
                expiresAtMillis = base + codeEntity.durationDays * 86_400_000L,
                lastCode = codeEntity.code,
                createdAtMillis = existing?.createdAtMillis ?: now
            )
        )
        dao.insertLog(AdminLogEntity(message = "Código ${codeEntity.code} asignado a $cleanName.", createdAtMillis = now))
    }

    suspend fun revokeCode(codeEntity: AdminCodeEntity) {
        val now = System.currentTimeMillis()
        dao.upsertCode(codeEntity.copy(status = "REVOKED"))
        dao.insertLog(AdminLogEntity(message = "Código ${codeEntity.code} revocado.", createdAtMillis = now))
    }

    suspend fun registerOrRenewClient(
        name: String,
        contact: String,
        days: Int,
        amountUsd: Double,
        method: String
    ): AdminCodeEntity {
        val cleanName = name.trim()
        val now = System.currentTimeMillis()
        val safeDays = days.coerceAtLeast(1)
        val code = AdminCrypto.generateSignedCode()

        val existing = dao.findClientByName(cleanName)
        val baseExpiry = maxOf(existing?.expiresAtMillis ?: now, now)
        val newExpiry = baseExpiry + safeDays * 86_400_000L

        val codeEntity = AdminCodeEntity(
            code = code,
            status = "ASSIGNED",
            createdAtMillis = now,
            activatedAtMillis = now,
            expiresAtMillis = newExpiry,
            assignedClientName = cleanName,
            durationDays = safeDays
        )
        dao.upsertCode(codeEntity)
        dao.upsertClient(
            AdminClientEntity(
                id = existing?.id ?: 0,
                name = cleanName,
                contact = contact.trim().ifBlank { existing?.contact ?: "" },
                status = "ACTIVE",
                planDays = safeDays,
                expiresAtMillis = newExpiry,
                lastCode = code,
                createdAtMillis = existing?.createdAtMillis ?: now
            )
        )
        if (amountUsd > 0.0) {
            dao.insertPayment(
                AdminPaymentEntity(
                    clientName = cleanName,
                    amountUsd = amountUsd,
                    method = method,
                    referenceCode = code,
                    note = if (existing == null) "Alta inicial +$safeDays días" else "Renovación +$safeDays días",
                    paidAtMillis = now
                )
            )
        }
        dao.insertLog(
            AdminLogEntity(
                message = "${if (existing == null) "Nuevo cliente" else "Renovación"}: $cleanName (+$safeDays d, $code).",
                createdAtMillis = now
            )
        )
        return codeEntity
    }

    suspend fun toggleClientStatus(client: AdminClientEntity) {
        val now = System.currentTimeMillis()
        val next = if (client.status == "ACTIVE") "SUSPENDED" else "ACTIVE"
        dao.upsertClient(client.copy(status = next))
        dao.insertLog(
            AdminLogEntity(
                message = "Cliente ${client.name} cambiado a estado $next.",
                createdAtMillis = now
            )
        )
    }

    suspend fun deleteClient(client: AdminClientEntity) {
        dao.deleteClient(client)
        dao.insertLog(
            AdminLogEntity(
                message = "Cliente eliminado: ${client.name}.",
                createdAtMillis = System.currentTimeMillis()
            )
        )
    }

    suspend fun addPayment(clientName: String, amountUsd: Double, method: String, note: String) {
        val now = System.currentTimeMillis()
        dao.insertPayment(
            AdminPaymentEntity(
                clientName = clientName.trim(),
                amountUsd = amountUsd,
                method = method,
                note = note.trim(),
                paidAtMillis = now
            )
        )
        dao.insertLog(
            AdminLogEntity(
                message = "Pago registrado: ${clientName.trim()} ($${String.format(Locale.US, "%.2f", amountUsd)} - $method).",
                createdAtMillis = now
            )
        )
    }

    suspend fun saveContent(
        id: String?,
        title: String,
        type: String,
        categories: String,
        year: Int,
        posterUrl: String,
        streamUrl: String,
        description: String,
        featured: Boolean
    ) {
        val now = System.currentTimeMillis()
        val contentId = id?.takeIf { it.isNotBlank() } ?: "custom_${UUID.randomUUID().toString().take(8)}"
        dao.upsertContent(
            AdminContentEntity(
                id = contentId,
                title = title.trim(),
                type = type,
                categories = categories.trim().ifBlank { "General" },
                year = year,
                posterUrl = posterUrl.trim(),
                streamUrl = streamUrl.trim(),
                description = description.trim(),
                featured = featured,
                available = true,
                updatedAtMillis = now
            )
        )
        dao.insertLog(AdminLogEntity(message = "Contenido guardado: ${title.trim()} ($type).", createdAtMillis = now))
    }

    suspend fun toggleContentAvailable(content: AdminContentEntity) {
        dao.upsertContent(content.copy(available = !content.available, updatedAtMillis = System.currentTimeMillis()))
    }

    suspend fun deleteContent(content: AdminContentEntity) {
        dao.deleteEpisodesForSeries(content.id)
        dao.deleteContent(content)
        dao.insertLog(AdminLogEntity(message = "Contenido eliminado: ${content.title}.", createdAtMillis = System.currentTimeMillis()))
    }

    suspend fun saveEpisode(
        id: String?,
        seriesId: String,
        seasonNumber: Int,
        episodeNumber: Int,
        title: String,
        description: String,
        thumbnailUrl: String,
        streamUrl: String,
        durationMinutes: Int
    ) {
        val now = System.currentTimeMillis()
        val epId = id?.takeIf { it.isNotBlank() } ?: "ep_${UUID.randomUUID().toString().take(8)}"
        dao.upsertEpisode(
            AdminEpisodeEntity(
                id = epId,
                seriesId = seriesId,
                seasonNumber = seasonNumber.coerceAtLeast(1),
                episodeNumber = episodeNumber.coerceAtLeast(1),
                title = title.trim(),
                description = description.trim(),
                thumbnailUrl = thumbnailUrl.trim(),
                streamUrl = streamUrl.trim(),
                durationMinutes = durationMinutes.coerceAtLeast(1)
            )
        )
        dao.insertLog(
            AdminLogEntity(
                message = "Episodio T${seasonNumber.coerceAtLeast(1)}E${episodeNumber.coerceAtLeast(1)} guardado: ${title.trim()}.",
                createdAtMillis = now
            )
        )
    }

    suspend fun deleteEpisode(episode: AdminEpisodeEntity) {
        dao.deleteEpisode(episode)
        dao.insertLog(AdminLogEntity(message = "Episodio eliminado: ${episode.title}.", createdAtMillis = System.currentTimeMillis()))
    }

    suspend fun saveSettings(settings: AdminSettings) {
        prefs.saveSettings(settings)
        dao.insertLog(AdminLogEntity(message = "Configuración actualizada (Plan $${String.format(Locale.US, "%.2f", settings.monthlyPriceUsd)} / ${settings.defaultDurationDays}d).", createdAtMillis = System.currentTimeMillis()))
    }

    suspend fun buildCatalogJson(): String {
        val contents = dao.observeContents().first()
        val episodesBySeries = dao.observeEpisodes().first().groupBy { it.seriesId }
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val array = JSONArray()
        contents.filter { it.available }.forEach { item ->
            val eps = episodesBySeries[item.id].orEmpty()
            val maxSeason = eps.maxOfOrNull { it.seasonNumber } ?: 1
            val epsArray = JSONArray()
            eps.forEach { ep ->
                epsArray.put(
                    JSONObject().apply {
                        put("id", ep.id)
                        put("seasonNumber", ep.seasonNumber)
                        put("episodeNumber", ep.episodeNumber)
                        put("title", ep.title)
                        put("description", ep.description)
                        put("thumbnailUrl", ep.thumbnailUrl)
                        put("streamUrl", ep.streamUrl)
                        put("durationMinutes", ep.durationMinutes)
                    }
                )
            }
            array.put(
                JSONObject().apply {
                    put("id", item.id)
                    put("title", item.title)
                    put("description", item.description)
                    put("posterUrl", item.posterUrl)
                    put("backdropUrl", item.posterUrl)
                    put("streamUrl", item.streamUrl)
                    put("type", item.type)
                    put("categories", item.categories)
                    put("year", item.year)
                    put("seasonsCount", maxSeason)
                    put("featured", item.featured)
                    put("available", item.available)
                    if (eps.isNotEmpty()) {
                        put("episodes", epsArray)
                    }
                }
            )
        }
        return JSONObject().apply {
            put("updatedAt", sdf.format(Date()))
            put("contents", array)
        }.toString(2)
    }
}
