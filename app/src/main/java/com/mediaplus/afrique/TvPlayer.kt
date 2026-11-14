@file:OptIn(androidx.media3.common.util.UnstableApi::class)
package com.mediaplus.afrique

import android.content.Context
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
import androidx.compose.ui.focus.focusProperties
import androidx.compose.foundation.focusable
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
    var settingsButtonFocused by remember { mutableStateOf(false) }
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
    val drawerFocusRequester = remember { FocusRequester() }
    val infoSettingsFocusRequester = remember { FocusRequester() }
    LaunchedEffect(menuVisible, settingsVisible, infoVisible) {
        // Focus targets can disappear while a remote key is still being dispatched.
        // Never let an unattached FocusRequester crash the player.
        kotlinx.coroutines.yield()
        runCatching {
            when {
                settingsVisible -> Unit
                menuVisible -> drawerFocusRequester.requestFocus()
                else -> focusRequester.requestFocus()
            }
        }
    }
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
    Box(
        Modifier.fillMaxSize().background(Color.Black)
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) false else when (event.key) {
                    Key.DirectionUp, Key.DirectionDown -> {
                        if (menuVisible || settingsVisible || infoVisible) false
                        else {
                            val index = channels.indexOfFirst { it.id == channel?.id }
                            if (index >= 0 && channels.isNotEmpty()) {
                                val step = if (event.key == Key.DirectionUp) -1 else 1
                                val nextIndex = (index + step + channels.size) % channels.size
                                s.playLive(channels[nextIndex])
                                true
                            } else false
                        }
                    }
                    Key.DirectionLeft, Key.DirectionRight -> {
                        if (!menuVisible && !settingsVisible && infoVisible) {
                            runCatching { infoSettingsFocusRequester.requestFocus() }.isSuccess
                        } else false
                    }
                    Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                        // Child controls handle OK while a drawer, settings or the gear has focus.
                        if (menuVisible || settingsVisible || settingsButtonFocused) false
                        else {
                            infoVisible = true
                            menuVisible = true
                            selectedBouquet = initialBouquet
                            drawerTab = 1
                            true
                        }
                    }
                    Key.Back, Key.Escape -> {
                        when {
                            settingsVisible -> { settingsVisible = false; true }
                            menuVisible -> { menuVisible = false; true }
                            else -> false
                        }
                    }
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
                    .background(Color(0x99000000)).padding(horizontal = 22.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(channel?.name ?: "Direct", color = Color.White,
                        fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    Text(if (current != null) current.title else "EPG indisponible",
                        color = Color(0xFFE2E6E6), fontSize = 15.sp, maxLines = 2)
                }
                val logo = channel?.logo.orEmpty()
                if (logo.startsWith("http")) {
                    AsyncImage(model = logo, contentDescription = "Logo de la chaîne",
                        modifier = Modifier.size(width = 94.dp, height = 70.dp), contentScale = ContentScale.Fit)
                }
                Surface(color = Color(0xB2223230), shape = RoundedCornerShape(8.dp)) {
                    Text(quality, color = Color.White, fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp))
                }
                TextButton(
                    modifier = Modifier
                        .focusRequester(infoSettingsFocusRequester)
                        .onFocusChanged { settingsButtonFocused = it.isFocused }
                        .background(
                            if (settingsButtonFocused) Color(0xFF075D3A) else Color(0x99222E2A),
                            RoundedCornerShape(12.dp)
                        )
                        .border(
                            if (settingsButtonFocused) 3.dp else 1.dp,
                            if (settingsButtonFocused) Color(0xFF2CFF86) else Color(0x66FFFFFF),
                            RoundedCornerShape(12.dp)
                        ),
                    onClick = { menuVisible = false; settingsVisible = true; infoVisible = true }
                ) {
                    Icon(Icons.Filled.Settings, contentDescription = "Paramètres", tint = Color.White, modifier = Modifier.size(26.dp))
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
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 12.dp, top = 12.dp, bottom = 12.dp).fillMaxWidth(if (portrait) 0.72f else 0.32f).fillMaxHeight(0.92f),
                color = Color(0xC9112820),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(Modifier.padding(12.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text("Paramètres", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("Format de l'image", color = Color.White)
                    listOf(
                        "Adapter à l'écran" to AspectRatioFrameLayout.RESIZE_MODE_FIT,
                        "Remplir l'écran" to AspectRatioFrameLayout.RESIZE_MODE_ZOOM,
                        "Étirer" to AspectRatioFrameLayout.RESIZE_MODE_FILL
                    ).forEach { (label, mode) ->
                        var focused by remember { mutableStateOf(false) }
                        Text(
                            label + if (resizeMode == mode) " ✓" else "",
                            modifier = Modifier.fillMaxWidth()
                                .onFocusChanged { focused = it.isFocused }
                                .background(if (focused) Color(0xFF075D3A) else Color(0x663D5047), RoundedCornerShape(9.dp))
                                .border(if (focused) 2.dp else 0.dp, if (focused) Color(0xFF2CFF86) else Color.Transparent, RoundedCornerShape(9.dp))
                                .clickable { setDisplayMode(mode); settingsVisible = false }
                                .focusable()
                                .padding(12.dp),
                            color = Color.White
                        )
                    }
                    Text("Audio", color = Color.White, fontWeight = FontWeight.Bold)
                    if (audioGroups.isEmpty()) Text("Aucune piste audio détectée", color = Color.LightGray, fontSize = 12.sp)
                    audioGroups.forEachIndexed { groupIndex, group ->
                        for (trackIndex in 0 until group.length) {
                            if (group.isTrackSupported(trackIndex)) {
                                val format = group.getTrackFormat(trackIndex)
                                val label = format.label ?: format.language ?: "Audio ${trackIndex + 1}"
                                var focused by remember { mutableStateOf(false) }
                                Text("🔊 $label" + if (group.isTrackSelected(trackIndex)) " ✓" else "",
                                    modifier = Modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused }
                                        .background(if (focused) Color(0xFF075D3A) else Color(0x553D5047), RoundedCornerShape(8.dp))
                                        .clickable {
                                            player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
                                                .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, trackIndex))
                                                .build()
                                        }.focusable().padding(9.dp),
                                    color = Color.White, fontSize = 13.sp)
                            }
                        }
                    }
                    Text("Sous-titres", color = Color.White, fontWeight = FontWeight.Bold)
                    var subtitlesOffFocused by remember { mutableStateOf(false) }
                    Text("Désactivés",
                        modifier = Modifier.fillMaxWidth().onFocusChanged { subtitlesOffFocused = it.isFocused }
                            .background(if (subtitlesOffFocused) Color(0xFF075D3A) else Color(0x553D5047), RoundedCornerShape(8.dp))
                            .clickable { player.trackSelectionParameters = player.trackSelectionParameters.buildUpon().setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true).build() }
                            .focusable().padding(9.dp), color = Color.White, fontSize = 13.sp)
                    if (textGroups.isEmpty()) Text("Aucun sous-titre disponible", color = Color.LightGray, fontSize = 12.sp)
                    textGroups.forEach { group ->
                        for (trackIndex in 0 until group.length) {
                            if (group.isTrackSupported(trackIndex)) {
                                val format = group.getTrackFormat(trackIndex)
                                val label = format.label ?: format.language ?: "Sous-titres ${trackIndex + 1}"
                                var focused by remember { mutableStateOf(false) }
                                Text("▤ $label" + if (group.isTrackSelected(trackIndex)) " ✓" else "",
                                    modifier = Modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused }
                                        .background(if (focused) Color(0xFF075D3A) else Color(0x553D5047), RoundedCornerShape(8.dp))
                                        .clickable {
                                            player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
                                                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                                                .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, trackIndex)).build()
                                        }.focusable().padding(9.dp),
                                    color = Color.White, fontSize = 13.sp)
                            }
                        }
                    }
                    TextButton(onClick = { settingsVisible = false }) { Text("Fermer", color = Color.White) }
                }
            }
        }
        if (menuVisible && !settingsVisible) {
            Surface(
                modifier = Modifier.align(Alignment.TopStart).padding(start = 12.dp, top = 12.dp, bottom = 92.dp).fillMaxHeight().fillMaxWidth(if (portrait) 0.76f else 0.53f),
                color = Color(0xF0152119),
                shape = RoundedCornerShape(topStart = 18.dp, bottomStart = 18.dp)
            ) {
                Column(Modifier.padding(10.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Bouquets" to 0, "Chaînes" to 1).forEach { (title, tab) ->
                            Surface(
                                modifier = Modifier.weight(1f)
                                    .then(if (tab == 1) Modifier.focusRequester(drawerFocusRequester) else Modifier)
                                    .onFocusChanged { if (it.isFocused) drawerTab = tab }
                                    .clickable { drawerTab = tab }.focusable(),
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
                                var rowFocused by remember { mutableStateOf(false) }
                                Surface(
                                    modifier = Modifier.fillMaxWidth()
                                        .onFocusChanged { rowFocused = it.isFocused }
                                        .border(if (rowFocused) 2.dp else 0.dp, if (rowFocused) Color(0xFF2CFF86) else Color.Transparent, RoundedCornerShape(11.dp))
                                        .clickable {
                                        selectedBouquet = name
                                        drawerTab = 1
                                    }.focusable(),
                                    color = if (rowFocused) Color(0xFF075D3A) else if (name == drawerBouquet) Color(0x554D8B72) else Color.Transparent,
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
                                var rowFocused by remember { mutableStateOf(false) }
                                Row(
                                    Modifier.fillMaxWidth()
                                        .onFocusChanged { rowFocused = it.isFocused }
                                        .background(
                                            if (rowFocused) Color(0xFF075D3A) else if (item.id == channel?.id) Color(0x554D8B72) else Color.Transparent,
                                            RoundedCornerShape(11.dp)
                                        )
                                        .border(if (rowFocused) 2.dp else 0.dp, if (rowFocused) Color(0xFF2CFF86) else Color.Transparent, RoundedCornerShape(11.dp))
                                        .clickable {
                                            s.selectPlaybackBouquet(drawerBouquet, drawerChannels)
                                            s.playLive(item)
                                            menuVisible = false
                                        }.focusable()
                                        .padding(horizontal = 9.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    if (item.logo.startsWith("http")) {
                                        AsyncImage(model = item.logo, contentDescription = null,
                                            modifier = Modifier.size(36.dp), contentScale = ContentScale.Fit)
                                    }
                                    Text(item.name, color = if (rowFocused) Color.White else if (item.id == channel?.id) Color(0xFFFFC18C) else Color.White,
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
