@file:OptIn(androidx.media3.common.util.UnstableApi::class)
package com.mediaplus.afrique

import android.content.Context
import android.media.AudioManager
import android.app.Activity
import android.view.View
import androidx.compose.ui.platform.LocalView
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
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

@Composable
fun TvPlayer(s: AppState) {
    val ctx = LocalContext.current
    val configuration = LocalConfiguration.current
    val portrait = configuration.screenHeightDp > configuration.screenWidthDp
    val audio = remember { ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val view = LocalView.current
    DisposableEffect(view) {
        val decor = (ctx as? Activity)?.window?.decorView
        val previous = decor?.systemUiVisibility ?: 0
        decor?.systemUiVisibility = previous or View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        onDispose { decor?.systemUiVisibility = previous }
    }
    val displayPrefs = remember { ctx.getSharedPreferences("player_display", Context.MODE_PRIVATE) }
    val player = remember(s) { s.livePlayer }
    var menuVisible by remember { mutableStateOf(false) }
    var drawerTab by remember { mutableIntStateOf(1) }
    var selectedBouquet by remember { mutableStateOf<String?>(null) }
    var settingsVisible by remember { mutableStateOf(false) }
    var infoVisible by remember { mutableStateOf(true) }
    var resizeMode by remember {
        mutableIntStateOf(displayPrefs.getInt("resize_mode", AspectRatioFrameLayout.RESIZE_MODE_FIT))
    }
    fun setDisplayMode(value: Int) {
        resizeMode = value
        displayPrefs.edit().putInt("resize_mode", value).apply()
    }
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
    val bouquets = remember(s.live, s.adultUnlocked) {
        (s.live.asSequence().map { it.group } + sequenceOf(s.playbackBouquetName.orEmpty())).filter { it.isNotBlank() }
            .filter { !s.isAdultGroup(it) || s.adultUnlocked }.distinct().toList()
    }
    val initialBouquet = s.playbackBouquetName?.takeIf { name ->
        s.playbackBouquetChannels.any { it.id == channel?.id } && name in bouquets
    } ?: channel?.group
    LaunchedEffect(channel?.id) {
        if (!menuVisible) selectedBouquet = initialBouquet
    }
    val drawerBouquet = selectedBouquet ?: initialBouquet
    val drawerChannels = if (drawerBouquet == initialBouquet && s.playbackBouquetChannels.any { it.id == channel?.id }) {
        s.playbackBouquetChannels.filter { !s.isAdultGroup(it.group) || s.adultUnlocked }
    } else s.live.filter { it.group == drawerBouquet && (!s.isAdultGroup(it.group) || s.adultUnlocked) }
    val now = System.currentTimeMillis()
    val current = epg.firstOrNull { now in it.start until it.end }
    val next = epg.firstOrNull { it.start >= (current?.end ?: now) }
    val quality = if (videoSize.width > 0 && videoSize.height > 0) {
        val h = videoSize.height
        val tier = when { h >= 2160 -> "4K"; h >= 1080 -> "FHD"; h >= 720 -> "HD"; else -> "SD" }
        val fps = player.videoFormat?.frameRate?.takeIf { it > 0 }?.let { " · ${it.toInt()} fps" } ?: ""
        "$tier · ${videoSize.width}×$h$fps"
    } else "Qualité : —"

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onVideoSizeChanged(size: VideoSize) { videoSize = size }
            override fun onTracksChanged(tracks: androidx.media3.common.Tracks) { tracksRevision++ }
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                s.onPlayerError(error.message ?: "Erreur vidéo")
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }
    LaunchedEffect(url) {
        if (url != null) {
            if (player.currentMediaItem?.localConfiguration?.uri?.toString() != url) {
                player.setMediaItem(MediaItem.fromUri(url))
                player.prepare()
            }
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
                    Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> { settingsVisible = false; infoVisible = true; menuVisible = !menuVisible; selectedBouquet = initialBouquet; drawerTab = 1; true }
                    Key.Back, Key.Escape -> { if (settingsVisible) { settingsVisible = false; true } else if (menuVisible) { menuVisible = false; true } else false }
                    else -> false
                }
            }.focusRequester(focusRequester).focusTarget()
    ) {
        AndroidView(
            factory = { PlayerView(it).apply { this.player = player; useController = false; keepScreenOn = true; resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT } },
            update = { it.resizeMode = resizeMode },
            modifier = Modifier.fillMaxSize()
        )
        Box(Modifier.fillMaxSize().clickable {
            if (settingsVisible) settingsVisible = false
            else { menuVisible = !menuVisible; selectedBouquet = initialBouquet; drawerTab = 1; infoVisible = true }
        })
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
                Surface(color = Color(0xB2223230), shape = RoundedCornerShape(8.dp)) {
                    Text(quality, color = Color.White, fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp))
                }
                TextButton(onClick = { settingsVisible = true; infoVisible = true }) {
                    Text("⚙", color = Color.White, fontSize = 24.sp)
                }
            }
        }
        if (settingsVisible) {
            Box(Modifier.fillMaxSize().clickable { settingsVisible = false; infoVisible = false })
            val observedTracksRevision = tracksRevision
            val availableGroups = remember(observedTracksRevision) { player.currentTracks.groups }
            val audioGroups = availableGroups.filter { it.type == C.TRACK_TYPE_AUDIO }
            val textGroups = availableGroups.filter { it.type == C.TRACK_TYPE_TEXT }
            Surface(
                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(0.84f).fillMaxWidth(0.62f),
                color = Color(0xF0152119),
                shape = RoundedCornerShape(topStart = 18.dp, bottomStart = 18.dp)
            ) {
                Column(Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState()).padding(10.dp)) {
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
                        "Largeur fixe" to AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH
                    ).forEach { (name, value) ->
                        TextButton(onClick = { setDisplayMode(value) }) {
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
            Box(Modifier.fillMaxSize().clickable { menuVisible = false })
            Surface(
                modifier = Modifier.align(Alignment.CenterStart)
                    .fillMaxHeight(if (portrait) 0.72f else 0.74f)
                    .fillMaxWidth(if (portrait) 0.78f else 0.36f)
                    .widthIn(max = 350.dp),
                color = Color(0xE9192421),
                shape = RoundedCornerShape(topEnd = 18.dp, bottomEnd = 18.dp),
                shadowElevation = 14.dp
            ) {
                Column(Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Chaînes TV", color = Color.White,
                            fontWeight = FontWeight.Bold, fontSize = 15.sp,
                            modifier = Modifier.weight(1f))
                        TextButton(onClick = { menuVisible = false }) {
                            Text("✕", color = Color.White, fontSize = 18.sp)
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Bouquets" to 0, "Chaînes" to 1).forEach { (title, tab) ->
                            Surface(
                                modifier = Modifier.weight(1f).clickable { drawerTab = tab },
                                color = if (drawerTab == tab) Color(0xFFDF792D) else Color(0x553D5047),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(title, modifier = Modifier.padding(vertical = 7.dp),
                                    color = Color.White, fontWeight = FontWeight.SemiBold,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    maxLines = 1)
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        if (drawerTab == 0) "${bouquets.size} bouquets"
                        else "${drawerBouquet ?: "Toutes les chaînes"} · ${drawerChannels.size} chaînes",
                        color = Color(0xFFE2E5E2), fontSize = 12.sp, maxLines = 1
                    )
                    Spacer(Modifier.height(8.dp))
                    if (drawerTab == 0) {
                        LazyColumn(modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            items(bouquets) { name ->
                                Surface(
                                    modifier = Modifier.fillMaxWidth().clickable {
                                        selectedBouquet = name
                                        drawerTab = 1
                                    },
                                    color = if (name == drawerBouquet) Color(0x554D8B72) else Color.Transparent,
                                    shape = RoundedCornerShape(11.dp)
                                ) {
                                    Text(name, color = if (name == drawerBouquet) Color(0xFFFFC18C) else Color.White,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                                        maxLines = 2)
                                }
                            }
                        }
                    } else {
                        LazyColumn(modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            items(drawerChannels, key = { it.id + it.url }) { item ->
                                Row(
                                    Modifier.fillMaxWidth()
                                        .background(
                                            if (item.id == channel?.id) Color(0x554D8B72) else Color.Transparent,
                                            RoundedCornerShape(11.dp)
                                        )
                                        .clickable {
                                            s.selectPlaybackBouquet(drawerBouquet, drawerChannels)
                                            s.playLive(item)
                                            menuVisible = false
                                        }
                                        .padding(horizontal = 9.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    if (item.logo.startsWith("http")) {
                                        AsyncImage(model = item.logo, contentDescription = null,
                                            modifier = Modifier.size(36.dp), contentScale = ContentScale.Fit)
                                    }
                                    Text(item.name, color = if (item.id == channel?.id) Color(0xFFFFC18C) else Color.White,
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 2, modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
