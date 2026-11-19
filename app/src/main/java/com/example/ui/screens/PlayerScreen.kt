package com.example.ui.screens

import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.PlayerView
import com.example.R
import com.example.data.model.CatalogItem
import com.example.data.model.EpisodeItem
import com.example.ui.theme.DetflixBlack
import com.example.ui.theme.DetflixBorder
import com.example.ui.theme.DetflixRed
import com.example.ui.theme.DetflixSurface
import com.example.ui.theme.DetflixTextSecondary
import com.example.ui.theme.DetflixWhite
import kotlinx.coroutines.delay

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    item: CatalogItem,
    episode: EpisodeItem?,
    initialQuality: String,
    initialSubtitlesEnabled: Boolean,
    autoPlayNextEnabled: Boolean,
    onGetResumePosition: suspend (String, String?) -> Long,
    onSaveProgress: (String, String?, Long, Long) -> Unit,
    onSavePlayerPrefs: (String, Boolean, Boolean) -> Unit,
    onPlayNextEpisode: (EpisodeItem) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity

    var isFullscreen by remember { mutableStateOf(false) }
    var controlsVisible by remember { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(true) }
    var isBuffering by remember { mutableStateOf(true) }
    var currentPosMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var volume by remember { mutableFloatStateOf(1f) }
    var selectedQuality by remember { mutableStateOf(initialQuality) }
    var subtitlesEnabled by remember { mutableStateOf(initialSubtitlesEnabled) }
    var selectedSubtitleLang by remember { mutableStateOf(item.subtitles.firstOrNull() ?: "Español") }
    var selectedAudioLang by remember { mutableStateOf(item.availableLanguages.firstOrNull() ?: item.language) }
    var showQualityDialog by remember { mutableStateOf(false) }
    var showSubtitleDialog by remember { mutableStateOf(false) }
    var showAudioDialog by remember { mutableStateOf(false) }
    var showNextEpisodeCountdown by remember { mutableStateOf(false) }
    var nextCountdownSec by remember { mutableIntStateOf(5) }

    val nextEpisode = remember(item.episodes, episode) {
        if (episode == null) null
        else {
            val idx = item.episodes.indexOfFirst { it.id == episode.id }
            if (idx >= 0 && idx + 1 < item.episodes.size) item.episodes[idx + 1] else null
        }
    }

    val streamUrl = episode?.streamUrl ?: item.streamUrl
    val displayTitle = if (episode != null) "${item.title} — T${episode.seasonNumber}:E${episode.episodeNumber} ${episode.title}" else item.title

    val trackSelector = remember { DefaultTrackSelector(context) }
    val exoPlayer = remember {
        ExoPlayer.Builder(context)
            .setTrackSelector(trackSelector)
            .build()
    }

    // Apply quality & subtitle track constraints
    LaunchedEffect(selectedQuality, subtitlesEnabled, selectedSubtitleLang) {
        val maxHeight = when (selectedQuality) {
            "1080p" -> 1080
            "720p" -> 720
            "480p" -> 480
            else -> Int.MAX_VALUE
        }
        trackSelector.setParameters(
            trackSelector.buildUponParameters()
                .setMaxVideoSize(Int.MAX_VALUE, maxHeight)
                .setPreferredTextLanguage(if (subtitlesEnabled) selectedSubtitleLang.take(2).lowercase() else null)
                .setSelectUndeterminedTextLanguage(subtitlesEnabled)
        )
        onSavePlayerPrefs(selectedQuality, autoPlayNextEnabled, subtitlesEnabled)
    }

    // Prepare media item and resume from saved watch history
    LaunchedEffect(streamUrl) {
        showNextEpisodeCountdown = false
        nextCountdownSec = 5
        val resumeMs = onGetResumePosition(item.id, episode?.id)
        exoPlayer.setMediaItem(MediaItem.fromUri(streamUrl))
        exoPlayer.prepare()
        if (resumeMs > 2000L) {
            exoPlayer.seekTo(resumeMs)
        }
        exoPlayer.playWhenReady = true
    }

    // Fullscreen orientation & immersive bars
    LaunchedEffect(isFullscreen) {
        activity?.let { act ->
            val controller = WindowCompat.getInsetsController(act.window, act.window.decorView)
            if (isFullscreen) {
                act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                controller.hide(WindowInsetsCompat.Type.systemBars())
                controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    // Player listener
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = playbackState == Player.STATE_BUFFERING
                if (playbackState == Player.STATE_ENDED && nextEpisode != null && autoPlayNextEnabled) {
                    showNextEpisodeCountdown = true
                }
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            onSaveProgress(item.id, episode?.id, exoPlayer.currentPosition, exoPlayer.duration.coerceAtLeast(1L))
            exoPlayer.removeListener(listener)
            exoPlayer.release()
            activity?.let { act ->
                act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                WindowCompat.getInsetsController(act.window, act.window.decorView)
                    .show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    // Periodic position ticker + auto-save watch progress
    LaunchedEffect(exoPlayer) {
        var tick = 0
        while (true) {
            currentPosMs = exoPlayer.currentPosition.coerceAtLeast(0L)
            durationMs = exoPlayer.duration.coerceAtLeast(0L)
            if (tick % 5 == 0 && durationMs > 0L) {
                onSaveProgress(item.id, episode?.id, currentPosMs, durationMs)
            }
            tick++
            delay(1000L)
        }
    }

    // Auto-play next episode countdown
    LaunchedEffect(showNextEpisodeCountdown, nextEpisode) {
        if (showNextEpisodeCountdown && nextEpisode != null) {
            nextCountdownSec = 5
            while (nextCountdownSec > 0 && showNextEpisodeCountdown) {
                delay(1000L)
                nextCountdownSec--
            }
            if (showNextEpisodeCountdown) {
                showNextEpisodeCountdown = false
                onPlayNextEpisode(nextEpisode)
            }
        }
    }

    BackHandler {
        if (isFullscreen) isFullscreen = false else onBack()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { controlsVisible = !controlsVisible }
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        if (isBuffering) {
            CircularProgressIndicator(
                color = DetflixRed,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // Custom Cinematic Player Overlay
        AnimatedVisibility(
            visible = controlsVisible,
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(16.dp)
            ) {
                // Top Bar
                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("player_back_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(id = R.string.cd_back), tint = DetflixWhite)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(displayTitle, style = MaterialTheme.typography.titleMedium, color = DetflixWhite, maxLines = 1)
                            Text("Calidad: $selectedQuality • Audio: $selectedAudioLang", style = MaterialTheme.typography.labelSmall, color = DetflixTextSecondary)
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        IconButton(onClick = { showAudioDialog = true }, modifier = Modifier.testTag("player_audio_button")) {
                            Icon(Icons.Filled.Language, contentDescription = "Idioma de audio", tint = DetflixWhite)
                        }
                        IconButton(onClick = { showSubtitleDialog = true }, modifier = Modifier.testTag("player_subtitles_button")) {
                            Icon(
                                Icons.Filled.ClosedCaption,
                                contentDescription = "Subtítulos",
                                tint = if (subtitlesEnabled) DetflixRed else DetflixWhite
                            )
                        }
                        IconButton(onClick = { showQualityDialog = true }, modifier = Modifier.testTag("player_quality_button")) {
                            Icon(Icons.Filled.HighQuality, contentDescription = "Calidad de video", tint = DetflixWhite)
                        }
                        IconButton(onClick = { isFullscreen = !isFullscreen }, modifier = Modifier.testTag("player_fullscreen_button")) {
                            Icon(
                                if (isFullscreen) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen,
                                contentDescription = stringResource(id = R.string.cd_fullscreen),
                                tint = DetflixWhite
                            )
                        }
                    }
                }

                // Center Play / Pause / Skip -10s / +10s
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(28.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { exoPlayer.seekTo((exoPlayer.currentPosition - 10_000L).coerceAtLeast(0L)) },
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                            .testTag("player_rewind_button")
                    ) {
                        Icon(Icons.Filled.Replay10, contentDescription = "Retroceder 10 segundos", tint = DetflixWhite, modifier = Modifier.size(32.dp))
                    }

                    IconButton(
                        onClick = { if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play() },
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(DetflixRed)
                            .testTag("player_play_pause_button")
                    ) {
                        Icon(
                            if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (isPlaying) stringResource(id = R.string.cd_pause) else stringResource(id = R.string.cd_play),
                            tint = DetflixWhite,
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    IconButton(
                        onClick = { exoPlayer.seekTo((exoPlayer.currentPosition + 10_000L).coerceAtMost(durationMs.coerceAtLeast(0L))) },
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                            .testTag("player_forward_button")
                    ) {
                        Icon(Icons.Filled.Forward10, contentDescription = "Avanzar 10 segundos", tint = DetflixWhite, modifier = Modifier.size(32.dp))
                    }
                }

                // Bottom Progress + Volume + Next Episode
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                ) {
                    val safeDuration = durationMs.coerceAtLeast(1L).toFloat()
                    Slider(
                        value = (currentPosMs.toFloat() / safeDuration).coerceIn(0f, 1f),
                        onValueChange = { fraction ->
                            exoPlayer.seekTo((fraction * safeDuration).toLong())
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = DetflixRed,
                            activeTrackColor = DetflixRed,
                            inactiveTrackColor = Color.White.copy(alpha = 0.35f)
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("player_seek_slider")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${formatDuration(currentPosMs)} / ${formatDuration(durationMs)}",
                            style = MaterialTheme.typography.labelLarge,
                            color = DetflixWhite
                        )

                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.width(170.dp)) {
                            IconButton(onClick = {
                                volume = if (volume > 0f) 0f else 1f
                                exoPlayer.volume = volume
                            }) {
                                Icon(
                                    if (volume == 0f) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = "Volumen",
                                    tint = DetflixWhite
                                )
                            }
                            Slider(
                                value = volume,
                                onValueChange = {
                                    volume = it
                                    exoPlayer.volume = it
                                },
                                colors = SliderDefaults.colors(thumbColor = DetflixWhite, activeTrackColor = DetflixWhite),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        if (nextEpisode != null) {
                            TextButton(
                                onClick = { onPlayNextEpisode(nextEpisode) },
                                modifier = Modifier.testTag("player_next_episode_button")
                            ) {
                                Icon(Icons.Filled.SkipNext, contentDescription = null, tint = DetflixWhite)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Siguiente Ep.", color = DetflixWhite)
                            }
                        }
                    }
                }
            }
        }

        // Next Episode Countdown Overlay
        if (showNextEpisodeCountdown && nextEpisode != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = DetflixSurface.copy(alpha = 0.96f)),
                border = BorderStroke(1.dp, DetflixRed),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Siguiente episodio en ${nextCountdownSec}s", style = MaterialTheme.typography.titleMedium, color = DetflixWhite)
                    Text("T${nextEpisode.seasonNumber}:E${nextEpisode.episodeNumber} — ${nextEpisode.title}", style = MaterialTheme.typography.bodyMedium, color = DetflixTextSecondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = { showNextEpisodeCountdown = false; onPlayNextEpisode(nextEpisode) },
                            colors = ButtonDefaults.buttonColors(containerColor = DetflixRed)
                        ) {
                            Text("Reproducir ahora")
                        }
                        OutlinedButton(
                            onClick = { showNextEpisodeCountdown = false },
                            border = BorderStroke(1.dp, DetflixBorder)
                        ) {
                            Text("Cancelar", color = DetflixTextSecondary)
                        }
                    }
                }
            }
        }
    }

    if (showQualityDialog) {
        OptionPickerDialog(
            title = "Calidad de Video",
            options = listOf("Auto", "1080p", "720p", "480p"),
            selected = selectedQuality,
            onSelect = { selectedQuality = it; showQualityDialog = false },
            onDismiss = { showQualityDialog = false }
        )
    }

    if (showSubtitleDialog) {
        val subtitleChoices = listOf("Desactivados") + item.subtitles.ifEmpty { listOf("Español", "Inglés") }
        val currentChoice = if (!subtitlesEnabled) "Desactivados" else selectedSubtitleLang
        OptionPickerDialog(
            title = "Subtítulos",
            options = subtitleChoices,
            selected = currentChoice,
            onSelect = { choice ->
                if (choice == "Desactivados") {
                    subtitlesEnabled = false
                } else {
                    subtitlesEnabled = true
                    selectedSubtitleLang = choice
                }
                showSubtitleDialog = false
            },
            onDismiss = { showSubtitleDialog = false }
        )
    }

    if (showAudioDialog) {
        val audioChoices = item.availableLanguages.ifEmpty { listOf(item.language) }
        OptionPickerDialog(
            title = "Idioma de Audio",
            options = audioChoices,
            selected = selectedAudioLang,
            onSelect = { selectedAudioLang = it; showAudioDialog = false },
            onDismiss = { showAudioDialog = false }
        )
    }
}

@Composable
private fun OptionPickerDialog(
    title: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DetflixSurface,
        title = { Text(title, color = DetflixWhite) },
        text = {
            Column {
                options.forEach { opt ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(opt) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = opt == selected,
                            onClick = { onSelect(opt) },
                            colors = RadioButtonDefaults.colors(selectedColor = DetflixRed)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(opt, color = DetflixWhite, fontWeight = if (opt == selected) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar", color = DetflixTextSecondary)
            }
        }
    )
}

private fun formatDuration(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    val hours = totalSec / 3600L
    val mins = (totalSec % 3600L) / 60L
    val secs = totalSec % 60L
    return if (hours > 0) String.format("%d:%02d:%02d", hours, mins, secs)
    else String.format("%02d:%02d", mins, secs)
}
