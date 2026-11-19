package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.NotificationEntity
import com.example.data.local.SyncLogEntity
import com.example.ui.components.BadgePill
import com.example.ui.theme.DetflixBlack
import com.example.ui.theme.DetflixBorder
import com.example.ui.theme.DetflixRed
import com.example.ui.theme.DetflixSuccess
import com.example.ui.theme.DetflixSurface
import com.example.ui.theme.DetflixSurfaceElevated
import com.example.ui.theme.DetflixTextSecondary
import com.example.ui.theme.DetflixWhite
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NotificationsScreen(
    notifications: List<NotificationEntity>,
    syncLogs: List<SyncLogEntity>,
    isSyncing: Boolean,
    onBack: () -> Unit,
    onMarkAllRead: () -> Unit,
    onSyncNow: () -> Unit
) {
    BackHandler(onBack = onBack)

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DetflixBlack)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("notifications_back_button")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(id = R.string.cd_back), tint = DetflixWhite)
                }
                Text("NOTIFICACIONES", style = MaterialTheme.typography.displayMedium, color = DetflixWhite)
            }
            TextButton(onClick = onMarkAllRead, modifier = Modifier.testTag("mark_all_read_button")) {
                Icon(Icons.Filled.DoneAll, contentDescription = null, tint = DetflixRed)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Leer todo", color = DetflixWhite)
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 90.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onSyncNow,
                        enabled = !isSyncing,
                        colors = ButtonDefaults.buttonColors(containerColor = DetflixRed),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Sync, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isSyncing) "Sincronizando..." else "Sincronizar Catálogo")
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        OutlinedButton(
                            onClick = { notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                            border = BorderStroke(1.dp, DetflixBorder)
                        ) {
                            Icon(Icons.Filled.NotificationsActive, contentDescription = null, tint = DetflixWhite)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Alertas", color = DetflixWhite)
                        }
                    }
                }
            }

            items(notifications, key = { it.id }) { n ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (n.isRead) DetflixSurface else DetflixSurfaceElevated
                    ),
                    border = BorderStroke(1.dp, if (n.isRead) DetflixBorder else DetflixRed.copy(alpha = 0.65f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BadgePill(text = n.type.replace("_", " "), background = if (n.isRead) DetflixSurface else DetflixRed)
                            Text(formatNotifTime(n.createdAt), style = MaterialTheme.typography.labelSmall, color = DetflixTextSecondary)
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(n.title, style = MaterialTheme.typography.titleMedium, color = DetflixWhite)
                        Text(n.message, style = MaterialTheme.typography.bodyMedium, color = DetflixTextSecondary)
                    }
                }
            }

            if (syncLogs.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("HISTORIAL DE SINCRONIZACIÓN", style = MaterialTheme.typography.headlineLarge, color = DetflixWhite)
                }
                items(syncLogs, key = { it.id }) { log ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DetflixSurface),
                        border = BorderStroke(1.dp, DetflixBorder),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(log.sourceName, style = MaterialTheme.typography.labelLarge, color = DetflixWhite)
                                Text(
                                    "+${log.addedCount} nuevos • ${log.updatedCount} actualizados • ${formatNotifTime(log.syncedAt)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = DetflixTextSecondary
                                )
                            }
                            BadgePill(
                                text = log.status,
                                background = DetflixSuccess.copy(alpha = 0.16f),
                                contentColor = DetflixSuccess
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatNotifTime(ts: Long): String {
    val sdf = SimpleDateFormat("dd MMM HH:mm", Locale("es", "ES"))
    return sdf.format(Date(ts))
}
