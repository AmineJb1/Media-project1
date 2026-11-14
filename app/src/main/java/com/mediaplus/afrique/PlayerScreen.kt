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
import android.view.KeyEvent
import android.view.ViewGroup
import android.widget.FrameLayout
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
fun VideoPlayer(url: String, onError: (String) -> Unit, modifier: Modifier = Modifier.fillMaxSize(), showControls: Boolean = true, onVideoResolution: (Int, Int) -> Unit = { _, _ -> }, sharedPlayer: ExoPlayer? = null, onBack: () -> Unit = {}, resumePositionMs: Long = 0L, onSavePosition: (Long, Long) -> Unit = { _, _ -> }) {
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
            if (resumePositionMs > 0L) player.seekTo(resumePositionMs)
        }
        player.playWhenReady = true
    }
    val latestSave by rememberUpdatedState(onSavePosition)
    DisposableEffect(player, url) {
        onDispose { latestSave(player.currentPosition, player.duration) }
    }
    LaunchedEffect(player, url) {
        while (true) {
            kotlinx.coroutines.delay(3000)
            if (player.currentPosition > 0) latestSave(player.currentPosition, player.duration)
        }
    }
    AndroidView(
        factory = { object : PlayerView(it) {
            override fun dispatchKeyEvent(event: KeyEvent): Boolean {
                if (showControls && (event.keyCode == KeyEvent.KEYCODE_BACK ||
                    event.keyCode == KeyEvent.KEYCODE_ESCAPE)) {
                    if (event.action == KeyEvent.ACTION_UP) onBack()
                    return true
                }
                if (showControls && event.action == KeyEvent.ACTION_DOWN &&
                    (event.keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
                     event.keyCode == KeyEvent.KEYCODE_ENTER ||
                     event.keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER) &&
                    !isControllerFullyVisible) {
                    player?.pause()
                    controllerShowTimeoutMs = 0
                    showController()
                    post {
                        findViewById<View>(androidx.media3.ui.R.id.exo_play_pause)?.requestFocus()
                            ?: findViewById<View>(androidx.media3.ui.R.id.exo_play)?.requestFocus()
                    }
                    return true
                }
                return super.dispatchKeyEvent(event)
            }
        }.apply {
            this.player = player
            isFocusable = true
            isFocusableInTouchMode = true
            requestFocus()
            keepScreenOn = true
            useController = showControls
            if (showControls) {
                controllerShowTimeoutMs = 5000
                controllerAutoShow = true
                val playButton = findViewById<View>(androidx.media3.ui.R.id.exo_play)
                playButton?.setOnClickListener {
                    player.play()
                    controllerShowTimeoutMs = 5000
                    showController()
                }
                val density = resources.displayMetrics.density
                findViewById<View>(androidx.media3.ui.R.id.exo_bottom_bar)?.apply {
                    minimumHeight = (112 * density).toInt()
                    setPadding((18 * density).toInt(), (16 * density).toInt(), (18 * density).toInt(), (20 * density).toInt())
                    setBackgroundColor(android.graphics.Color.argb(215, 9, 25, 46))
                }
                val seek = findViewById<View>(androidx.media3.ui.R.id.exo_progress)
                val settings = findViewById<View>(androidx.media3.ui.R.id.exo_settings)
                // Keep the native Media3 seekbar focus/scrubber visuals.
                // Applying a background to exo_progress highlights its full width
                // instead of the scrubber and can obscure the timeline on TV.
                seek?.isFocusable = true
                seek?.setOnFocusChangeListener(null)
                seek?.background = null
                settings?.apply { isFocusable = true; minimumWidth = (52 * density).toInt(); minimumHeight = (52 * density).toInt(); setPadding((8 * density).toInt(), (8 * density).toInt(), (8 * density).toInt(), (8 * density).toInt()) }
                settings?.setOnFocusChangeListener { v, focused ->
                    v.background = if (focused) GradientDrawable().apply {
                        setColor(android.graphics.Color.argb(190, 15, 78, 160))
                        setStroke((3 * density).toInt(), android.graphics.Color.rgb(70, 164, 255))
                        cornerRadius = 12 * density
                    } else null
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
