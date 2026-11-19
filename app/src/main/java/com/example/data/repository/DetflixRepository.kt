package com.example.data.repository

import android.content.Context
import android.net.Uri
import com.example.data.local.AccessCodeEntity
import com.example.data.local.AppDatabase
import com.example.data.local.CatalogContentEntity
import com.example.data.local.EpisodeEntity
import com.example.data.local.FavoriteEntity
import com.example.data.local.MembershipEntity
import com.example.data.local.NotificationEntity
import com.example.data.local.ProfileEntity
import com.example.data.local.RenewalRecordEntity
import com.example.data.local.SessionPreferences
import com.example.data.local.SyncLogEntity
import com.example.data.local.UserEntity
import com.example.data.local.WatchHistoryEntity
import com.example.data.model.AgeRating
import com.example.data.model.CatalogItem
import com.example.data.model.ContentType
import com.example.data.model.ContinueWatchingEntry
import com.example.data.model.EpisodeItem
import com.example.data.security.DetflixCrypto
import com.example.data.sync.CatalogSyncEngine
import com.example.notifications.DetflixNotifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit

class DetflixRepository(private val context: Context) {
    companion object {
        private val ADMIN_REVOCATIONS_URI: Uri =
            Uri.parse("content://com.aistudio.detflix.admin.revocations/revoked")
    }

    private val db = AppDatabase.getInstance(context)
    private val dao = db.detflixDao()
    val prefs = SessionPreferences(context)
    private val syncEngine = CatalogSyncEngine(context, db = db, prefs = prefs)

    private val _sessionClosedReason = MutableStateFlow<String?>(null)
    val sessionClosedReason: StateFlow<String?> = _sessionClosedReason.asStateFlow()

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    suspend fun initializeApp() = withContext(Dispatchers.IO) {
        syncEngine.ensureSeeded()
        val uid = prefs.currentUserId.first()
        if (uid != null) {
            verifyActiveSessionAndCode(uid)
        }
    }

    fun clearSessionClosedReason() {
        _sessionClosedReason.value = null
    }

    suspend fun syncCatalogNow(): Result<Int> = syncEngine.syncFromAuthorizedSources(notifyUser = true)

    suspend fun searchCatalogOnline(query: String): Int = syncEngine.searchLivePublicCatalog(query)

    fun observeCurrentUserId(): Flow<String?> = prefs.currentUserId
    fun observeCurrentProfileId(): Flow<String?> = prefs.currentProfileId
    fun observeLastSyncAt(): Flow<Long> = prefs.lastSyncAt
    fun observeCloudGatewayUrl(): Flow<String> = prefs.cloudGatewayUrl
    fun observeRemoteCatalogUrl(): Flow<String> = prefs.remoteCatalogUrl
    fun observeSavedAccounts(): Flow<List<UserEntity>> = dao.observeAllUsers()

    fun observeUser(userId: String): Flow<UserEntity?> = dao.observeUser(userId)
    fun observeProfiles(userId: String): Flow<List<ProfileEntity>> = dao.observeProfiles(userId)
    fun observeProfile(profileId: String): Flow<ProfileEntity?> = dao.observeProfile(profileId)
    fun observeRenewals(userId: String): Flow<List<RenewalRecordEntity>> = dao.observeRenewals(userId)
    fun observeNotifications(userId: String): Flow<List<NotificationEntity>> = dao.observeNotifications(userId)
    fun observeUnreadNotificationCount(userId: String): Flow<Int> = dao.observeUnreadNotificationCount(userId)
    fun observeSyncLogs(): Flow<List<SyncLogEntity>> = dao.observeSyncLogs()

    fun observeMembership(userId: String): Flow<MembershipEntity?> =
        dao.observeMembership(userId).map { entity ->
            if (entity == null) null
            else {
                val now = System.currentTimeMillis()
                if (entity.status == "ACTIVE" && entity.expiresAt != null && entity.expiresAt <= now) {
                    entity.copy(status = "EXPIRED")
                } else entity
            }
        }

