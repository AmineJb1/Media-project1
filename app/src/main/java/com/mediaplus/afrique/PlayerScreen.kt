@file:OptIn(UnstableApi::class)

package com.mediaplus.afrique

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import android.view.View
import android.graphics.drawable.GradientDrawable
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import androidx.media3.ui.AspectRatioFrameLayout

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
        factory = { PlayerView(it).apply {
            this.player = player
            keepScreenOn = true
            useController = showControls
            if (showControls) {
                controllerShowTimeoutMs = 8000
                controllerAutoShow = true
                val density = resources.displayMetrics.density
                findViewById<View>(androidx.media3.ui.R.id.exo_bottom_bar)?.apply {
                    minimumHeight = (96 * density).toInt()
                    setPadding((14 * density).toInt(), (12 * density).toInt(), (14 * density).toInt(), (16 * density).toInt())
                    setBackgroundColor(android.graphics.Color.argb(205, 9, 27, 24))
                }
                val seek = findViewById<View>(androidx.media3.ui.R.id.exo_progress)
                val settings = findViewById<View>(androidx.media3.ui.R.id.exo_settings)
                listOfNotNull(seek, settings).forEach { control ->
                    control.isFocusable = true
                    control.setOnFocusChangeListener { v, focused ->
                        v.background = if (focused) GradientDrawable().apply {
                            setColor(android.graphics.Color.argb(190, 4, 91, 54))
                            setStroke((3 * density).toInt(), android.graphics.Color.rgb(44, 255, 134))
                            cornerRadius = 12 * density
                        } else null
                    }
                }
                settings?.nextFocusLeftId = androidx.media3.ui.R.id.exo_progress
                seek?.nextFocusRightId = androidx.media3.ui.R.id.exo_settings
            }
            resizeMode = if (showControls) AspectRatioFrameLayout.RESIZE_MODE_ZOOM else AspectRatioFrameLayout.RESIZE_MODE_FIT
            setBackgroundColor(android.graphics.Color.BLACK)
            layoutParams = android.widget.FrameLayout.LayoutParams(-1, -1)
        } },
        modifier = modifier.background(Color.Black).fillMaxSize()
    )
}
