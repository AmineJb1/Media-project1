package com.mediaplus.afrique

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LoginScreen(s: AppState) {
    val c = MaterialTheme.colorScheme
    val field = RoundedCornerShape(16.dp)
    val f = Modifier.fillMaxWidth().padding(top = 10.dp)
    var showPassword by remember { mutableStateOf(false) }
    val host = LocalContext.current as? FragmentActivity
    val biometricReady = host != null && BiometricManager.from(host).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= 700.dp
        Box(
            Modifier.fillMaxSize().background(
                Brush.radialGradient(
                    listOf(Color(0xFFE7F5EB), Color.White, Color(0xFFFFF9F1)),
                    radius = 1100f
                )
            )
        )
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = if (wide) 48.dp else 22.dp, vertical = if (wide) 32.dp else 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(R.drawable.media_plus_brand),
                    contentDescription = "Media+ Afrique",
                    modifier = Modifier.size(if (wide) 82.dp else 64.dp).clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Fit
                )
                Spacer(Modifier.width(14.dp))
                Column {
                    Text("Media+ Afrique", color = c.onSurface, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                    Text("🇹🇳 🇩🇿 🇲🇦 🇪🇬 🇸🇦 🇦🇪", color = c.secondary, style = MaterialTheme.typography.bodyMedium)
                    Text("DIRECT · FILMS · SÉRIES", color = c.secondary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(if (wide) 20.dp else 14.dp))
            Card(
                Modifier.fillMaxWidth().widthIn(max = 560.dp).animateContentSize(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, c.outline),
                elevation = CardDefaults.cardElevation(defaultElevation = 14.dp)
            ) {
                Column(Modifier.fillMaxWidth().padding(if (wide) 30.dp else 20.dp)) {
                    Text("Bienvenue", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = c.onSurface)
                    Text("Accédez à vos chaînes, films et séries.", color = c.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 5.dp, bottom = 20.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Pill("Identifiants", s.mode == 0, Modifier.weight(1f)) { s.mode = 0 }
                        Pill("Adresse MAC", s.mode == 1, Modifier.weight(1f)) { s.mode = 1 }
                    }
                    if (s.mode == 0) {
                        OutlinedTextField(s.user, { s.saveUsername(it) }, f, singleLine = true, shape = field, label = { Text("Nom d'utilisateur") })
                        OutlinedTextField(s.pass, { s.pass = it }, f, singleLine = true, shape = field, label = { Text("Mot de passe") }, visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(), trailingIcon = { TextButton(onClick = { showPassword = !showPassword }) { Text(if (showPassword) "Masquer" else "Voir") } })
                    } else {
                        OutlinedTextField(s.mac, { s.mac = it }, f, singleLine = true, shape = field, label = { Text("Adresse MAC") }, leadingIcon = { Text("▣") })
                        Text("Clé MAC : ${s.macDisplayKey()} · Renouvellement tous les 30 jours", modifier = Modifier.padding(top = 12.dp), color = c.primary)
                    }
                    Button(
                        onClick = { s.connect() },
                        modifier = Modifier.fillMaxWidth().padding(top = 18.dp).height(if (wide) 60.dp else 54.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = c.primary)
                    ) { Text("Se connecter", fontWeight = FontWeight.Bold) }
                    if (s.mode == 0 && biometricReady && s.user.isNotBlank() && s.pass.isNotBlank()) {
                        OutlinedButton(onClick = {
                            val activity = host ?: return@OutlinedButton
                            val prompt = BiometricPrompt(activity, ContextCompat.getMainExecutor(activity),
                                object : BiometricPrompt.AuthenticationCallback() {
                                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                                        super.onAuthenticationSucceeded(result)
                                        s.connect()
                                    }
                                })
                            val info = BiometricPrompt.PromptInfo.Builder()
                                .setTitle("Media+ Afrique")
                                .setSubtitle("Connexion par empreinte")
                                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                                .setNegativeButtonText("Annuler")
                                .build()
                            prompt.authenticate(info)
                        }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                            Text("Connexion par empreinte")
                        }
                    }
                    AnimatedVisibility(s.status.isNotEmpty()) {
                        Text(s.status, Modifier.padding(top = 12.dp), color = c.secondary, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            Spacer(Modifier.height(18.dp))
        }
    }
}

@Composable
fun MainScreen(s: AppState, settingsOpen: Boolean, settingsPage: String?, onSettingsOpen: (Boolean) -> Unit, onSettingsPage: (String?) -> Unit) {
    val c = MaterialTheme.colorScheme
    val list = s.current()
    val favUrls = s.favs.map { it.url }.toSet()
    var searchOpen by remember { mutableStateOf(false) }
    val recent = s.recentlyWatched.mapNotNull { id -> s.live.firstOrNull { it.id == id } }
    // Exact channel names supplied by the user; no fabricated streams or URLs.
    val ivoryCoastNames = remember {
        setOf(
            "AF - LIFE TV", "AF - RENOUVEAU", "AF - IVOIRE MUSIC TV", "AF - A+ IVOIRE",
            "COITED - NOVELA", "COITED - RTI1", "COITED - LA 3 HD", "COITED - RTI2",
            "COITED - NCI", "COITED - LMTV", "COITED - NTV COTE D'IVOIRE",
            "COITED - ALBAYANE", "COITED - A+", "COITED - BENIE TV"
        )
    }
    val ivoryCoastChannels = remember(s.live.toList()) {
        s.live.filter { it.name.trim().uppercase(Locale.ROOT) in ivoryCoastNames }
    }
    val specialGroups = if (s.section == 0) buildMap<String, List<Item>> {
        if (recent.isNotEmpty()) put("🕘 Récemment regardées", recent)
        if (ivoryCoastChannels.isNotEmpty()) put("🇨🇮 Côte d’Ivoire", ivoryCoastChannels)
    } else emptyMap()
    if (s.pendingAdultGroup != null || s.pendingAdultItem != null) {
        ParentalGate(s)
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= 700.dp
        Column(Modifier.fillMaxSize().padding(horizontal = if (wide) 30.dp else 16.dp)) {
            Spacer(Modifier.height(14.dp))
            Header(false, s.subscriptionExpirySeconds) { }
            Spacer(Modifier.height(5.dp))
            if (settingsOpen) {
                SettingsScreen(s, settingsPage, onSettingsPage, Modifier.weight(1f))
            } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) { Text(
                when (s.section) { 2 -> "Films"; 3 -> "Séries"; else -> "TV en direct" },
                color = c.onSurface,
                style = if (wide) MaterialTheme.typography.headlineLarge else MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold
            ) }
            IconButton(onClick = { searchOpen = !searchOpen; if (!searchOpen) s.query = "" }) { Icon(if (searchOpen) Icons.Filled.Close else Icons.Filled.Search, contentDescription = "Rechercher", tint = c.primary, modifier = Modifier.size(25.dp)) }
            }
            if (searchOpen) OutlinedTextField(s.query, { s.query = it }, Modifier.fillMaxWidth(), singleLine = true, placeholder = { Text("Rechercher une chaîne ou un bouquet") })
            Text("Vos chaînes et vos contenus", color = c.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(5.dp))
            if (s.group != null) {
                TextButton({ s.group = null }) {
                    Text("‹  Retour aux catégories", color = c.secondary, fontWeight = FontWeight.SemiBold)
                }
            }
            if (s.status.isNotEmpty()) {
                Text(s.status, color = c.secondary, style = MaterialTheme.typography.bodySmall)
            }
            if (s.section == 2 || s.section == 3) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { s.refreshCatalog() }) { Text("↻ Actualiser") }
                    TextButton(onClick = { s.newestFirst = !s.newestFirst }) {
                        Text(if (s.newestFirst) "Date ↓" else "Ordre serveur", color = c.onSurface)
                    }
                }
                Text("Tri par date disponible selon les informations du serveur", color = c.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall)
            }
            if (s.section == 0 && wide) {
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(Modifier.weight(0.54f).fillMaxHeight()) {
                        ItemList(list, s.group, s.query, favUrls, false,
                            { s.requestGroup(it) }, { item ->
                                if (s.section == 0) s.selectPlaybackBouquet(s.group, specialGroups[s.group] ?: list.filter { it.group == s.group })
                                s.requestAdultItem(item) { s.pick(item) }
                            }, { s.toggleFav(it) }, Modifier.fillMaxSize(), specialGroups = specialGroups, savedPositions = s.bouquetPositions)
                    }
                    if (s.previewChannelId != null) {
                        Box(Modifier.weight(0.46f).fillMaxHeight()) {
                            LivePreviewPanel(s, compact = false)
                        }
                    }
                }
            } else {
                if (s.section == 0 && s.previewChannelId != null) LivePreviewPanel(s, compact = true)
                ItemList(list, s.group, s.query, favUrls, false,
                    { s.requestGroup(it) }, { item ->
                                if (s.section == 0) s.selectPlaybackBouquet(s.group, specialGroups[s.group] ?: list.filter { it.group == s.group })
                                s.requestAdultItem(item) { s.pick(item) }
                            }, { s.toggleFav(it) }, Modifier.weight(1f),
                    categories = when (s.section) { 2 -> s.movieCategories; 3 -> s.seriesCategories; else -> emptyList() },
                    newestFirst = (s.section == 2 || s.section == 3) && s.newestFirst,
                    specialGroups = specialGroups, savedPositions = s.bouquetPositions)
            }
            }
            Spacer(Modifier.height(5.dp))
            Spacer(Modifier.height(4.dp))
            NavigationBar(
                containerColor = Color.White,
                contentColor = c.onSurface,
                tonalElevation = 0.dp,
                modifier = Modifier.clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
            ) {
                listOf(0 to "Direct", 2 to "Films", 3 to "Séries", 5 to "Paramètres").forEach { (section, title) ->
                    NavigationBarItem(
                        selected = if (section == 5) settingsOpen else !settingsOpen && s.section == section,
                        onClick = { if (section == 5) { onSettingsOpen(true); s.clearPreview() } else { onSettingsOpen(false); onSettingsPage(null); s.pickSection(section) } },
                        icon = { if (section == 5) Icon(Icons.Filled.Settings, contentDescription = "Paramètres", modifier = Modifier.size(23.dp)) else Text(when (section) { 0 -> "◉"; 2 -> "▶"; else -> "▣" }, fontSize = 20.sp) },
                        label = { Text(title, fontWeight = FontWeight.SemiBold, fontSize = 10.sp, maxLines = 1, softWrap = false) },
                        alwaysShowLabel = true,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = c.primary,
                            selectedTextColor = c.primary,
                            unselectedIconColor = Color(0xFF788078),
                            unselectedTextColor = Color(0xFF788078),
                            indicatorColor = Color(0xFFE5F2E8)
                        )
                    )
                }
            }
        }
    }
}


