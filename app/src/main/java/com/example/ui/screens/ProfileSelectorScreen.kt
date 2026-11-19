package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.local.ProfileEntity
import com.example.data.local.UserEntity
import com.example.data.model.DetflixAvatars
import com.example.data.model.DetflixCategory
import com.example.ui.components.AvatarBadge
import com.example.ui.components.BadgePill
import com.example.ui.components.DetflixBrandMark
import com.example.ui.theme.DetflixBlack
import com.example.ui.theme.DetflixBorder
import com.example.ui.theme.DetflixGold
import com.example.ui.theme.DetflixRed
import com.example.ui.theme.DetflixSurface
import com.example.ui.theme.DetflixSurfaceElevated
import com.example.ui.theme.DetflixTextSecondary
import com.example.ui.theme.DetflixWhite

@Composable
fun ProfileSelectorScreen(
    user: UserEntity?,
    profiles: List<ProfileEntity>,
    activeProfileId: String?,
    onSelectProfile: (String) -> Unit,
    onSaveProfile: (String?, String, String, Boolean, String?, List<String>) -> Unit,
    onDeleteProfile: (String) -> Unit,
    onLogout: () -> Unit,
    onContinueToHome: () -> Unit
) {
    var editingProfile by remember { mutableStateOf<ProfileEntity?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var pinPromptProfile by remember { mutableStateOf<ProfileEntity?>(null) }
    var pinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DetflixBlack)
            .statusBarsPadding()
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            DetflixBrandMark(size = 36.dp)
            OutlinedButton(
                onClick = onLogout,
                border = BorderStroke(1.dp, DetflixBorder),
                modifier = Modifier.testTag("profile_logout_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = DetflixTextSecondary, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Salir", color = DetflixTextSecondary)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "¿Quién está viendo ahora?",
            style = MaterialTheme.typography.displayMedium,
            color = DetflixWhite
        )
        Text(
            text = "${user?.displayName?.ifBlank { "Cuenta DETFLIX" } ?: "Cuenta DETFLIX"} • Máximo ${user?.maxProfiles ?: 5} perfiles por cuenta",
            style = MaterialTheme.typography.bodyMedium,
            color = DetflixTextSecondary
        )
        Spacer(modifier = Modifier.height(20.dp))

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 140.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(profiles, key = { it.id }) { profile ->
                val isSelected = profile.id == activeProfileId
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(DetflixSurface)
                        .clickable {
                            if (!profile.parentalPin.isNullOrBlank()) {
                                pinInput = ""
                                pinError = null
                                pinPromptProfile = profile
                            } else {
                                onSelectProfile(profile.id)
                                onContinueToHome()
                            }
                        }
                        .padding(14.dp)
                        .testTag("profile_card_${profile.id}")
                ) {
                    Box {
                        AvatarBadge(
                            avatarId = profile.avatarId,
                            name = profile.name,
                            isKids = profile.isKids,
                            size = 78.dp,
                            isSelected = isSelected
                        )
                        IconButton(
                            onClick = { editingProfile = profile },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(32.dp)
                                .testTag("edit_profile_${profile.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = "Editar perfil ${profile.name}",
                                tint = DetflixWhite,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = profile.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = DetflixWhite
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (profile.isKids) {
                            BadgePill(text = "INFANTIL", background = DetflixGold, contentColor = DetflixBlack)
                        }
                        if (!profile.parentalPin.isNullOrBlank()) {
                            Icon(
                                imageVector = Icons.Filled.Lock,
                                contentDescription = "Protegido con PIN",
                                tint = DetflixTextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            if (profiles.size < (user?.maxProfiles ?: 5)) {
                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .height(152.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(DetflixSurfaceElevated)
                            .clickable { showCreateDialog = true }
                            .padding(14.dp)
                            .testTag("add_profile_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Agregar perfil",
                            tint = DetflixWhite,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Nuevo Perfil",
                            style = MaterialTheme.typography.labelLarge,
                            color = DetflixTextSecondary
                        )
                    }
                }
            }
        }
    }

    if (showCreateDialog || editingProfile != null) {
        ProfileEditorDialog(
            existing = editingProfile,
            canDelete = profiles.size > 1 && editingProfile != null,
            onDismiss = { showCreateDialog = false; editingProfile = null },
            onSave = { name, avatarId, isKids, pin, genres ->
                onSaveProfile(editingProfile?.id, name, avatarId, isKids, pin, genres)
                showCreateDialog = false
                editingProfile = null
            },
            onDelete = {
                editingProfile?.let { onDeleteProfile(it.id) }
                editingProfile = null
            }
        )
    }

    pinPromptProfile?.let { lockedProfile ->
        AlertDialog(
            onDismissRequest = { pinPromptProfile = null },
            containerColor = DetflixSurface,
            title = { Text("PIN Parental — ${lockedProfile.name}", color = DetflixWhite) },
            text = {
                Column {
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { pinInput = it },
                        label = { Text("Ingresa tu PIN de 4 dígitos") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    pinError?.let {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(it, color = DetflixRed, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pinInput == lockedProfile.parentalPin) {
                            pinPromptProfile = null
                            onSelectProfile(lockedProfile.id)
                            onContinueToHome()
                        } else {
                            pinError = "PIN incorrecto"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DetflixRed)
                ) {
                    Text("Desbloquear")
                }
            },
            dismissButton = {
                TextButton(onClick = { pinPromptProfile = null }) {
                    Text("Cancelar", color = DetflixTextSecondary)
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProfileEditorDialog(
    existing: ProfileEntity?,
    canDelete: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, String, Boolean, String?, List<String>) -> Unit,
    onDelete: () -> Unit
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var avatarId by remember { mutableStateOf(existing?.avatarId ?: DetflixAvatars.all.first().id) }
    var isKids by remember { mutableStateOf(existing?.isKids ?: false) }
    var pin by remember { mutableStateOf(existing?.parentalPin ?: "") }
    var selectedGenres by remember {
        mutableStateOf(
            existing?.preferredGenres?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }?.toSet()
                ?: setOf("Acción", "Ciencia ficción")
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DetflixSurface,
        title = {
            Text(
                text = if (existing == null) "Crear Perfil" else "Editar Perfil",
                color = DetflixWhite
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre del perfil") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DetflixRed,
                        unfocusedBorderColor = DetflixBorder,
                        focusedTextColor = DetflixWhite,
                        unfocusedTextColor = DetflixWhite
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("profile_name_input")
                )

                Text("Avatar", style = MaterialTheme.typography.labelLarge, color = DetflixWhite)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DetflixAvatars.all.forEach { avatar ->
                        Box(
                            modifier = Modifier
                                .clickable { avatarId = avatar.id }
                                .testTag("avatar_option_${avatar.id}")
                        ) {
                            AvatarBadge(
                                avatarId = avatar.id,
                                name = name.ifBlank { "D" },
                                isKids = avatar.isKidsAvatar,
                                size = 46.dp,
                                isSelected = avatarId == avatar.id
                            )
                            if (avatarId == avatar.id) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = DetflixWhite,
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .size(16.dp)
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Perfil Infantil (Control Parental)", color = DetflixWhite, style = MaterialTheme.typography.bodyLarge)
                        Text("Solo muestra contenido apto TP / +7", color = DetflixTextSecondary, style = MaterialTheme.typography.labelSmall)
                    }
                    Switch(
                        checked = isKids,
                        onCheckedChange = { isKids = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = DetflixWhite, checkedTrackColor = DetflixRed)
                    )
                }

                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 4) pin = it.filter { ch -> ch.isDigit() } },
                    label = { Text("PIN parental (opcional, 4 dígitos)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DetflixRed,
                        unfocusedBorderColor = DetflixBorder,
                        focusedTextColor = DetflixWhite,
                        unfocusedTextColor = DetflixWhite
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Géneros para recomendaciones", style = MaterialTheme.typography.labelLarge, color = DetflixWhite)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    DetflixCategory.entries.take(8).forEach { cat ->
                        val selected = selectedGenres.contains(cat.displayName)
                        FilterChip(
                            selected = selected,
                            onClick = {
                                selectedGenres = if (selected) selectedGenres - cat.displayName
                                else selectedGenres + cat.displayName
                            },
                            label = { Text(cat.displayName, style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = DetflixRed,
                                selectedLabelColor = DetflixWhite
                            )
                        )
                    }
                }

                if (canDelete) {
                    TextButton(onClick = onDelete) {
                        Text("Eliminar perfil", color = DetflixRed)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, avatarId, isKids, pin.takeIf { it.length == 4 }, selectedGenres.toList()) },
                colors = ButtonDefaults.buttonColors(containerColor = DetflixRed),
                modifier = Modifier.testTag("save_profile_button")
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = DetflixTextSecondary)
            }
        }
    )
}
