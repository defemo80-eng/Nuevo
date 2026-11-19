package com.detflix.admin.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.detflix.admin.data.AdminClientEntity
import com.detflix.admin.data.AdminCodeEntity
import com.detflix.admin.data.AdminContentEntity
import com.detflix.admin.data.AdminDatabase
import com.detflix.admin.data.AdminEpisodeEntity
import com.detflix.admin.data.AdminLogEntity
import com.detflix.admin.data.AdminPaymentEntity
import com.detflix.admin.data.AdminPreferences
import com.detflix.admin.data.AdminRepository
import com.detflix.admin.data.AdminSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AdminTab(val label: String) {
    DASHBOARD("Inicio"),
    CODES("Códigos"),
    CLIENTS("Clientes"),
    PAYMENTS("Pagos"),
    CATALOG("Catálogo")
}

data class AdminDashboardMetrics(
    val totalRevenueUsd: Double = 0.0,
    val activeClients: Int = 0,
    val expiringSoonClients: Int = 0,
    val availableCodes: Int = 0,
    val assignedCodes: Int = 0,
    val publishedContents: Int = 0
)

data class AdminUiState(
    val codes: List<AdminCodeEntity> = emptyList(),
    val clients: List<AdminClientEntity> = emptyList(),
    val payments: List<AdminPaymentEntity> = emptyList(),
    val contents: List<AdminContentEntity> = emptyList(),
    val episodes: List<AdminEpisodeEntity> = emptyList(),
    val logs: List<AdminLogEntity> = emptyList(),
    val settings: AdminSettings = AdminSettings(),
    val metrics: AdminDashboardMetrics = AdminDashboardMetrics(),
    val latestGeneratedCode: AdminCodeEntity? = null,
    val snackbarMessage: String? = null,
    val isUnlocked: Boolean = false,
    val pinError: String? = null
)

class AdminViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AdminDatabase.getInstance(application)
    private val prefs = AdminPreferences(application)
    private val repository = AdminRepository(db.adminDao(), prefs)

    private val latestCode = MutableStateFlow<AdminCodeEntity?>(null)
    private val snackbar = MutableStateFlow<String?>(null)
    private val pinUnlocked = MutableStateFlow(false)
    private val pinError = MutableStateFlow<String?>(null)

    private val baseDataFlow = combine(
        repository.codesFlow,
        repository.clientsFlow,
        repository.paymentsFlow,
        repository.contentsFlow,
        repository.logsFlow
    ) { codes, clients, payments, contents, logs ->
        BaseDataBundle(codes, clients, payments, contents, logs)
    }

    val uiState: StateFlow<AdminUiState> = combine(
        baseDataFlow,
        repository.episodesFlow,
        repository.settingsFlow,
        latestCode,
        combine(snackbar, pinUnlocked, pinError) { msg, unlocked, pError -> Triple(msg, unlocked, pError) }
    ) { bundle, episodes, settings, latest, uiFlags ->
        val (toastMsg, unlocked, pError) = uiFlags
        val now = System.currentTimeMillis()
        val fiveDaysMs = 5 * 86_400_000L
        val activeClients = bundle.clients.count { it.status == "ACTIVE" && it.expiresAtMillis > now }
        val expiringSoon = bundle.clients.count {
            it.status == "ACTIVE" && it.expiresAtMillis > now && (it.expiresAtMillis - now) <= fiveDaysMs
        }
        val metrics = AdminDashboardMetrics(
            totalRevenueUsd = bundle.payments.sumOf { it.amountUsd },
            activeClients = activeClients,
            expiringSoonClients = expiringSoon,
            availableCodes = bundle.codes.count { it.status == "AVAILABLE" },
            assignedCodes = bundle.codes.count { it.status == "ASSIGNED" },
            publishedContents = bundle.contents.count { it.available }
        )
        AdminUiState(
            codes = bundle.codes,
            clients = bundle.clients,
            payments = bundle.payments,
            contents = bundle.contents,
            episodes = episodes,
            logs = bundle.logs,
            settings = settings,
            metrics = metrics,
            latestGeneratedCode = latest,
            snackbarMessage = toastMsg,
            isUnlocked = !settings.requirePin || unlocked,
            pinError = pError
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AdminUiState()
    )

    init {
        viewModelScope.launch {
            repository.ensureSeeded()
        }
    }

    fun verifyPin(pin: String) {
        val expected = uiState.value.settings.adminPin
        if (pin.trim() == expected) {
            pinUnlocked.value = true
            pinError.value = null
        } else {
            pinError.value = "PIN incorrecto"
        }
    }

    fun generateQuickCode(clientName: String? = null, recordPayment: Boolean = false, method: String = "Transferencia") {
        viewModelScope.launch {
            val code = repository.generateCode(
                clientName = clientName,
                recordPayment = recordPayment,
                paymentMethod = method
            )
            latestCode.value = code
            snackbar.value = if (clientName.isNullOrBlank()) {
                "Código generado: ${code.code}"
            } else {
                "Código ${code.code} asignado a ${clientName.trim()}"
            }
        }
    }

    fun assignCodeToClient(codeEntity: AdminCodeEntity, clientName: String) {
        viewModelScope.launch {
            repository.assignExistingCode(codeEntity, clientName)
            snackbar.value = "Código ${codeEntity.code} asignado a ${clientName.trim()}"
        }
    }

    fun revokeCode(codeEntity: AdminCodeEntity) {
        viewModelScope.launch {
            repository.revokeCode(codeEntity)
            snackbar.value = "Código ${codeEntity.code} revocado"
        }
    }

    fun registerOrRenewClient(
        name: String,
        contact: String,
        days: Int,
        amountUsd: Double,
        method: String
    ) {
        if (name.isBlank()) {
            snackbar.value = "Escribe el nombre del cliente"
            return
        }
        viewModelScope.launch {
            val generated = repository.registerOrRenewClient(name, contact, days, amountUsd, method)
            latestCode.value = generated
            snackbar.value = "Listo: ${name.trim()} (+$days días) • ${generated.code}"
        }
    }

    fun toggleClientStatus(client: AdminClientEntity) {
        viewModelScope.launch {
            repository.toggleClientStatus(client)
            snackbar.value = "Estado actualizado para ${client.name}"
        }
    }

    fun deleteClient(client: AdminClientEntity) {
        viewModelScope.launch {
            repository.deleteClient(client)
            snackbar.value = "Cliente eliminado"
        }
    }

    fun recordPayment(clientName: String, amountUsd: Double, method: String, note: String) {
        if (clientName.isBlank() || amountUsd <= 0.0) {
            snackbar.value = "Ingresa cliente y monto válido"
            return
        }
        viewModelScope.launch {
            repository.addPayment(clientName, amountUsd, method, note)
            snackbar.value = "Pago registrado para ${clientName.trim()}"
        }
    }

    fun saveContent(
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
        if (title.isBlank() || streamUrl.isBlank()) {
            snackbar.value = "Título y URL del video son obligatorios"
            return
        }
        viewModelScope.launch {
            repository.saveContent(id, title, type, categories, year, posterUrl, streamUrl, description, featured)
            snackbar.value = "Contenido guardado: ${title.trim()}"
        }
    }

    fun toggleContentAvailable(content: AdminContentEntity) {
        viewModelScope.launch {
            repository.toggleContentAvailable(content)
        }
    }

    fun deleteContent(content: AdminContentEntity) {
        viewModelScope.launch {
            repository.deleteContent(content)
            snackbar.value = "Contenido eliminado"
        }
    }

    fun saveEpisode(
        seriesId: String,
        seasonNumber: Int,
        episodeNumber: Int,
        title: String,
        description: String,
        thumbnailUrl: String,
        streamUrl: String,
        durationMinutes: Int
    ) {
        if (title.isBlank() || streamUrl.isBlank()) {
            snackbar.value = "Título y URL del episodio son obligatorios"
            return
        }
        viewModelScope.launch {
            repository.saveEpisode(
                id = null,
                seriesId = seriesId,
                seasonNumber = seasonNumber,
                episodeNumber = episodeNumber,
                title = title,
                description = description,
                thumbnailUrl = thumbnailUrl,
                streamUrl = streamUrl,
                durationMinutes = durationMinutes
            )
            snackbar.value = "Episodio T${seasonNumber}E${episodeNumber} agregado"
        }
    }

    fun deleteEpisode(episode: AdminEpisodeEntity) {
        viewModelScope.launch {
            repository.deleteEpisode(episode)
            snackbar.value = "Episodio eliminado"
        }
    }

    fun saveSettings(settings: AdminSettings) {
        viewModelScope.launch {
            repository.saveSettings(settings)
            snackbar.value = "Configuración guardada"
        }
    }

    fun exportCatalogJson(onReady: (String) -> Unit) {
        viewModelScope.launch {
            val json = repository.buildCatalogJson()
            onReady(json)
        }
    }

    fun notifyMessage(msg: String) {
        snackbar.value = msg
    }

    fun clearSnackbar() {
        snackbar.value = null
    }

    private data class BaseDataBundle(
        val codes: List<AdminCodeEntity>,
        val clients: List<AdminClientEntity>,
        val payments: List<AdminPaymentEntity>,
        val contents: List<AdminContentEntity>,
        val logs: List<AdminLogEntity>
    )
}
