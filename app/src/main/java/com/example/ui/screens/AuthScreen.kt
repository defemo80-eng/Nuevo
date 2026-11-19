package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.UserEntity
import com.example.ui.theme.DetflixBlack
import com.example.ui.theme.DetflixBorder
import com.example.ui.theme.DetflixRed
import com.example.ui.theme.DetflixRedDark
import com.example.ui.theme.DetflixSuccess
import com.example.ui.theme.DetflixSurface
import com.example.ui.theme.DetflixSurfaceElevated
import com.example.ui.theme.DetflixTextSecondary
import com.example.ui.theme.DetflixWhite
import com.example.ui.viewmodel.AuthUiState

@Composable
fun AuthScreen(
    uiState: AuthUiState,
    savedAccounts: List<UserEntity>,
    onEnterWithNameAndCode: (String, String) -> Unit,
    onEnterSavedAccount: (String) -> Unit,
    onClearMessages: () -> Unit
) {
    var displayName by remember { mutableStateOf("") }
    var accessCode by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        DetflixRedDark.copy(alpha = 0.42f),
                        DetflixBlack,
                        DetflixBlack
                    )
                )
            )
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Image(
                painter = painterResource(id = R.drawable.detflix_logo),
                contentDescription = stringResource(id = R.string.cd_detflix_logo),
                modifier = Modifier
                    .size(110.dp)
                    .clip(RoundedCornerShape(24.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.height(14.dp))
            Row {
                Text("DET", style = MaterialTheme.typography.displayMedium, color = DetflixWhite)
                Text("FLIX", style = MaterialTheme.typography.displayMedium, color = DetflixRed)
            }
            Text(
                text = stringResource(id = R.string.detflix_tagline),
                style = MaterialTheme.typography.bodyMedium,
                color = DetflixTextSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 480.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = DetflixSurface),
                border = BorderStroke(1.dp, DetflixBorder)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Acceso con Nombre y Código",
                        style = MaterialTheme.typography.titleLarge,
                        color = DetflixWhite,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Ingresa tu nombre y el código mensual único entregado por el administrador de DETFLIX.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = DetflixTextSecondary
                    )

                    uiState.errorMessage?.let { err ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(DetflixRed.copy(alpha = 0.16f))
                                .padding(12.dp)
                        ) {
                            Text(err, color = DetflixWhite, style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    uiState.infoMessage?.let { info ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(DetflixSuccess.copy(alpha = 0.16f))
                                .padding(12.dp)
                        ) {
                            Text(info, color = DetflixSuccess, style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    val fieldColors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DetflixRed,
                        unfocusedBorderColor = DetflixBorder,
                        focusedTextColor = DetflixWhite,
                        unfocusedTextColor = DetflixWhite,
                        focusedLabelColor = DetflixWhite,
                        unfocusedLabelColor = DetflixTextSecondary,
                        cursorColor = DetflixRed
                    )

                    OutlinedTextField(
                        value = displayName,
                        onValueChange = {
                            displayName = it
                            if (uiState.errorMessage != null || uiState.infoMessage != null) onClearMessages()
                        },
                        label = { Text("Tu nombre") },
                        placeholder = { Text("Ej. Carlos Cine") },
                        leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        colors = fieldColors,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_name_input")
                    )

                    OutlinedTextField(
                        value = accessCode,
                        onValueChange = {
                            accessCode = it.uppercase()
                            if (uiState.errorMessage != null || uiState.infoMessage != null) onClearMessages()
                        },
                        label = { Text("Código de acceso") },
                        placeholder = { Text("DFX-XXXX-XXXX-XXXX") },
                        leadingIcon = { Icon(Icons.Filled.Key, contentDescription = null) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                        colors = fieldColors,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_code_input")
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = { onEnterWithNameAndCode(displayName, accessCode) },
                        enabled = !uiState.isLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = DetflixRed),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("auth_enter_button")
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = DetflixWhite,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Entrar y Activar", fontWeight = FontWeight.Bold)
                        }
                    }

                    if (savedAccounts.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        HorizontalDivider(color = DetflixBorder)
                        Text(
                            text = "Cuentas guardadas en este dispositivo",
                            style = MaterialTheme.typography.labelLarge,
                            color = DetflixWhite
                        )
                        Text(
                            text = "Toca tu nombre para entrar con tu historial, Mi Lista o renovar tu membresía.",
                            style = MaterialTheme.typography.labelSmall,
                            color = DetflixTextSecondary
                        )
                        savedAccounts.forEach { user ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(DetflixSurfaceElevated)
                                    .clickable(enabled = !uiState.isLoading) { onEnterSavedAccount(user.uid) }
                                    .padding(horizontal = 14.dp, vertical = 12.dp)
                                    .testTag("auth_saved_account_${user.uid}")
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(DetflixRed)
                                ) {
                                    Text(
                                        text = user.displayName.firstOrNull()?.uppercase() ?: "D",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = DetflixWhite,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = user.displayName,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = DetflixWhite
                                    )
                                    Text(
                                        text = "Tocar para entrar o renovar",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = DetflixTextSecondary
                                    )
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = "Entrar como ${user.displayName}",
                                    tint = DetflixTextSecondary
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