    fun observeCatalogWithEpisodes(): Flow<List<CatalogItem>> =
        combine(dao.observeAvailableCatalog(), dao.observeAllEpisodes()) { contents, episodes ->
            val episodesBySeries = episodes.groupBy { it.seriesId }
            contents.map { entity ->
                val epList = episodesBySeries[entity.id].orEmpty().map { it.toDomain() }
                entity.toDomain(epList)
            }
        }

    fun observeFavoritesIds(profileId: String): Flow<Set<String>> =
        dao.observeFavorites(profileId).map { list -> list.map { it.contentId }.toSet() }

    fun observeContinueWatching(profileId: String): Flow<List<ContinueWatchingEntry>> =
        combine(
            dao.observeWatchHistory(profileId),
            observeCatalogWithEpisodes()
        ) { history, catalog ->
            val byId = catalog.associateBy { it.id }
            history.mapNotNull { h ->
                val item = byId[h.contentId] ?: return@mapNotNull null
                val ep = if (h.episodeId != null) item.episodes.firstOrNull { it.id == h.episodeId } else null
                ContinueWatchingEntry(
                    item = item,
                    episode = ep,
                    positionMs = h.positionMs,
                    durationMs = h.durationMs,
                    progressPercent = h.progressPercent.coerceIn(0f, 1f),
                    lastWatchedAt = h.lastWatchedAt
                )
            }
        }

    /**
     * Queries the standalone DETFLIX Admin APK (if installed on the same device)
     * for revoked access codes and suspended client names.
     */
    private fun queryLocalAdminRevocations(): Pair<Set<String>, Set<String>> {
        val revokedCodes = mutableSetOf<String>()
        val suspendedClients = mutableSetOf<String>()
        runCatching {
            context.contentResolver.query(ADMIN_REVOCATIONS_URI, null, null, null, null)?.use { cursor ->
                val typeIdx = cursor.getColumnIndex("entry_type")
                val valIdx = cursor.getColumnIndex("entry_value")
                if (typeIdx >= 0 && valIdx >= 0) {
                    while (cursor.moveToNext()) {
                        val entryType = cursor.getString(typeIdx)?.trim().orEmpty()
                        val entryVal = cursor.getString(valIdx)?.trim().orEmpty()
                        if (entryVal.isNotEmpty()) {
                            if (entryType == "CODE") revokedCodes.add(entryVal.uppercase())
                            else if (entryType == "CLIENT") suspendedClients.add(entryVal.lowercase())
                        }
                    }
                }
            }
        }
        return revokedCodes to suspendedClients
    }

