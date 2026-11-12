package com.mediaplus.afrique

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.focus.onFocusChanged
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
    val field = RoundedCornerShape(17.dp)
    val f = Modifier.fillMaxWidth().padding(top = 6.dp)
    var showPassword by remember { mutableStateOf(false) }
    var rememberUsername by remember { mutableStateOf(true) }
    var showBiometricHelp by remember { mutableStateOf(false) }
    val host = LocalContext.current as? FragmentActivity
    val biometricReady = host != null && BiometricManager.from(host).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS

    var biometricRequested by remember { mutableStateOf(false) }
    fun requestBiometric() {
        val activity = host ?: return
        if (!biometricReady || !s.hasSavedBiometricAccount() || biometricRequested) return
        biometricRequested = true
        val prompt = BiometricPrompt(activity, ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    biometricRequested = false
                    s.connectWithBiometric()
                }
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    biometricRequested = false
                }
                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                }
            })
        prompt.authenticate(BiometricPrompt.PromptInfo.Builder()
            .setTitle("Media+ Côte d’Ivoire")
            .setSubtitle("Confirmez votre empreinte pour accéder au compte enregistré")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
            .setNegativeButtonText("Annuler")
            .build())
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= 700.dp
        Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.12f)))
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = if (wide) 48.dp else 18.dp, vertical = if (wide) 24.dp else 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(R.drawable.media_plus_brand),
                    contentDescription = "Media+ Côte d’Ivoire",
                    modifier = Modifier.size(if (wide) 96.dp else 78.dp).clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Fit
                )
                Text("🇨🇮", fontSize = 15.sp, modifier = Modifier.offset(x = (-15).dp, y = (-30).dp))
                Spacer(Modifier.width(6.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Media+", color = c.onSurface, fontSize = if (wide) 25.sp else 20.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
                        Spacer(Modifier.width(5.dp))
                        Text("Côte d’Ivoire", color = c.onSurface, fontSize = if (wide) 17.sp else 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    }
                    Text("DIRECT · FILMS · SÉRIES", color = c.secondary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }
            val appContext = LocalContext.current
            val installedVersion = remember(appContext) {
                try { appContext.packageManager.getPackageInfo(appContext.packageName, 0).versionName ?: "inconnue" }
                catch (_: Exception) { "inconnue" }
            }
            Text("Version $installedVersion", fontSize = 10.sp, color = c.onSurfaceVariant.copy(alpha = .7f))
            Spacer(Modifier.height(if (wide) 14.dp else 9.dp))
            Card(
                Modifier.fillMaxWidth().widthIn(max = 560.dp).animateContentSize()
                    .shadow(16.dp, RoundedCornerShape(26.dp), ambientColor = Color(0x223B4540), spotColor = Color(0x33475045)),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = .88f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = .95f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(Modifier.fillMaxWidth().padding(if (wide) 22.dp else 16.dp)) {
                    Text("Bienvenue", fontSize = 23.sp, fontWeight = FontWeight.Bold, color = c.onSurface, modifier = Modifier.align(Alignment.CenterHorizontally))
                    Text("Accédez à vos chaînes, films et séries.", color = c.onSurfaceVariant, fontSize = 13.sp, modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 6.dp, bottom = 18.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(onClick = { s.mode = 0 }, modifier = Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(25.dp), colors = ButtonDefaults.outlinedButtonColors(containerColor = if (s.mode == 0) Color(0xFFEDEDED) else Color.White)) { Text("Identifiants", color = c.onSurface, fontSize = 13.sp) }
                        OutlinedButton(onClick = { s.mode = 1 }, modifier = Modifier.weight(1f), colors = ButtonDefaults.outlinedButtonColors(containerColor = if (s.mode == 1) Color(0xFFEDEDED) else Color.White)) { Text("MAC", color = c.onSurface, fontSize = 13.sp) }
                    }
                    if (s.mode == 0) {
                        OutlinedTextField(s.user, { if (rememberUsername) s.saveUsername(it) else s.user = it }, f, singleLine = true, shape = field, label = { Text("Nom d'utilisateur") })
                        OutlinedTextField(s.pass, { s.pass = it }, f.onFocusChanged { if (it.isFocused && s.hasSavedBiometricAccount()) requestBiometric() }, singleLine = true, shape = field, label = { Text("Mot de passe") }, visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(), trailingIcon = { TextButton(onClick = { showPassword = !showPassword }) { Text(if (showPassword) "◉ Masquer" else "◉ Voir") } })
                        if (biometricReady) {
                            TextButton(onClick = {
                                if (s.hasSavedBiometricAccount()) requestBiometric()
                                else showBiometricHelp = !showBiometricHelp
                            }, modifier = Modifier.align(Alignment.End)) {
                                Text("◉  Connexion par empreinte", color = c.primary, fontSize = 13.sp)
                            }
                            if (showBiometricHelp && !s.hasSavedBiometricAccount()) {
                                Text("Connectez-vous une première fois avec votre mot de passe pour activer l’empreinte.",
                                    color = c.onSurfaceVariant, fontSize = 11.sp)
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
                            Checkbox(checked = s.rememberPassword, onCheckedChange = { s.rememberPassword = it })
                            Column(Modifier.weight(1f)) {
                                Text("Enregistrer mon mot de passe", fontSize = 12.sp, color = c.onSurface)
                                Text("Chiffré sur cet appareil · connexion automatique", fontSize = 10.sp, color = c.onSurfaceVariant)
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
                            Checkbox(checked = rememberUsername, onCheckedChange = { rememberUsername = it; if (it) s.saveUsername(s.user) })
                            Text("Mémoriser le nom d’utilisateur", fontSize = 12.sp, color = c.onSurfaceVariant)
                        }
                    } else {
                        OutlinedTextField(s.mac, { s.mac = it }, f, singleLine = true, shape = field, label = { Text("Adresse MAC") }, leadingIcon = { Text("▣") })
                        Text("Clé MAC : ${s.macDisplayKey()}", modifier = Modifier.padding(top = 12.dp), color = c.primary)
                        Text("Renouvellement tous les 30 jours", style = MaterialTheme.typography.labelSmall, color = c.onSurfaceVariant)
                    }
                    Button(
                        onClick = { s.connect() },
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp).height(if (wide) 52.dp else 48.dp),
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF1F2F1), contentColor = c.onSurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCDD1CD)),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 9.dp)
                    ) { Text("▶  Se connecter", fontSize = 15.sp, fontWeight = FontWeight.SemiBold) }
                    AnimatedVisibility(s.status.startsWith("Erreur", ignoreCase = true) || s.status.contains("indisponible", ignoreCase = true)) {
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
        val landscapeCompact = maxWidth > maxHeight && maxHeight < 560.dp
        val tvWide = maxWidth >= 900.dp && maxHeight >= 560.dp
        val useSideRail = landscapeCompact || tvWide
        Row(Modifier.fillMaxSize()) {
            if (useSideRail) {
                Column(Modifier.width(if (tvWide) 92.dp else 76.dp).fillMaxHeight().background(Color.White.copy(alpha = 0.60f))
                    .padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(0 to "Direct", 2 to "Films", 3 to "Séries", 4 to "Favoris", 5 to "Réglages").forEach { (section, title) ->
                        val active = if (section == 5) settingsOpen else !settingsOpen && s.section == section
                        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                            .background(if (active) Color.White.copy(alpha = 0.72f) else Color.Transparent)
                            .clickable {
                                if (section == 5) { onSettingsOpen(true); s.clearPreview() }
                                else { onSettingsOpen(false); onSettingsPage(null); s.pickSection(section) }
                            }.padding(vertical = 5.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(when (section) { 0 -> Icons.Filled.Home; 2 -> Icons.Filled.PlayArrow; 3 -> Icons.Filled.List; 4 -> Icons.Filled.Star; else -> Icons.Filled.Settings }, contentDescription = title, tint = if (active) c.secondary else c.primary, modifier = Modifier.size(23.dp))
                            Text(title, fontSize = 10.sp, color = c.onSurface, maxLines = 1)
                        }
                    }
                }
            }
            Column(Modifier.weight(1f).fillMaxHeight()
                .padding(horizontal = if (useSideRail) 12.dp else if (wide) 30.dp else 16.dp)) {
            Spacer(Modifier.height(if (landscapeCompact) 2.dp else 14.dp))
            Header(false, s.subscriptionExpirySeconds, compact = useSideRail) { }
            Spacer(Modifier.height(if (landscapeCompact) 1.dp else 5.dp))
            if (settingsOpen) {
                SettingsScreen(s, settingsPage, onSettingsPage, Modifier.weight(1f))
            } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) { Text(
                when (s.section) { 2 -> "Films"; 3 -> "Séries"; 4 -> "Mes favoris"; else -> "TV en direct" },
                color = c.onSurface,
                style = if (useSideRail) MaterialTheme.typography.titleMedium else if (wide) MaterialTheme.typography.headlineLarge else MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold
            ) }
            IconButton(onClick = { searchOpen = !searchOpen; if (!searchOpen) s.query = "" }) { Icon(if (searchOpen) Icons.Filled.Close else Icons.Filled.Search, contentDescription = "Rechercher", tint = c.primary, modifier = Modifier.size(25.dp)) }
            }
            if (searchOpen) OutlinedTextField(s.query, { s.query = it }, Modifier.fillMaxWidth(), singleLine = true, placeholder = { Text("Rechercher une chaîne ou un bouquet") })
            if (!useSideRail) Text("Vos chaînes et vos contenus", color = c.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(5.dp))
            if (s.group != null && s.section != 4) {
                TextButton({ s.group = null }) {
                    Text(if (landscapeCompact) "‹  Bouquets" else "‹  Retour aux catégories", color = c.secondary, fontWeight = FontWeight.SemiBold, fontSize = if (landscapeCompact) 12.sp else 14.sp)
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
            if (s.section == 0 && useSideRail) {
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.weight(if (s.previewChannelId != null) 0.64f else 1f).fillMaxHeight()) {
                        val visibleItems = if (s.query.isNotBlank()) {
                            list.filter { it.name.contains(s.query, ignoreCase = true) || it.group.contains(s.query, ignoreCase = true) }
                        } else if (s.group == null) emptyList()
                        else specialGroups[s.group] ?: list.filter { it.group == s.group }
                        if (s.group == null && s.query.isBlank()) {
                            val groups = (specialGroups.keys + list.map { it.group }).filter { it.isNotBlank() }.distinct()
                            LazyVerticalGrid(columns = GridCells.Adaptive(100.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(bottom = 12.dp)) {
                                items(groups) { groupName ->
                                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                                        .background(Color.White.copy(alpha = 0.72f))
                                        .clickable { s.requestGroup(groupName) }
                                        .padding(9.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(bouquetIcon(groupName), fontSize = 27.sp)
                                        Text(groupName, fontSize = 11.sp, maxLines = 2,
                                            color = c.onSurface, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        } else {
                            LazyVerticalGrid(columns = GridCells.Adaptive(94.dp),
                                horizontalArrangement = Arrangement.spacedBy(7.dp),
                                verticalArrangement = Arrangement.spacedBy(7.dp),
                                contentPadding = PaddingValues(bottom = 12.dp)) {
                                items(visibleItems) { item ->
                                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                                        .background(Color.White.copy(alpha = if (s.previewChannelId == item.id) 0.92f else 0.70f))
                                        .clickable {
                                            s.selectPlaybackBouquet(s.group, visibleItems)
                                            s.requestAdultItem(item) { s.pick(item) }
                                        }.padding(7.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally) {
                                        Logo(item)
                                        Spacer(Modifier.height(4.dp))
                                        Text(item.name, fontSize = 10.sp, color = c.onSurface,
                                            fontWeight = FontWeight.Medium, maxLines = 2)
                                    }
                                }
                            }
                        }
                    }
                    if (s.previewChannelId != null) {
                        Box(Modifier.weight(0.36f).fillMaxHeight()) {
                            LivePreviewPanel(s, compact = false)
                        }
                    }
                }
            } else if (s.section == 0 && wide) {
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(Modifier.weight(if (landscapeCompact) 0.58f else 0.54f).fillMaxHeight()) {
                        ItemList(list, if (s.section == 4) null else s.group, s.query, favUrls, s.section == 4,
                            { s.requestGroup(it) }, { item ->
                                if (s.section == 0) s.selectPlaybackBouquet(s.group, specialGroups[s.group] ?: list.filter { it.group == s.group })
                                s.requestAdultItem(item) { s.pick(item) }
                            }, { s.toggleFav(it) }, Modifier.fillMaxSize(), specialGroups = specialGroups, savedPositions = s.bouquetPositions)
                    }
                    if (s.previewChannelId != null) {
                        Box(Modifier.weight(if (landscapeCompact) 0.42f else 0.46f).fillMaxHeight()) {
                            LivePreviewPanel(s, compact = false)
                        }
                    }
                }
            } else {
                if (s.section == 0 && s.previewChannelId != null) LivePreviewPanel(s, compact = true)
                ItemList(list, if (s.section == 4) null else s.group, s.query, favUrls, s.section == 4,
                    { s.requestGroup(it) }, { item ->
                                if (s.section == 0) s.selectPlaybackBouquet(s.group, specialGroups[s.group] ?: list.filter { it.group == s.group })
                                s.requestAdultItem(item) { s.pick(item) }
                            }, { s.toggleFav(it) }, Modifier.weight(1f),
                    categories = when (s.section) { 2 -> s.movieCategories; 3 -> s.seriesCategories; else -> emptyList() },
                    newestFirst = (s.section == 2 || s.section == 3) && s.newestFirst,
                    specialGroups = specialGroups, savedPositions = s.bouquetPositions)
            }
            }
            Spacer(Modifier.height(2.dp))
            if (!useSideRail) NavigationBar(
                containerColor = Color.White,
                contentColor = c.onSurface,
                tonalElevation = 0.dp,
                modifier = Modifier.clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
            ) {
                listOf(0 to "Direct", 2 to "Films", 3 to "Séries", 4 to "Favoris", 5 to "Paramètres").forEach { (section, title) ->
                    NavigationBarItem(
                        selected = if (section == 5) settingsOpen else !settingsOpen && s.section == section,
                        onClick = { if (section == 5) { onSettingsOpen(true); s.clearPreview() } else { onSettingsOpen(false); onSettingsPage(null); s.pickSection(section) } },
                        icon = { Icon(when (section) { 0 -> Icons.Filled.Home; 2 -> Icons.Filled.PlayArrow; 3 -> Icons.Filled.List; 4 -> Icons.Filled.Star; else -> Icons.Filled.Settings }, contentDescription = title, modifier = Modifier.size(23.dp)) },
                        label = { Text(title, fontWeight = FontWeight.SemiBold, fontSize = 10.sp, maxLines = 1, softWrap = false) },
                        alwaysShowLabel = true,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = c.onSurface,
                            selectedTextColor = c.onSurface,
                            unselectedIconColor = Color(0xFF62666A),
                            unselectedTextColor = Color(0xFF62666A),
                            indicatorColor = Color(0xFFE9EAEC)
                        )
                    )
                }
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