@Composable
private fun LivePreviewPanel(s: AppState, compact: Boolean) {
    val c = MaterialTheme.colorScheme
    val selected = s.live.firstOrNull { it.id == s.previewChannelId }
    var programs by remember(selected?.id) { mutableStateOf<List<Prog>>(emptyList()) }
    var resolution by remember(selected?.id) { mutableStateOf("—") }
    LaunchedEffect(selected?.id, s.mode) {
        programs = emptyList()
        if (selected != null && s.mode == 0) {
            programs = runCatching { s.epg(selected.id) }.getOrDefault(emptyList())
        }
    }
    val now = System.currentTimeMillis()
    val currentProgram = programs.firstOrNull { now in it.start until it.end }
    val nextProgram = programs.firstOrNull { it.start >= (currentProgram?.end ?: now) }
    val fmt = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val epgText = if (currentProgram != null) "${fmt.format(Date(currentProgram.start))} ${currentProgram.title}" else "EPG indisponible"
    val nextText = nextProgram?.let { "Après : ${it.title}" } ?: ""

    if (selected != null) {
        if (compact) {
            // Mini-player horizontal: no large card, captions or extra fullscreen button.
            Row(
                Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF1F5F1)).padding(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    Modifier.weight(0.44f).aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(9.dp)).background(Color.Black)
                        .clickable(enabled = s.previewUrl != null) { s.playLive(selected) },
                    contentAlignment = Alignment.Center
                ) {
                    val url = s.previewUrl
                    if (url != null) VideoPlayer(url, onError = { s.onPlayerError(it) },
                        modifier = Modifier.fillMaxSize(), showControls = false,
                        onVideoResolution = { w, h -> resolution = if (w > 0 && h > 0) "$w × $h" else "—" })
                    else Text("Chargement…", color = Color.White,
                        style = MaterialTheme.typography.labelSmall)
                }
                Column(Modifier.weight(0.56f)) {
                    Text(s.previewChannelName, style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold, color = c.onSurface,
                        maxLines = 2)
                    Text("En direct · $resolution", style = MaterialTheme.typography.labelSmall,
                        color = c.primary)
                    Text(epgText, style = MaterialTheme.typography.labelSmall, maxLines = 2, color = c.onSurfaceVariant)
                    if (nextText.isNotEmpty()) Text(nextText, style = MaterialTheme.typography.labelSmall, maxLines = 1, color = c.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        TextButton(onClick = { s.playLive(selected) },
                            enabled = s.previewUrl != null,
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)) {
                            Text("⛶", style = MaterialTheme.typography.titleMedium)
                        }
                        TextButton(onClick = { s.clearPreview() },
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)) {
                            Text("✕", color = c.onSurfaceVariant)
                        }
                    }
                }
            }
        } else {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                Column(Modifier.padding(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(s.previewChannelName, Modifier.weight(1f),
                            style = MaterialTheme.typography.titleSmall)
                        TextButton(onClick = { s.clearPreview() }) { Text("Fermer") }
                    }
                    Text("$epgText · $resolution", style = MaterialTheme.typography.labelSmall)
                    Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(10.dp)).background(Color.Black)
                        .clickable(enabled = s.previewUrl != null) { s.playLive(selected) }) {
                        val url = s.previewUrl
                        if (url != null) VideoPlayer(url, onError = { s.onPlayerError(it) },
                            modifier = Modifier.fillMaxSize(), showControls = false,
                        onVideoResolution = { w, h -> resolution = if (w > 0 && h > 0) "$w × $h" else "—" })
                    }
                }
            }
        }
    }
}
