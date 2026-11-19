package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.CatalogItem
import com.example.data.model.ContentType
import com.example.data.model.ContinueWatchingEntry
import com.example.data.model.DetflixAvatars
import com.example.ui.theme.DetflixBlack
import com.example.ui.theme.DetflixBorder
import com.example.ui.theme.DetflixGold
import com.example.ui.theme.DetflixRed
import com.example.ui.theme.DetflixSurface
import com.example.ui.theme.DetflixSurfaceElevated
import com.example.ui.theme.DetflixTextSecondary
import com.example.ui.theme.DetflixWhite

@Composable
fun DetflixBrandMark(
    modifier: Modifier = Modifier,
    size: Dp = 38.dp,
    showWordmark: Boolean = true
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = R.drawable.detflix_logo),
            contentDescription = stringResource(id = R.string.cd_detflix_logo),
            modifier = Modifier
                .size(size)
                .clip(RoundedCornerShape(10.dp)),
            contentScale = ContentScale.Crop
        )
        if (showWordmark) {
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "DET",
                style = MaterialTheme.typography.headlineLarge,
                color = DetflixWhite
            )
            Text(
                text = "FLIX",
                style = MaterialTheme.typography.headlineLarge,
                color = DetflixRed
            )
        }
    }
}

@Composable
fun AvatarBadge(
    avatarId: String,
    name: String,
    isKids: Boolean,
    size: Dp = 44.dp,
    isSelected: Boolean = false
) {
    val option = DetflixAvatars.findById(avatarId)
    val initial = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "D"
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(option.primaryColorHex), Color(option.accentColorHex))
                )
            )
            .border(
                width = if (isSelected) 2.5.dp else 1.dp,
                color = if (isSelected) DetflixWhite else Color.White.copy(alpha = 0.2f),
                shape = RoundedCornerShape(14.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isKids) {
            Icon(
                imageVector = Icons.Filled.ChildCare,
                contentDescription = "Perfil infantil",
                tint = DetflixWhite,
                modifier = Modifier.size(size * 0.55f)
            )
        } else {
            Text(
                text = initial,
                color = DetflixWhite,
                fontWeight = FontWeight.Bold,
                fontSize = (size.value * 0.44f).sp
            )
        }
    }
}

@Composable
fun PosterCard(
    item: CatalogItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = 132.dp
) {
    Card(
        modifier = modifier
            .width(width)
            .testTag("poster_card_${item.id}")
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = DetflixSurfaceElevated),
        border = BorderStroke(1.dp, DetflixBorder.copy(alpha = 0.6f))
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.72f)
                    .background(DetflixSurface)
            ) {
                AsyncImage(
                    model = item.posterUrl,
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Transparent,
                                    DetflixBlack.copy(alpha = 0.85f)
                                )
                            )
                        )
                )
                // Top-left type / new badge
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (item.type == ContentType.SERIES) {
                        BadgePill(text = "SERIE", background = DetflixRed)
                    } else if (item.isNewRelease) {
                        BadgePill(text = "ESTRENO", background = DetflixRed)
                    }
                }
                // Rating bottom-right
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        tint = DetflixGold,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = String.format("%.1f", item.rating),
                        style = MaterialTheme.typography.labelSmall,
                        color = DetflixWhite
                    )
                }
                // Fallback icon if image fails subtly behind
                if (item.posterUrl.isBlank()) {
                    Icon(
                        imageVector = Icons.Filled.Movie,
                        contentDescription = null,
                        tint = DetflixTextSecondary,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(32.dp)
                    )
                }
            }
            Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.labelLarge,
                    color = DetflixWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${item.year} • ${item.ageRating.code}",
                    style = MaterialTheme.typography.labelSmall,
                    color = DetflixTextSecondary,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun ContinueWatchingCard(
    entry: ContinueWatchingEntry,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .width(210.dp)
            .testTag("continue_card_${entry.item.id}")
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = DetflixSurfaceElevated),
        border = BorderStroke(1.dp, DetflixBorder)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(118.dp)
            ) {
                AsyncImage(
                    model = entry.episode?.thumbnailUrl ?: entry.item.backdropUrl,
                    contentDescription = entry.item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(DetflixBlack.copy(alpha = 0.35f))
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(DetflixRed.copy(alpha = 0.92f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = stringResource(id = R.string.cd_play),
                        tint = DetflixWhite,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
            LinearProgressIndicator(
                progress = { entry.progressPercent },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
                color = DetflixRed,
                trackColor = DetflixBorder
            )
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = entry.item.title,
                    style = MaterialTheme.typography.labelLarge,
                    color = DetflixWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val subtitle = entry.episode?.let { "T${it.seasonNumber} E${it.episodeNumber} • ${it.title}" }
                    ?: "${(entry.progressPercent * 100).toInt()}% visto"
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = DetflixTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun BadgePill(
    text: String,
    background: Color = DetflixSurfaceElevated,
    contentColor: Color = DetflixWhite
) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = background
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun MembershipExpiredGateDialog(
    onGoToMembership: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DetflixSurface,
        icon = {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = null,
                tint = DetflixRed,
                modifier = Modifier.size(36.dp)
            )
        },
        title = {
            Text(
                text = stringResource(id = R.string.membership_required_title),
                color = DetflixWhite,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Text(
                text = stringResource(id = R.string.membership_required_body),
                color = DetflixTextSecondary,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(
                onClick = onGoToMembership,
                colors = ButtonDefaults.buttonColors(containerColor = DetflixRed),
                modifier = Modifier.testTag("gate_go_to_membership_button")
            ) {
                Text("Ingresar Código", color = DetflixWhite)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                border = BorderStroke(1.dp, DetflixBorder)
            ) {
                Text("Cerrar", color = DetflixTextSecondary)
            }
        }
    )
}
