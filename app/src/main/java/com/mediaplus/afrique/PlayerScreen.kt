@file:OptIn(UnstableApi::class)

package com.mediaplus.afrique

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView

@Composable
fun VideoPlayer(url: String, onError: (String) -> Unit, modifier: Modifier = Modifier.fillMaxSize(), showControls: Boolean = true, onVideoResolution: (Int, Int) -> Unit = { _, _ -> }, sharedPlayer: ExoPlayer? = null) {
    val ctx = LocalContext.current
    val player = remember(sharedPlayer) {
        sharedPlayer ?: ExoPlayer.Builder(ctx)
            .setMediaSourceFactory(DefaultMediaSourceFactory(if (showControls) PlayerCache.factory(ctx) else PlayerCache.liveFactory()))
            .setLoadControl(DefaultLoadControl.Builder().setBufferDurationsMs(15000, 50000, 1500, 3000).build())
            .build()
    }
    DisposableEffect(player) {
        val l = object : Player.Listener {
            override fun onVideoSizeChanged(size: androidx.media3.common.VideoSize) { onVideoResolution(size.width, size.height) }
            override fun onPlayerError(error: PlaybackException) { onError(error.message ?: "erreur") }
        }
        player.addListener(l)
        onDispose { player.removeListener(l); if (sharedPlayer == null) player.release() }
    }
    LaunchedEffect(url) {
        if (player.currentMediaItem?.localConfiguration?.uri?.toString() != url) {
            player.setMediaItem(MediaItem.fromUri(url))
            player.prepare()
        }
        player.playWhenReady = true
    }
    AndroidView(
        factory = { PlayerView(it).apply { this.player = player; keepScreenOn = true; useController = showControls } },
        modifier = modifier
    )
}