    private fun checkRemoteAccessRevoked(
        gatewayUrl: String,
        code: String?,
        userId: String,
        displayName: String
    ): Pair<Boolean, Boolean> {
        // Returns (isCodeRevoked, isUserSuspended)
        if (gatewayUrl.isBlank()) return false to false
        return runCatching {
            val endpoint = gatewayUrl.trimEnd('/') + "/checkAccessStatus"
            val json = JSONObject().apply {
                put("userId", userId)
                put("displayName", displayName)
                if (!code.isNullOrBlank()) put("code", code)
            }
            val request = Request.Builder()
                .url(endpoint)
                .post(json.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()
            httpClient.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) return@runCatching false to false
                val parsed = JSONObject(resp.body?.string().orEmpty())
                val codeRevoked = parsed.optBoolean("revoked", false)
                val userSuspended = parsed.optBoolean("suspended", false)
                codeRevoked to userSuspended
            }
        }.getOrDefault(false to false)
    }

    /**
     * Checks whether the active session's user or access code was revoked/suspended
     * by DETFLIX Admin (locally via ContentProvider/Room or remotely via Firebase).
     * Immediately signs out and clears active session if revoked.
     * Returns true if session remains valid, false if revoked and signed out.
     */
    suspend fun verifyActiveSessionAndCode(userId: String): Boolean = withContext(Dispatchers.IO) {
        val user = dao.getUserById(userId)
        val membership = dao.getMembership(userId)
        val lastCode = membership?.lastCodeUsed?.trim()?.uppercase()
        val localCode = lastCode?.let { dao.getAccessCode(it) }

        val (adminRevokedCodes, adminSuspendedClients) = queryLocalAdminRevocations()
        val gatewayUrl = prefs.cloudGatewayUrl.first()
        val (remoteCodeRevoked, remoteSuspended) = checkRemoteAccessRevoked(
            gatewayUrl = gatewayUrl,
            code = lastCode,
            userId = userId,
            displayName = user?.displayName.orEmpty()
        )

        val clientSuspended = (user == null) ||
            user.isSuspended ||
            (user.displayName.trim().lowercase() in adminSuspendedClients) ||
            remoteSuspended

        val codeRevoked = (membership?.status == "SUSPENDED") ||
            (localCode?.status == "CANCELLED") ||
            (lastCode != null && lastCode in adminRevokedCodes) ||
            remoteCodeRevoked

        if (clientSuspended || codeRevoked) {
            val now = System.currentTimeMillis()
            if (lastCode != null) {
                val baseCode = localCode ?: AccessCodeEntity(
                    code = lastCode,
                    status = "CANCELLED",
                    durationDays = 30,
                    createdAt = now,
                    activatedAt = membership?.activatedAt,
                    expiresAt = membership?.expiresAt,
                    usedByUserId = userId
                )
                dao.upsertAccessCode(baseCode.copy(status = "CANCELLED"))
            }
            if (membership != null && membership.status != "SUSPENDED") {
                dao.upsertMembership(membership.copy(status = "SUSPENDED", updatedAt = now))
            }
            if (clientSuspended && user != null && !user.isSuspended) {
                dao.insertUser(user.copy(isSuspended = true))
            }
            val msg = if (clientSuspended) {
                "Tu acceso fue suspendido por el administrador de DETFLIX."
            } else {
                "El código de acceso ${lastCode ?: ""} fue revocado por el administrador. Tu sesión se cerró."
            }
            dao.insertNotification(
                NotificationEntity(
                    id = "revoked_${userId}_${lastCode ?: "acc"}",
                    userId = userId,
                    title = "Sesión cerrada por administración",
                    message = msg,
                    type = "MEMBERSHIP_EXPIRED",
                    isRead = false,
                    createdAt = now
                )
            )
            _sessionClosedReason.value = msg
            logout()
            return@withContext false
        }
        true
    }

    // --- Authentication (Nombre + Código de acceso) ---
    suspend fun enterWithNameAndCode(
        displayName: String,
        rawCode: String
    ): Result<UserEntity> = withContext(Dispatchers.IO) {
        val cleanName = displayName.trim()
        val code = rawCode.trim().uppercase()
        if (cleanName.length < 2) {
            return@withContext Result.failure(IllegalArgumentException("Ingresa tu nombre (mínimo 2 caracteres)."))
        }
        if (code.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Ingresa el código de acceso entregado por el administrador."))
        }

        val (adminRevokedCodes, adminSuspendedClients) = queryLocalAdminRevocations()
        if (cleanName.lowercase() in adminSuspendedClients) {
            return@withContext Result.failure(IllegalStateException("Tu acceso ha sido suspendido por el administrador."))
        }
        if (code in adminRevokedCodes) {
            dao.upsertAccessCode(
                AccessCodeEntity(
                    code = code,
                    status = "CANCELLED",
                    durationDays = 30,
                    createdAt = System.currentTimeMillis()
                )
            )
            return@withContext Result.failure(IllegalStateException("Este código fue revocado por el administrador y ya no es válido."))
        }

        val existingUser = dao.getUserByDisplayName(cleanName)
        if (existingUser?.isSuspended == true) {
            return@withContext Result.failure(IllegalStateException("Tu acceso ha sido suspendido por el administrador."))
        }

        val gatewayUrl = prefs.cloudGatewayUrl.first()
        if (gatewayUrl.isBlank()) {
            if (!DetflixCrypto.isValidAccessCodeFormatAndSignature(code)) {
                return@withContext Result.failure(
                    IllegalArgumentException("Código inválido o no emitido por DETFLIX ADMIN. Formato requerido: DFX-XXXX-XXXX-XXXX")
                )
            }
            val existingCode = dao.getAccessCode(code)
            if (existingCode != null) {
                return@withContext Result.failure(
                    IllegalStateException(
                        when (existingCode.status) {
                            "USED" -> "Este código ya fue utilizado y no puede reutilizarse."
                            "EXPIRED" -> "Este código ya expiró."
                            "CANCELLED" -> "Este código fue revocado por el administrador."
                            else -> "El código no está disponible."
                        }
                    )
                )
            }
        }

        val now = System.currentTimeMillis()
        val user = existingUser ?: UserEntity(
            uid = "usr_${UUID.randomUUID().toString().take(12)}",
            email = "",
            displayName = cleanName,
            passwordHash = "",
            recoveryQuestion = "",
            recoveryAnswerHash = "",
            isSuspended = false,
            maxProfiles = 5,
            createdAt = now
        )

        if (gatewayUrl.isNotBlank()) {
            val serverResult = redeemViaCloudGateway(gatewayUrl, code, user)
            if (serverResult.isFailure) {
                return@withContext Result.failure(
                    serverResult.exceptionOrNull() ?: IllegalStateException("El servidor rechazó el código.")
                )
            }
            val membership = serverResult.getOrThrow()
            if (existingUser == null) {
                dao.insertUser(user)
            }
            val profiles = dao.getProfilesForUser(user.uid)
            val profile = profiles.firstOrNull() ?: ProfileEntity(
                id = "prof_${UUID.randomUUID().toString().take(10)}",
                userId = user.uid,
                name = cleanName,
                avatarId = "crimson_crown",
                isKids = false,
                preferredGenres = "Acción,Ciencia ficción",
                createdAt = now
            ).also { dao.upsertProfile(it) }

            val codeEntity = AccessCodeEntity(
                code = code,
                status = "USED",
                durationDays = 30,
                createdAt = membership.activatedAt ?: now,
                activatedAt = membership.activatedAt,
                expiresAt = membership.expiresAt,
                usedByUserId = user.uid
            )
            val renewal = RenewalRecordEntity(
                id = UUID.randomUUID().toString(),
                userId = user.uid,
                code = code,
                durationDays = 30,
                activatedAt = membership.activatedAt ?: now,
                expiresAt = membership.expiresAt ?: now,
                source = "FIREBASE_CLOUD_FUNCTION"
            )
            dao.activateMembershipWithCode(user.uid, codeEntity, membership, renewal)
            _sessionClosedReason.value = null
            prefs.setSession(user.uid, profile.id)
            return@withContext Result.success(user)
        }

        if (existingUser == null) {
            dao.insertUser(user)
        }
        val profiles = dao.getProfilesForUser(user.uid)
        val profile = profiles.firstOrNull() ?: ProfileEntity(
            id = "prof_${UUID.randomUUID().toString().take(10)}",
            userId = user.uid,
            name = cleanName,
            avatarId = "crimson_crown",
            isKids = false,
            preferredGenres = "Acción,Ciencia ficción",
            createdAt = now
        ).also { dao.upsertProfile(it) }

        if (existingUser == null) {
            dao.upsertMembership(
                MembershipEntity(
                    userId = user.uid,
                    status = "NONE",
                    planName = "Mensual DETFLIX 30 Días",
                    activatedAt = null,
                    expiresAt = null,
                    lastCodeUsed = null,
                    updatedAt = now
                )
            )
        }

        val redeemResult = redeemAccessCode(user.uid, code)
        if (redeemResult.isFailure) {
            return@withContext Result.failure(
                redeemResult.exceptionOrNull() ?: IllegalStateException("No se pudo activar el código.")
            )
        }

        _sessionClosedReason.value = null
        prefs.setSession(user.uid, profile.id)
        Result.success(user)
    }

    suspend fun enterSavedAccount(uid: String): Result<UserEntity> = withContext(Dispatchers.IO) {
        val user = dao.getUserById(uid)
            ?: return@withContext Result.failure(IllegalArgumentException("Cuenta no encontrada en este dispositivo."))
        val membership = dao.getMembership(uid)
        val lastCode = membership?.lastCodeUsed?.trim()?.uppercase()
        val localCode = lastCode?.let { dao.getAccessCode(it) }
        val (adminRevokedCodes, adminSuspendedClients) = queryLocalAdminRevocations()

        if (user.isSuspended || user.displayName.trim().lowercase() in adminSuspendedClients) {
            return@withContext Result.failure(IllegalStateException("Tu acceso ha sido suspendido por el administrador."))
        }
        if (membership?.status == "SUSPENDED" || localCode?.status == "CANCELLED" || (lastCode != null && lastCode in adminRevokedCodes)) {
            if (lastCode != null) {
                val baseCode = localCode ?: AccessCodeEntity(code = lastCode, status = "CANCELLED", createdAt = System.currentTimeMillis())
                dao.upsertAccessCode(baseCode.copy(status = "CANCELLED"))
            }
            if (membership != null && membership.status != "SUSPENDED") {
                dao.upsertMembership(membership.copy(status = "SUSPENDED", updatedAt = System.currentTimeMillis()))
            }
            return@withContext Result.failure(
                IllegalStateException("Tu código anterior fue revocado. Ingresa tu nombre y un nuevo código de acceso.")
            )
        }

        val profiles = dao.getProfilesForUser(user.uid)
        val firstProfile = profiles.firstOrNull() ?: ProfileEntity(
            id = "prof_${UUID.randomUUID().toString().take(10)}",
            userId = user.uid,
            name = user.displayName,
            avatarId = "crimson_crown",
            isKids = false,
            preferredGenres = "Acción,Ciencia ficción",
            createdAt = System.currentTimeMillis()
        ).also { dao.upsertProfile(it) }

        _sessionClosedReason.value = null
        prefs.setSession(user.uid, firstProfile.id)
        checkAndUpdateMembershipExpiration(user.uid)
        Result.success(user)
    }

    suspend fun logout() {
        prefs.setSession(null, null)
    }

    // --- Profiles ---
    suspend fun selectProfile(profileId: String) {
        prefs.setCurrentProfile(profileId)
    }

    suspend fun saveProfile(
        userId: String,
        profileId: String?,
        name: String,
        avatarId: String,
        isKids: Boolean,
        parentalPin: String?,
        preferredGenres: List<String>
    ): Result<ProfileEntity> = withContext(Dispatchers.IO) {
        val cleanName = name.trim()
        if (cleanName.length < 2) {
            return@withContext Result.failure(IllegalArgumentException("El nombre del perfil debe tener al menos 2 caracteres."))
        }
        val user = dao.getUserById(userId)
        val maxProfiles = user?.maxProfiles ?: 5
        val existingProfiles = dao.getProfilesForUser(userId)
        if (profileId == null && existingProfiles.size >= maxProfiles) {
            return@withContext Result.failure(IllegalStateException("Límite máximo de $maxProfiles perfiles alcanzado."))
        }
        val now = System.currentTimeMillis()
        val entity = ProfileEntity(
            id = profileId ?: "prof_${UUID.randomUUID().toString().take(10)}",
            userId = userId,
            name = cleanName,
            avatarId = avatarId,
            isKids = isKids,
            parentalPin = parentalPin?.takeIf { it.isNotBlank() },
            preferredGenres = preferredGenres.joinToString(","),
            createdAt = existingProfiles.firstOrNull { it.id == profileId }?.createdAt ?: now
        )
        dao.upsertProfile(entity)
        prefs.setCurrentProfile(entity.id)
        Result.success(entity)
    }

    suspend fun deleteProfile(userId: String, profileId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val profiles = dao.getProfilesForUser(userId)
        if (profiles.size <= 1) {
            return@withContext Result.failure(IllegalStateException("Debes conservar al menos un perfil activo."))
        }
        dao.deleteProfile(profileId, userId)
        val current = prefs.currentProfileId.first()
        if (current == profileId) {
            val next = profiles.firstOrNull { it.id != profileId }?.id
            prefs.setCurrentProfile(next)
        }
        Result.success(Unit)
    }

    // --- Membership & 30-day Code Activation ---
    suspend fun checkAndUpdateMembershipExpiration(userId: String) = withContext(Dispatchers.IO) {
        val m = dao.getMembership(userId) ?: return@withContext
        val now = System.currentTimeMillis()
        val exp = m.expiresAt ?: return@withContext
        if (m.status == "ACTIVE" && exp <= now) {
            dao.upsertMembership(m.copy(status = "EXPIRED", updatedAt = now))
            m.lastCodeUsed?.let { code ->
                dao.getAccessCode(code)?.let { c ->
                    dao.upsertAccessCode(c.copy(status = "EXPIRED"))
                }
            }
            dao.insertNotification(
                NotificationEntity(
                    id = "exp_${userId}_${exp}",
                    userId = userId,
                    title = "Tu membresía DETFLIX ha vencido",
                    message = "Tu acceso mensual de 30 días finalizó. Ingresa un nuevo código del administrador para renovar.",
                    type = "MEMBERSHIP_EXPIRED",
                    isRead = false,
                    createdAt = now
                )
            )
            DetflixNotifier.postSystemNotification(
                context,
                2001,
                "Membresía DETFLIX vencida",
                "Renueva con un nuevo código mensual para seguir reproduciendo."
            )
        } else if (m.status == "ACTIVE") {
            val daysLeft = ((exp - now) / (1000L * 60 * 60 * 24)).toInt()
            if (daysLeft in 0..3) {
                val notifId = "exp_soon_${userId}_${exp / 86400000L}"
                dao.insertNotification(
                    NotificationEntity(
                        id = notifId,
                        userId = userId,
                        title = "Tu membresía vence pronto ($daysLeft días)",
                        message = "Solicita tu nuevo código de renovación mensual al administrador de DETFLIX.",
                        type = "MEMBERSHIP_EXPIRING",
                        isRead = false,
                        createdAt = now
                    )
                )
            }
        }
    }

    suspend fun redeemAccessCode(userId: String, rawCode: String): Result<MembershipEntity> =
        withContext(Dispatchers.IO) {
            val code = rawCode.trim().uppercase()
            if (code.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Ingresa un código de membresía."))
            }
            val user = dao.getUserById(userId)
                ?: return@withContext Result.failure(IllegalStateException("Usuario no autenticado."))
            if (user.isSuspended) {
                return@withContext Result.failure(IllegalStateException("Cuenta suspendida por administración."))
            }

            // 1. If a Firebase Cloud Function Gateway URL is configured, validate directly on the server
            val gatewayUrl = prefs.cloudGatewayUrl.first()
            if (gatewayUrl.isNotBlank()) {
                val serverResult = redeemViaCloudGateway(gatewayUrl, code, user)
                if (serverResult.isSuccess) {
                    val membership = serverResult.getOrThrow()
                    val codeEntity = AccessCodeEntity(
                        code = code,
                        status = "USED",
                        durationDays = 30,
                        createdAt = membership.activatedAt ?: System.currentTimeMillis(),
                        activatedAt = membership.activatedAt,
                        expiresAt = membership.expiresAt,
                        usedByUserId = userId
                    )
                    val renewal = RenewalRecordEntity(
                        id = UUID.randomUUID().toString(),
                        userId = userId,
                        code = code,
                        durationDays = 30,
                        activatedAt = membership.activatedAt ?: System.currentTimeMillis(),
                        expiresAt = membership.expiresAt ?: System.currentTimeMillis(),
                        source = "FIREBASE_CLOUD_FUNCTION"
                    )
                    dao.activateMembershipWithCode(userId, codeEntity, membership, renewal)
                    return@withContext Result.success(membership)
                } else {
                    return@withContext serverResult
                }
            }

            // 2. Local revocation & cryptographic verification matching DETFLIX ADMIN
            val (adminRevokedCodes, adminSuspendedClients) = queryLocalAdminRevocations()
            if (user.displayName.trim().lowercase() in adminSuspendedClients) {
                return@withContext Result.failure(IllegalStateException("Tu acceso fue suspendido por administración."))
            }
            if (code in adminRevokedCodes) {
                dao.upsertAccessCode(AccessCodeEntity(code = code, status = "CANCELLED", createdAt = System.currentTimeMillis()))
                return@withContext Result.failure(IllegalStateException("Este código fue revocado por el administrador."))
            }
            if (!DetflixCrypto.isValidAccessCodeFormatAndSignature(code)) {
                return@withContext Result.failure(
                    IllegalArgumentException("Código inválido o no emitido por DETFLIX ADMIN. Formato requerido: DFX-XXXX-XXXX-XXXX")
                )
            }

            // Enforce single-use invariant: reject if already redeemed, expired, or cancelled
            val existingCode = dao.getAccessCode(code)
            if (existingCode != null) {
                return@withContext Result.failure(
                    IllegalStateException(
                        when (existingCode.status) {
                            "USED" -> "Este código ya fue utilizado y no puede reutilizarse."
                            "EXPIRED" -> "Este código ya expiró."
                            "CANCELLED" -> "Este código fue revocado por el administrador."
                            else -> "El código no está disponible."
                        }
                    )
                )
            }

            val now = System.currentTimeMillis()
            val durationDays = 30
            val thirtyDaysMs = durationDays * 24L * 60L * 60L * 1000L
            val currentMembership = dao.getMembership(userId)
            val baseStart = if (currentMembership?.status == "ACTIVE" && (currentMembership.expiresAt ?: 0L) > now) {
                currentMembership.expiresAt!!
            } else {
                now
            }
            val expiresAt = baseStart + thirtyDaysMs

            val codeEntity = AccessCodeEntity(
                code = code,
                status = "USED",
                durationDays = durationDays,
                createdAt = now,
                activatedAt = now,
                expiresAt = expiresAt,
                usedByUserId = userId
            )
            val membership = MembershipEntity(
                userId = userId,
                status = "ACTIVE",
                planName = "Mensual DETFLIX 30 Días",
                activatedAt = now,
                expiresAt = expiresAt,
                lastCodeUsed = code,
                updatedAt = now
            )
            val renewal = RenewalRecordEntity(
                id = UUID.randomUUID().toString(),
                userId = userId,
                code = code,
                durationDays = durationDays,
                activatedAt = now,
                expiresAt = expiresAt,
                source = "DETFLIX_SIGNED_CODE"
            )
            dao.activateMembershipWithCode(userId, codeEntity, membership, renewal)
            dao.insertNotification(
                NotificationEntity(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    title = "Membresía activada por 30 días",
                    message = "Código $code validado correctamente. Disfruta todo el catálogo sin límites.",
                    type = "PREMIERE",
                    isRead = false,
                    createdAt = now
                )
            )
            Result.success(membership)
        }

    private fun redeemViaCloudGateway(
        gatewayUrl: String,
        code: String,
        user: UserEntity
    ): Result<MembershipEntity> {
        return try {
            val endpoint = gatewayUrl.trimEnd('/') + "/redeemAccessCode"
            val json = JSONObject().apply {
                put("code", code)
                put("userId", user.uid)
                put("displayName", user.displayName)
                put("email", user.email)
            }
            val request = Request.Builder()
                .url(endpoint)
                .post(json.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()
            httpClient.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string().orEmpty()
                val parsed = runCatching { JSONObject(bodyStr) }.getOrNull()
                if (!response.isSuccessful || parsed?.optBoolean("success", false) != true) {
                    val msg = parsed?.optString("message")?.takeIf { it.isNotBlank() }
                        ?: "El servidor rechazó el código (${response.code})."
                    return Result.failure(IllegalStateException(msg))
                }
                val now = System.currentTimeMillis()
                val activatedAt = parsed.optLong("activatedAt", now)
                val expiresAt = parsed.optLong("expiresAt", now + 30L * 86400_000L)
                Result.success(
                    MembershipEntity(
                        userId = user.uid,
                        status = "ACTIVE",
                        planName = "Mensual DETFLIX 30 Días",
                        activatedAt = activatedAt,
                        expiresAt = expiresAt,
                        lastCodeUsed = code,
                        updatedAt = now
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(IllegalStateException("Error conectando con el servidor Firebase: ${e.localizedMessage}"))
        }
    }

    suspend fun setCloudGatewayUrl(url: String) = prefs.setCloudGatewayUrl(url)
    suspend fun setRemoteCatalogUrl(url: String) = prefs.setRemoteCatalogUrl(url)

    // --- Watch History, Favorites, Notifications ---
    suspend fun toggleFavorite(userId: String, profileId: String, contentId: String) =
        withContext(Dispatchers.IO) {
            if (dao.isFavorite(profileId, contentId)) {
                dao.removeFavorite(profileId, contentId)
            } else {
                dao.addFavorite(
                    FavoriteEntity(
                        profileId = profileId,
                        userId = userId,
                        contentId = contentId,
                        addedAt = System.currentTimeMillis()
                    )
                )
            }
        }

    suspend fun saveWatchProgress(
        userId: String,
        profileId: String,
        contentId: String,
        episodeId: String?,
        positionMs: Long,
        durationMs: Long
    ) = withContext(Dispatchers.IO) {
        if (durationMs <= 0L) return@withContext
        val progress = (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
        dao.upsertWatchHistory(
            WatchHistoryEntity(
                profileId = profileId,
                userId = userId,
                contentId = contentId,
                episodeId = episodeId,
                positionMs = positionMs,
                durationMs = durationMs,
                progressPercent = progress,
                lastWatchedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun getResumePosition(profileId: String, contentId: String, episodeId: String?): Long =
        withContext(Dispatchers.IO) {
            val entry = dao.getWatchHistoryEntry(profileId, contentId) ?: return@withContext 0L
            if (episodeId != null && entry.episodeId != episodeId) return@withContext 0L
            if (entry.progressPercent >= 0.96f) 0L else entry.positionMs
        }

    suspend fun markAllNotificationsRead(userId: String) = withContext(Dispatchers.IO) {
        dao.markAllNotificationsRead(userId)
    }

    suspend fun savePlayerPreferences(quality: String, autoPlay: Boolean, subtitles: Boolean) {
        prefs.setPlayerPreferences(quality, autoPlay, subtitles)
    }

    // --- Mappers ---
    private fun CatalogContentEntity.toDomain(episodes: List<EpisodeItem>): CatalogItem =
        CatalogItem(
            id = id,
            title = title,
            description = description,
            posterUrl = posterUrl,
            backdropUrl = backdropUrl,
            streamUrl = streamUrl,
            type = runCatching { ContentType.valueOf(type) }.getOrDefault(ContentType.MOVIE),
            categories = categoriesCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() },
            year = year,
            durationMinutes = durationMinutes,
            language = language,
            availableLanguages = availableLanguagesCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() },
            subtitles = subtitlesCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() },
            ageRating = AgeRating.fromCode(ageRating),
            cast = castCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() },
            director = director,
            rating = rating,
            popularityScore = popularityScore,
            isFeatured = isFeatured,
            isNewRelease = isNewRelease,
            isTrending = isTrending,
            isNewAddition = isNewAddition,
            sourceName = sourceName,
            seasonsCount = seasonsCount,
            episodes = episodes,
            subtitleUrl = subtitleUrl
        )

    private fun EpisodeEntity.toDomain(): EpisodeItem =
        EpisodeItem(
            id = id,
            seriesId = seriesId,
            seasonNumber = seasonNumber,
            episodeNumber = episodeNumber,
            title = title,
            description = description,
            thumbnailUrl = thumbnailUrl,
            streamUrl = streamUrl,
            durationMinutes = durationMinutes,
            subtitleUrl = subtitleUrl
        )
}
