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
import androidx.media3.common.MediaItem
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
            .setMediaSourceFactory(DefaultMediaSourceFactory(PlayerCache.factory(ctx)))
            .setLoadControl(DefaultLoadControl.Builder().setBufferDurationsMs(15000, 50000, 1500, 3000).build())
            .build()
    }
    var menuVisible by remember { mutableStateOf(false) }
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
    val quality = when {
        videoSize.height >= 2160 -> "4K"
        videoSize.height >= 1080 -> "Full HD"
        videoSize.height >= 720 -> "HD"
        videoSize.height > 0 -> "SD"
        else -> "Qualité en attente"
    }
    val time = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onVideoSizeChanged(size: VideoSize) { videoSize = size }
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                s.onPlayerError(error.message ?: "Erreur vidéo")
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener); player.release() }
    }
    LaunchedEffect(url) {
        if (url != null) {
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
                    Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> { menuVisible = !menuVisible; true }
                    Key.Back, Key.Escape -> { if (menuVisible) { menuVisible = false; true } else false }
                    else -> false
                }
            }.focusRequester(focusRequester).focusable()
    ) {
        AndroidView(
            factory = { PlayerView(it).apply { this.player = player; useController = false; keepScreenOn = true } },
            modifier = Modifier.fillMaxSize()
        )
        Box(Modifier.fillMaxSize().clickable { menuVisible = !menuVisible })
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .background(Color(0xD9000000)).padding(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(channel?.name ?: "Lecture", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text(quality, color = Color(0xFFFFA629), fontWeight = FontWeight.Bold)
            }
            Text(
                if (current != null) "En cours  ${time.format(Date(current.start))}–${time.format(Date(current.end))}  ${current.title}"
                else if (epgError) "EPG indisponible" else "Programme EPG non disponible",
                color = Color.White
            )
            if (next != null) Text("À suivre  ${time.format(Date(next.start))}  ${next.title}", color = Color.LightGray)
            Text("OK : chaînes  ·  ↑ ↓ : changer  ·  ← → : volume", color = Color.LightGray, style = MaterialTheme.typography.labelSmall)
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
