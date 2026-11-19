package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.RenewalRecordEntity
import com.example.data.model.MembershipStatus
import com.example.ui.theme.DetflixBlack
import com.example.ui.theme.DetflixBorder
import com.example.ui.theme.DetflixGold
import com.example.ui.theme.DetflixRed
import com.example.ui.theme.DetflixSuccess
import com.example.ui.theme.DetflixSurface
import com.example.ui.theme.DetflixSurfaceElevated
import com.example.ui.theme.DetflixTextSecondary
import com.example.ui.theme.DetflixWarning
import com.example.ui.theme.DetflixWhite
import com.example.ui.viewmodel.MembershipUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MembershipScreen(
    uiState: MembershipUiState,
    redeemBanner: Triple<Boolean, String?, Boolean>,
    renewals: List<RenewalRecordEntity>,
    cloudGatewayUrl: String,
    remoteCatalogUrl: String,
    onRedeemCode: (String) -> Unit,
    onClearMessage: () -> Unit,
    onSaveGatewayUrl: (String) -> Unit,
    onSaveRemoteCatalogUrl: (String) -> Unit
) {
    var codeInput by remember { mutableStateOf("") }
    var gatewayInput by remember(cloudGatewayUrl) { mutableStateOf(cloudGatewayUrl) }
    var catalogFeedInput by remember(remoteCatalogUrl) { mutableStateOf(remoteCatalogUrl) }
    var configSavedNote by remember { mutableStateOf(false) }
    var showCloudConfig by remember { mutableStateOf(false) }

    val (isRedeeming, bannerMsg, isBannerError) = redeemBanner

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DetflixBlack)
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "MI MEMBRESÍA",
                style = MaterialTheme.typography.displayMedium,
                color = DetflixWhite
            )
            Text(
                text = "Acceso mensual de 30 días mediante código único DETFLIX",
                style = MaterialTheme.typography.bodyMedium,
                color = DetflixTextSecondary
            )
        }

        // Status Card
        item {
            val statusColor = when (uiState.status) {
                MembershipStatus.ACTIVE -> DetflixSuccess
                MembershipStatus.EXPIRING_SOON -> DetflixWarning
                MembershipStatus.EXPIRED, MembershipStatus.SUSPENDED, MembershipStatus.NONE -> DetflixRed
            }
            Card(
                colors = CardDefaults.cardColors(containerColor = DetflixSurface),
                border = BorderStroke(1.5.dp, statusColor.copy(alpha = 0.7f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = when (uiState.status) {
                                    MembershipStatus.ACTIVE -> Icons.Filled.CheckCircle
                                    MembershipStatus.EXPIRING_SOON -> Icons.Filled.Warning
                                    else -> Icons.Filled.Lock
                                },
                                contentDescription = null,
                                tint = statusColor,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(uiState.status.label, style = MaterialTheme.typography.titleLarge, color = DetflixWhite)
                                Text(uiState.planName, style = MaterialTheme.typography.labelSmall, color = DetflixGold)
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = statusColor.copy(alpha = 0.18f)
                        ) {
                            Text(
                                text = "${uiState.daysRemaining} días",
                                style = MaterialTheme.typography.labelLarge,
                                color = statusColor,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }

                    LinearProgressIndicator(
                        progress = { (uiState.daysRemaining / 30f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = statusColor,
                        trackColor = DetflixSurfaceElevated
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Activación", style = MaterialTheme.typography.labelSmall, color = DetflixTextSecondary)
                            Text(formatDate(uiState.activatedAt), style = MaterialTheme.typography.bodyMedium, color = DetflixWhite)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Vencimiento", style = MaterialTheme.typography.labelSmall, color = DetflixTextSecondary)
                            Text(formatDate(uiState.expiresAt), style = MaterialTheme.typography.bodyMedium, color = DetflixWhite)
                        }
                    }

                    uiState.lastCodeUsed?.let {
                        Text(
                            text = "Último código activado: $it",
                            style = MaterialTheme.typography.labelSmall,
                            color = DetflixTextSecondary
                        )
                    }
                }
            }
        }

        // Redeem Code Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DetflixSurface),
                border = BorderStroke(1.dp, DetflixBorder),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Key, contentDescription = null, tint = DetflixRed)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (uiState.status == MembershipStatus.ACTIVE) "Renovar con Nuevo Código" else "Activar Membresía (30 Días)",
                            style = MaterialTheme.typography.titleMedium,
                            color = DetflixWhite
                        )
                    }

                    bannerMsg?.let { msg ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isBannerError) DetflixRed.copy(alpha = 0.16f)
                                    else DetflixSuccess.copy(alpha = 0.16f)
                                )
                                .padding(12.dp)
                        ) {
                            Text(
                                text = msg,
                                color = if (isBannerError) DetflixWhite else DetflixSuccess,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    OutlinedTextField(
                        value = codeInput,
                        onValueChange = {
                            codeInput = it.uppercase()
                            if (bannerMsg != null) onClearMessage()
                        },
                        label = { Text("Código mensual (DFX-XXXX-XXXX-XXXX)") },
                        placeholder = { Text("DFX-XXXX-XXXX-XXXX") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DetflixRed,
                            unfocusedBorderColor = DetflixBorder,
                            focusedTextColor = DetflixWhite,
                            unfocusedTextColor = DetflixWhite
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("membership_code_input")
                    )

                    Button(
                        onClick = { onRedeemCode(codeInput) },
                        enabled = !isRedeeming && codeInput.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = DetflixRed),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("membership_redeem_button")
                    ) {
                        if (isRedeeming) {
                            CircularProgressIndicator(Modifier.size(22.dp), color = DetflixWhite, strokeWidth = 2.dp)
                        } else {
                            Text("Validar y Activar 30 Días", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Instructions to Obtain a New Code & Support
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DetflixSurfaceElevated),
                border = BorderStroke(1.dp, DetflixBorder),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.SupportAgent, contentDescription = null, tint = DetflixGold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Cómo obtener o renovar tu código mensual", style = MaterialTheme.typography.titleMedium, color = DetflixWhite)
                    }
                    Text("1. Realiza el pago de la mensualidad mediante el medio autorizado por el propietario de DETFLIX.", style = MaterialTheme.typography.bodyMedium, color = DetflixTextSecondary)
                    Text("2. Envía tu comprobante y tu nombre al soporte multicanal del propietario.", style = MaterialTheme.typography.bodyMedium, color = DetflixTextSecondary)
                    Text("3. El administrador confirmará tu pago en DETFLIX ADMIN WEB y te entregará un código único de un solo uso.", style = MaterialTheme.typography.bodyMedium, color = DetflixTextSecondary)
                    Text("4. Introduce el código arriba para activar 30 días continuos (tu perfil, Mi Lista e historial siempre se conservan).", style = MaterialTheme.typography.bodyMedium, color = DetflixTextSecondary)

                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedButton(
                        onClick = { showCloudConfig = !showCloudConfig },
                        border = BorderStroke(1.dp, DetflixBorder),
                        modifier = Modifier.testTag("membership_cloud_config_toggle")
                    ) {
                        Icon(Icons.Filled.CloudSync, contentDescription = null, tint = DetflixTextSecondary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (showCloudConfig) "Ocultar configuración de servidor" else "Conectar Servidor y Catálogo Remoto",
                            color = DetflixTextSecondary
                        )
                    }

                    if (showCloudConfig) {
                        val cfgFieldColors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DetflixRed,
                            unfocusedBorderColor = DetflixBorder,
                            focusedTextColor = DetflixWhite,
                            unfocusedTextColor = DetflixWhite
                        )
                        OutlinedTextField(
                            value = gatewayInput,
                            onValueChange = { gatewayInput = it; configSavedNote = false },
                            label = { Text("URL base de Cloud Functions (opcional)") },
                            placeholder = { Text("https://us-central1-tu-proyecto.cloudfunctions.net") },
                            singleLine = true,
                            colors = cfgFieldColors,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("membership_gateway_url_input")
                        )
                        OutlinedTextField(
                            value = catalogFeedInput,
                            onValueChange = { catalogFeedInput = it; configSavedNote = false },
                            label = { Text("URL Feed catalog.json del Admin (opcional)") },
                            placeholder = { Text("https://tu-admin-detflix.web.app/catalog.json") },
                            singleLine = true,
                            colors = cfgFieldColors,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("membership_catalog_feed_input")
                        )
                        Button(
                            onClick = {
                                onSaveGatewayUrl(gatewayInput)
                                onSaveRemoteCatalogUrl(catalogFeedInput)
                                configSavedNote = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DetflixSurface),
                            border = BorderStroke(1.dp, DetflixRed),
                            modifier = Modifier.testTag("membership_save_server_button")
                        ) {
                            Text("Guardar Conexión", color = DetflixWhite)
                        }
                        if (configSavedNote) {
                            Text(
                                text = "Enlaces guardados. Usa 'Sincronizar ahora' en Inicio para traer las novedades.",
                                style = MaterialTheme.typography.labelSmall,
                                color = DetflixSuccess
                            )
                        }
                    }
                }
            }
        }

        // Renewal History
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.History, contentDescription = null, tint = DetflixRed)
                Spacer(modifier = Modifier.width(8.dp))
                Text("HISTORIAL DE RENOVACIONES", style = MaterialTheme.typography.headlineLarge, color = DetflixWhite)
            }
        }

        if (renewals.isEmpty()) {
            item {
                Text(
                    text = "Aún no tienes renovaciones registradas.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = DetflixTextSecondary
                )
            }
        } else {
            items(renewals, key = { it.id }) { rec ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = DetflixSurface),
                    border = BorderStroke(1.dp, DetflixBorder),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(rec.code, style = MaterialTheme.typography.titleMedium, color = DetflixWhite)
                            Text("Activado: ${formatDate(rec.activatedAt)}", style = MaterialTheme.typography.labelSmall, color = DetflixTextSecondary)
                            Text("Vence: ${formatDate(rec.expiresAt)}", style = MaterialTheme.typography.labelSmall, color = DetflixTextSecondary)
                        }
                        Surface(shape = RoundedCornerShape(8.dp), color = DetflixSuccess.copy(alpha = 0.16f)) {
                            Text(
                                text = "+${rec.durationDays}d",
                                style = MaterialTheme.typography.labelLarge,
                                color = DetflixSuccess,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatDate(timestamp: Long?): String {
    if (timestamp == null || timestamp <= 0L) return "Sin fecha"
    val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("es", "ES"))
    return sdf.format(Date(timestamp))
}
