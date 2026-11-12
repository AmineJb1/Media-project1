@file:OptIn(androidx.media3.common.util.UnstableApi::class)
package com.mediaplus.afrique

import android.content.Context
import android.media.AudioManager
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusable
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.MediaItem
import androidx.media3.ui.AspectRatioFrameLayout
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TvPlayer(s: AppState) {
    val ctx = LocalContext.current
    val audio = remember { ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val player = remember {
        ExoPlayer.Builder(ctx)
            .setMediaSourceFactory(DefaultMediaSourceFactory(PlayerCache.liveFactory()))
            .setLoadControl(DefaultLoadControl.Builder().setBufferDurationsMs(15000, 50000, 1500, 3000).build())
            .build()
    }
    var menuVisible by remember { mutableStateOf(false) }
    var settingsVisible by remember { mutableStateOf(false) }
    var infoVisible by remember { mutableStateOf(true) }
    var resizeMode by remember { mutableIntStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }
    var tracksRevision by remember { mutableIntStateOf(0) }
    LaunchedEffect(s.playingChannel?.id) { infoVisible = true }
    LaunchedEffect(infoVisible, settingsVisible, menuVisible, s.playingChannel?.id) {
        if (infoVisible && !settingsVisible && !menuVisible) { delay(5000); infoVisible = false }
    }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    var videoSize by remember { mutableStateOf(VideoSize.UNKNOWN) }
    var epg by remember { mutableStateOf<List<Prog>>(emptyList()) }
    var epgError by remember { mutableStateOf(false) }
    val channel = s.playingChannel
    val url = s.playing
    // Keep the fullscreen channel drawer and remote navigation inside the current bouquet.
    val channels = if (s.playbackBouquetChannels.any { it.id == channel?.id }) {
        s.playbackBouquetChannels
    } else {
        val bouquet = channel?.group
        if (bouquet == null) s.live else s.live.filter { it.group == bouquet }
    }
    val now = System.currentTimeMillis()
    val current = epg.firstOrNull { now in it.start until it.end }
    val next = epg.firstOrNull { it.start >= (current?.end ?: now) }
    val quality = if (videoSize.width > 0 && videoSize.height > 0) "${videoSize.width} × ${videoSize.height}" else "Résolution…"
    /* val qualityLabel = when {
        videoSize.height >= 2160 -> "4K"
        videoSize.height >= 1080 -> "Full HD"
        videoSize.height >= 720 -> "HD"
        videoSize.height > 0 -> "SD"
        else -> "Qualité en attente"
    } */
    val time = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onVideoSizeChanged(size: VideoSize) { videoSize = size }
            override fun onTracksChanged(tracks: androidx.media3.common.Tracks) { tracksRevision++ }
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                s.onPlayerError(error.message ?: "Erreur vidéo")
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener); player.release() }
    }
    LaunchedEffect(url) {
        if (url != null) {
            player.stop()
            player.clearMediaItems()
            player.setMediaItem(MediaItem.fromUri(url))
            player.prepare()
            player.playWhenReady = true
        }
    }
    LaunchedEffect(channel?.id, s.mode) {
        epg = emptyList(); epgError = false
        if (channel != null && s.mode == 0) {
            try { epg = withContext(Dispatchers.IO) { s.epg(channel.id) } }
            catch (_: Exception) { epgError = true }
        }
    }
    fun changeChannel(delta: Int) {
        val index = channels.indexOfFirst { it.id == channel?.id }
        if (index >= 0 && channels.isNotEmpty()) s.playLive(channels[(index + delta + channels.size) % channels.size])
    }
    Box(
        Modifier.fillMaxSize().background(Color.Black)
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) false else when (event.key) {
                    Key.DirectionUp -> { if (menuVisible) false else { changeChannel(-1); true } }
                    Key.DirectionDown -> { if (menuVisible) false else { changeChannel(1); true } }
                    Key.DirectionLeft -> { if (menuVisible) false else { audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI); true } }
                    Key.DirectionRight -> { if (menuVisible) false else { audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI); true } }
                    Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> { infoVisible = true; menuVisible = !menuVisible; true }
                    Key.Back, Key.Escape -> { if (settingsVisible) { settingsVisible = false; true } else if (menuVisible) { menuVisible = false; true } else false }
                    else -> false
                }
            }.focusRequester(focusRequester).focusable()
    ) {
        AndroidView(
            factory = { PlayerView(it).apply { this.player = player; useController = false; keepScreenOn = true; resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT } },
            update = { it.resizeMode = resizeMode },
            modifier = Modifier.fillMaxSize()
        )
        Box(Modifier.fillMaxSize().clickable { infoVisible = !infoVisible })
        if (infoVisible) {
            Row(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                    .background(Color(0xD9000000)).padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val logo = channel?.logo.orEmpty()
                if (logo.startsWith("http")) {
                    AsyncImage(model = logo, contentDescription = "Logo de la chaîne",
                        modifier = Modifier.size(48.dp), contentScale = ContentScale.Fit)
                } else {
                    Text("◉", color = Color.White, fontSize = 26.sp)
                }
                Column(Modifier.weight(1f)) {
                    Text(channel?.name ?: "Direct", color = Color.White,
                        fontWeight = FontWeight.SemiBold, maxLines = 1)
                    Text(if (current != null) current.title else "EPG indisponible",
                        color = Color.LightGray, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                }
                Text(quality, color = Color.White, style = MaterialTheme.typography.labelSmall)
                TextButton(onClick = { settingsVisible = true; infoVisible = true }) {
                    Text("⚙", color = Color.White, fontSize = 24.sp)
                }
            }
        }
        if (settingsVisible) {
            val availableGroups = player.currentTracks.groups
            val audioGroups = availableGroups.filter { it.type == C.TRACK_TYPE_AUDIO }
            val textGroups = availableGroups.filter { it.type == C.TRACK_TYPE_TEXT }
            Surface(
                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().fillMaxWidth(0.82f),
                color = Color(0xF0152119),
                shape = RoundedCornerShape(topStart = 18.dp, bottomStart = 18.dp)
            ) {
                Column(Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState()).padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Paramètres du lecteur", color = Color.White,
                            fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        TextButton(onClick = { settingsVisible = false }) { Text("Fermer") }
                    }
                    Text("Format d'image", color = Color.White, fontWeight = FontWeight.Bold)
                    listOf(
                        "Ajuster" to AspectRatioFrameLayout.RESIZE_MODE_FIT,
                        "Remplir" to AspectRatioFrameLayout.RESIZE_MODE_FILL,
                        "Zoom" to AspectRatioFrameLayout.RESIZE_MODE_ZOOM,
                        "Original" to AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH
                    ).forEach { (name, value) ->
                        TextButton(onClick = { resizeMode = value }) {
                            Text((if (resizeMode == value) "✓ " else "") + name, color = Color.White)
                        }
                    }
                    Text("Audio disponible", color = Color.White, fontWeight = FontWeight.Bold)
                    if (audioGroups.isEmpty()) Text("Aucune piste audio détectée", color = Color.LightGray)
                    audioGroups.forEach { group ->
                        for (index in 0 until group.length) {
                            if (group.isTrackSupported(index)) {
                                val format = group.getTrackFormat(index)
                                val title = listOfNotNull(format.language?.takeUnless { it == "und" },
                                    format.label).joinToString(" · ").ifBlank { "Audio ${index + 1}" }
                                TextButton(onClick = {
                                    player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
                                        .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, index)).build()
                                }) {
                                    Text((if (group.isTrackSelected(index)) "✓ " else "") + title, color = Color.White)
                                }
                            }
                        }
                    }
                    Text("Sous-titres", color = Color.White, fontWeight = FontWeight.Bold)
                    TextButton(onClick = {
                        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
                            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true).build()
                    }) { Text("Désactivés", color = Color.White) }
                    if (textGroups.isEmpty()) Text("Aucun sous-titre disponible", color = Color.LightGray)
                    textGroups.forEach { group ->
                        for (index in 0 until group.length) {
                            if (group.isTrackSupported(index)) {
                                val format = group.getTrackFormat(index)
                                val title = listOfNotNull(format.language?.takeUnless { it == "und" },
                                    format.label).joinToString(" · ").ifBlank { "Sous-titre ${index + 1}" }
                                TextButton(onClick = {
                                    player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
                                        .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                                        .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, index)).build()
                                }) {
                                    Text((if (group.isTrackSelected(index)) "✓ " else "") + title, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }
        if (menuVisible) {
            Surface(
                Modifier.align(Alignment.CenterStart).fillMaxHeight().fillMaxWidth(0.72f),
                color = Color(0xF0152119), shape = RoundedCornerShape(topEnd = 18.dp, bottomEnd = 18.dp)
            ) {
                Column(Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(s.playbackBouquetName?.takeIf { s.playbackBouquetChannels.any { item -> item.id == channel?.id } }
                            ?: channel?.group?.takeIf { it.isNotBlank() } ?: "Chaînes en direct", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        TextButton(onClick = { menuVisible = false }) { Text("Fermer") }
                    }
                    LazyColumn {
                        items(channels, key = { it.id + it.url }) { item ->
                            Text(
                                item.name, color = if (item.id == channel?.id) Color(0xFFFFA629) else Color.White,
                                modifier = Modifier.fillMaxWidth().clickable { s.playLive(item); menuVisible = false }.padding(vertical = 12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
