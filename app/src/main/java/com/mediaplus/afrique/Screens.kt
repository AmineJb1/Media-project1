package com.mediaplus.afrique

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.ui.text.style.TextOverflow
import coil.compose.AsyncImage
import androidx.compose.material3.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.text.style.TextAlign
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import android.content.res.Configuration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
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
    val f = Modifier.fillMaxWidth().padding(top = 8.dp)
    var showPassword by remember { mutableStateOf(false) }
    var showMacAccess by remember { mutableStateOf(false) }
    val initialButtonFocus = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val isTv = (LocalContext.current.resources.configuration.uiMode and Configuration.UI_MODE_TYPE_MASK) == Configuration.UI_MODE_TYPE_TELEVISION
    val clipboard = LocalClipboardManager.current
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
        val tvLayout = isTv || (maxWidth >= 800.dp && maxWidth > maxHeight)
        LaunchedEffect(tvLayout) {
            if (tvLayout) {
                initialButtonFocus.requestFocus()
                keyboardController?.hide()
            }
        }
        Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.12f)))
        // Constrain the parent before using fillMaxWidth inside the login form.
        Box(
            Modifier.fillMaxSize().padding(
                horizontal = if (tvLayout) 32.dp else 18.dp,
                vertical = if (tvLayout) 28.dp else 16.dp
            ),
            contentAlignment = Alignment.TopCenter
        ) {
            if (tvLayout) {
                Column(
                    Modifier.align(Alignment.BottomStart).offset(x = (-42).dp).padding(bottom = 34.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(R.drawable.media_plus_brand),
                            contentDescription = "Media+ Afrique",
                            modifier = Modifier.size(205.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
                val tvAppContext = LocalContext.current
                val tvVersion = remember(tvAppContext) {
                    try { tvAppContext.packageManager.getPackageInfo(tvAppContext.packageName, 0).versionName ?: "2.3.12" }
                    catch (_: Exception) { "2.3.12" }
                }
                Text("Version $tvVersion",
                    color = c.onSurfaceVariant.copy(alpha = .9f),
                    fontSize = 12.sp,
                    modifier = Modifier.align(Alignment.BottomStart).padding(start = 12.dp, bottom = 8.dp))
            }
            Column(
                Modifier.align(if (tvLayout) Alignment.CenterEnd else Alignment.TopCenter)
                    .widthIn(max = if (tvLayout) 450.dp else 560.dp)
                    .fillMaxWidth()
                    .padding(top = if (tvLayout) 0.dp else 0.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
            if (!tvLayout) {
            Row(Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(R.drawable.media_plus_brand),
                    contentDescription = "Media+ Côte d’Ivoire",
                    modifier = Modifier.size(if (tvLayout) 80.dp else if (wide) 88.dp else 78.dp).clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Fit
                )
                Spacer(Modifier.width(6.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Media+", color = c.onSurface, fontSize = if (tvLayout) 30.sp else if (wide) 25.sp else 20.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
                        Spacer(Modifier.width(5.dp))
                        Text("Côte d’Ivoire", color = c.onSurface, fontSize = if (tvLayout) 19.sp else if (wide) 17.sp else 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    }
                    Text("DIRECT · FILMS · SÉRIES", color = c.secondary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }
            val appContext = LocalContext.current
            val installedVersion = remember(appContext) {
                try { appContext.packageManager.getPackageInfo(appContext.packageName, 0).versionName ?: "inconnue" }
                catch (_: Exception) { "inconnue" }
            }
            Text("Version $installedVersion", fontSize = if (tvLayout) 12.sp else 10.sp,
                color = c.onSurfaceVariant.copy(alpha = .85f),
                modifier = Modifier.fillMaxWidth())
            }
            Spacer(Modifier.height(if (tvLayout) 8.dp else if (wide) 14.dp else 9.dp))
            Card(
                Modifier.fillMaxWidth().animateContentSize()
                    ,
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = if (tvLayout) Color.Transparent else Color.White.copy(alpha = .88f)),
                border = androidx.compose.foundation.BorderStroke(if (tvLayout) 0.dp else 1.dp, if (tvLayout) Color.Transparent else Color.White.copy(alpha = .95f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(Modifier.fillMaxWidth().padding(if (tvLayout) 12.dp else if (wide) 22.dp else 16.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                        Text("Media+", fontSize = if (tvLayout) 32.sp else 26.sp,
                            fontWeight = FontWeight.ExtraBold, color = Color.Black, maxLines = 1)
                        Text("Côte d’Ivoire", fontSize = if (tvLayout) 21.sp else 18.sp,
                            fontWeight = FontWeight.Bold, color = Color(0xFF124D37), maxLines = 1)
                        Row(Modifier.padding(top = 5.dp).width(if (tvLayout) 190.dp else 150.dp).height(6.dp).clip(RoundedCornerShape(4.dp))) {
                            Box(Modifier.weight(1f).fillMaxHeight().background(Color(0xFFFF8200)))
                            Box(Modifier.weight(1f).fillMaxHeight().background(Color.White).border(.5.dp, Color(0xFFDADADA)))
                            Box(Modifier.weight(1f).fillMaxHeight().background(Color(0xFF009A49)))
                        }
                    }
                    Text("Accédez à vos chaînes, films et séries.", color = c.onSurfaceVariant, fontSize = if (tvLayout) 15.sp else 13.sp, modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 6.dp, bottom = 18.dp))
                    Text("Identifiants", color = Color(0xFF0B4F32), fontSize = if (tvLayout) 20.sp else 15.sp,
                        fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 4.dp))
                    OutlinedTextField(s.user, { s.saveUsername(it) }, f.widthIn(max = if (tvLayout) 380.dp else 560.dp).align(Alignment.CenterHorizontally), singleLine = true,
                        shape = field, label = { Text("Nom d'utilisateur") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF0B4F32)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF0B4F32),
                            unfocusedBorderColor = Color(0xFF739E89),
                            focusedLabelColor = Color(0xFF71311E)))
                    Row(Modifier.fillMaxWidth().widthIn(max = if (tvLayout) 380.dp else 620.dp)
                        .align(Alignment.CenterHorizontally).padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(s.pass, { s.pass = it },
                            Modifier.weight(1f).onFocusChanged {
                                if (it.isFocused && s.hasSavedBiometricAccount()) requestBiometric()
                            },
                            singleLine = true, shape = field, label = { Text("Mot de passe") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF0B4F32)) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF0B4F32), unfocusedBorderColor = Color(0xFF739E89)),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation())
                        OutlinedButton(onClick = { showPassword = !showPassword },
                            modifier = Modifier.size(50.dp),
                            shape = RoundedCornerShape(15.dp),
                            contentPadding = PaddingValues(0.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (showPassword) Color(0xFF0B4F32) else Color.White.copy(alpha = .85f),
                                contentColor = if (showPassword) Color(0xFFFFA02A) else Color(0xFF0B4F32)
                            ),
                            border = BorderStroke(2.dp, if (showPassword) Color(0xFF42D77B) else Color(0xFF0B4F32))) {
                            Icon(
                                imageVector = if (showPassword) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                contentDescription = if (showPassword) "Masquer le mot de passe" else "Afficher le mot de passe",
                                modifier = Modifier.size(25.dp)
                            )
                        }
                    }
                    AnimatedVisibility(s.mode == 0 && (s.status.startsWith("Erreur", ignoreCase = true) || s.status == "Abonnement Expiré")) {
                        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = Color(0xFFD32F2F), modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(s.status, color = Color(0xFFD32F2F), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Row(Modifier.align(Alignment.CenterHorizontally).padding(top = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = s.rememberPassword, onCheckedChange = { s.rememberPassword = it })
                        Text("Enregistrer mon mot de passe sur cet appareil", fontSize = 12.sp, color = Color(0xFF173E33))
                    }
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
                    Button(
                        onClick = { s.mode = 0; s.connect() },
                        modifier = Modifier.fillMaxWidth(if (tvLayout) .82f else .90f).align(Alignment.CenterHorizontally).focusRequester(initialButtonFocus).padding(top = 8.dp).height(if (tvLayout) 44.dp else if (wide) 52.dp else 48.dp),
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0B4F32), contentColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCDD1CD)),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp, pressedElevation = 1.dp)
                    ) { Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color(0xFF0B4F32))
                        Spacer(Modifier.width(8.dp))
                        Text("Se connecter", fontSize = 15.sp, color = Color.White, fontWeight = FontWeight.Bold) }
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = { showMacAccess = true },
                        modifier = Modifier.fillMaxWidth(if (tvLayout) .82f else .90f).align(Alignment.CenterHorizontally).height(if (tvLayout) 40.dp else 44.dp),
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF8A00), contentColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCDD1CD)),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp, pressedElevation = 1.dp)
                    ) { Icon(Icons.Default.Home, contentDescription = null, tint = Color(0xFF0B4F32))
                        Spacer(Modifier.width(8.dp))
                        Text("Connexion avec Adresse MAC", fontSize = if (tvLayout) 13.sp else 14.sp, color = Color.White, fontWeight = FontWeight.SemiBold) }

                }
            }
            Spacer(Modifier.height(18.dp))
            }
        }
    }
    if (showMacAccess) {
        Dialog(onDismissRequest = { showMacAccess = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Surface(
                modifier = Modifier.fillMaxWidth(.92f).widthIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
                shape = RoundedCornerShape(24.dp),
                color = Color.White,
                shadowElevation = 16.dp
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Connexion avec Adresse MAC",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold, color = c.onSurface)
                        IconButton(onClick = { showMacAccess = false }) {
                            Icon(Icons.Filled.Close, contentDescription = "Fermer")
                        }
                    }
                    Text("Identifiant MAC de cette installation", color = c.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(s.mac, modifier = Modifier.weight(1f),
                            color = c.onSurface, fontWeight = FontWeight.SemiBold)
                        TextButton(onClick = { clipboard.setText(AnnotatedString(s.mac)) }) {
                            Text("Copier MAC")
                        }
                    }
                    HorizontalDivider()
                    Text("Clé d’application · renouvelée tous les 30 jours",
                        color = c.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(s.macDisplayKey(), modifier = Modifier.weight(1f),
                            color = c.primary, fontWeight = FontWeight.Bold)
                        TextButton(onClick = { clipboard.setText(AnnotatedString(s.macDisplayKey())) }) {
                            Text("Copier clé")
                        }
                    }
                    Button(onClick = { s.mode = 1; s.connect() },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(24.dp)) {
                        Text("Récupérer Ma Liste", fontWeight = FontWeight.SemiBold)
                    }
                    if (s.mode == 1 && (s.status == "Erreur de récupération" || s.status == "Abonnement Expiré")) {
                        Text(s.status, color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium)
                    }
                    Text("L’accès nécessite une autorisation active du serveur IPTV.",
                        color = c.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }

}

@Composable
fun MainScreen(s: AppState, settingsOpen: Boolean, settingsPage: String?, onSettingsOpen: (Boolean) -> Unit, onSettingsPage: (String?) -> Unit) {
    val c = MaterialTheme.colorScheme
    val list = s.current()
    if (s.selectedVod != null) VodDetailsPage(s)
    val favUrls = s.favs.map { it.url }.toSet()
    var searchOpen by remember { mutableStateOf(false) }
    var lastBouquet by remember { mutableStateOf<String?>(null) }
    val restoreBouquetFocus = remember { FocusRequester() }
    val bouquetGridState = rememberLazyGridState()
    var restoreBouquetPending by remember { mutableStateOf(false) }
    LaunchedEffect(s.group, s.section) {
        if (s.group != null && s.section == 0) restoreBouquetPending = true
    }
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
                Column(Modifier.width(if (tvWide) 188.dp else 154.dp).fillMaxHeight()
                    .background(Color(0xFFF8FAFC))
                    .border(1.dp, Color(0xFFE0E8EC))
                    .padding(horizontal = 7.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Image(
                        painter = painterResource(R.drawable.media_plus_brand),
                        contentDescription = "Media+ Afrique",
                        modifier = Modifier.fillMaxWidth().height(if (tvWide) 178.dp else 128.dp),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(Modifier.height(4.dp))
                    var focusedNavSection by remember { mutableStateOf<Int?>(null) }
                    listOf(0 to "Direct", 2 to "Films", 3 to "Séries", 4 to "Favoris", 5 to "Réglages").forEach { (section, title) ->
                        val active = if (section == 5) settingsOpen else !settingsOpen && s.section == section
                        var navFocused by remember { mutableStateOf(false) }
                        val selected = if (focusedNavSection != null) focusedNavSection == section else active
                        val bottleGreen = Color(0xFF0B4F32)
                        Row(Modifier.fillMaxWidth()
                            .shadow(if (selected) 6.dp else 3.dp, RoundedCornerShape(15.dp))
                            .clip(RoundedCornerShape(15.dp))
                            .onFocusChanged {
                                navFocused = it.isFocused
                                if (it.isFocused) focusedNavSection = section
                                else if (focusedNavSection == section) focusedNavSection = null
                            }
                            .background(if (selected) bottleGreen else Color(0xFFFF8A00))
                            .border(if (selected) 2.5.dp else 1.dp,
                                if (selected) Color(0xFF42D77B) else Color(0xFFE87500),
                                RoundedCornerShape(15.dp))
                            .clickable {
                                if (section == 5) { onSettingsOpen(true); s.clearPreview() }
                                else { onSettingsOpen(false); onSettingsPage(null); s.pickSection(section) }
                            }
                            .padding(horizontal = 8.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(38.dp).clip(RoundedCornerShape(11.dp))
                                .background(if (selected) Color(0xFF126B42) else Color(0xFFFFA533)),
                                contentAlignment = Alignment.Center) {
                                Icon(when (section) {
                                        0 -> Icons.Filled.Home
                                        2 -> Icons.Filled.PlayArrow
                                        3 -> Icons.Filled.List
                                        4 -> Icons.Filled.Star
                                        else -> Icons.Filled.Settings
                                    }, contentDescription = title,
                                        tint = if (selected) Color(0xFFFF8A00) else bottleGreen,
                                        modifier = Modifier.size(26.dp))
                            }
                            Spacer(Modifier.width(7.dp))
                            Text(title, fontSize = if (tvWide) 14.sp else 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1)
                        }
                    }
                }
            }
            Column(Modifier.weight(1f).fillMaxHeight()
                .padding(start = if (useSideRail) 14.dp else if (wide) 30.dp else 16.dp,
                    end = if (useSideRail) 34.dp else if (wide) 30.dp else 16.dp)) {
            Spacer(Modifier.height(if (landscapeCompact) 2.dp else 14.dp))
            Header(false, s.subscriptionExpirySeconds, compact = useSideRail) { }
            Spacer(Modifier.height(if (landscapeCompact) 1.dp else 5.dp))
            if (settingsOpen) {
                SettingsScreen(s, settingsPage, onSettingsPage, Modifier.weight(1f))
            } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) { if (s.section != 0) Text(
                when (s.section) { 2 -> "Films"; 3 -> "Séries"; 4 -> "Mes favoris"; else -> "" },
                color = c.onSurface,
                style = if (useSideRail) MaterialTheme.typography.titleMedium else if (wide) MaterialTheme.typography.headlineLarge else MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold
            ) }
            Surface(shape = RoundedCornerShape(16.dp), color = if (searchOpen) Color(0xFF0B4F32) else Color.White,
                border = BorderStroke(2.dp, if (searchOpen) Color(0xFF42D77B) else Color(0xFF0B4F32)), shadowElevation = 5.dp) {
                IconButton(onClick = { searchOpen = !searchOpen; if (!searchOpen) s.query = "" }, modifier = Modifier.size(48.dp)) {
                    Icon(if (searchOpen) Icons.Filled.Close else Icons.Filled.Search,
                        contentDescription = if (searchOpen) "Fermer la recherche" else "Rechercher",
                        tint = if (searchOpen) Color(0xFFFFA02A) else Color(0xFF0B4F32), modifier = Modifier.size(30.dp))
                }
            }
            }
            if (searchOpen) OutlinedTextField(
                value = s.query,
                onValueChange = { s.query = it },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    .shadow(5.dp, RoundedCornerShape(16.dp))
                    .background(Color.White, RoundedCornerShape(16.dp)),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = Color(0xFF0B4F32)) },
                placeholder = { Text("Rechercher une chaîne ou un bouquet", color = Color(0xFF536A60)) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color(0xFF173D2E),
                    unfocusedTextColor = Color(0xFF173D2E),
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = Color(0xFF0B4F32),
                    unfocusedBorderColor = Color(0xFF8FAFA1),
                    cursorColor = Color(0xFF0B4F32)
                )
            )
            if (!useSideRail) Text("Vos chaînes et vos contenus", color = c.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(5.dp))
            if (s.group != null && s.section != 4) {
                TextButton({ s.group = null }) {
                    Text(if (landscapeCompact) "‹  Bouquets" else "‹  Retour aux catégories", color = Color(0xFF154A36), fontWeight = FontWeight.SemiBold, fontSize = if (landscapeCompact) 12.sp else 14.sp)
                }
            }
            if (s.status.isNotEmpty()) {
                Text(s.status, color = c.secondary, style = MaterialTheme.typography.bodySmall)
            }
            if (s.section == 2 || s.section == 3) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { s.refreshCatalog() }, contentPadding = PaddingValues(horizontal = 10.dp)) { Text("↻ Actualiser", color = Color(0xFF154A36), maxLines = 1, fontSize = 12.sp) }
                    var sortOpen by remember { mutableStateOf(false) }
                    Box {
                        OutlinedButton(onClick = { sortOpen = true }) {
                            Text(when (s.vodSort) {
                                1 -> "Récemment ajouté"
                                2 -> "A-Z"
                                else -> "Par défaut"
                            }, maxLines = 1, fontSize = 12.sp, color = Color(0xFF154A36))
                        }
                        DropdownMenu(expanded = sortOpen, onDismissRequest = { sortOpen = false }) {
                            listOf("Par défaut", "Récemment ajouté", "A-Z").forEachIndexed { index, label ->
                                DropdownMenuItem(text = { Text(label) }, onClick = {
                                    s.vodSort = index
                                    sortOpen = false
                                })
                            }
                        }
                    }
                }
            }
            if (s.section == 4 && list.none { it.url in favUrls }) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(
                        Modifier.widthIn(max = 460.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(Color.White.copy(alpha = 0.96f))
                            .border(1.5.dp, Color(0xFFB5D8C6), RoundedCornerShape(22.dp))
                            .padding(horizontal = 28.dp, vertical = 30.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Filled.Star, contentDescription = null,
                            tint = Color(0xFF176247), modifier = Modifier.size(44.dp))
                        Text("Aucun favori pour le moment", fontWeight = FontWeight.Bold,
                            color = Color(0xFF153F31), fontSize = 20.sp, textAlign = TextAlign.Center)
                        Text("Pour retrouver facilement une chaîne, ajoutez-la à vos favoris depuis la liste des chaînes.",
                            color = Color(0xFF466257), fontSize = 14.sp, textAlign = TextAlign.Center)
                        OutlinedButton(onClick = { s.section = 0; s.group = null }) {
                            Text("Découvrir les chaînes", color = Color(0xFF145A43))
                        }
                    }
                }
            } else if (s.episodes != null && s.section == 3) {
                Column(Modifier.weight(1f)) {
                    TextButton(onClick = { s.closeEpisodes() }) {
                        Text("‹  Retour aux séries", color = Color(0xFF15523B),
                            fontWeight = FontWeight.SemiBold)
                    }
                    Text(s.episodeSeries?.name ?: "Épisodes", fontSize = 20.sp,
                        fontWeight = FontWeight.Bold, color = Color(0xFF173D2E))
                    Spacer(Modifier.height(10.dp))
                    val episodes = s.episodes.orEmpty()
                    if (episodes.isEmpty()) {
                        Text(if (s.status.startsWith("Erreur")) "Impossible de récupérer les épisodes."
                            else if (s.status.contains("Chargement")) "Chargement des épisodes…"
                            else "Aucun épisode disponible pour cette série.",
                            color = Color(0xFF173D2E), fontSize = 14.sp)
                    } else {
                        androidx.compose.foundation.lazy.LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(9.dp),
                            contentPadding = PaddingValues(bottom = 18.dp)
                        ) {
                            items(episodes.size) { index ->
                                val episode = episodes[index]
                                Row(
                                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                                        .background(Color.White.copy(alpha = .94f))
                                        .clickable { s.pick(episode) }
                                        .padding(9.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(Modifier.width(92.dp).height(65.dp)
                                        .clip(RoundedCornerShape(9.dp))
                                        .background(Color(0xFFE7EEE9)),
                                        contentAlignment = Alignment.Center) {
                                        if (episode.logo.startsWith("http")) {
                                            AsyncImage(model = episode.logo,
                                                contentDescription = null,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize())
                                        } else {
                                            Icon(Icons.Filled.PlayArrow,
                                                contentDescription = null,
                                                tint = Color(0xFF15523B))
                                        }
                                    }
                                    Column(Modifier.weight(1f)) {
                                        Text("Épisode " + (index + 1),
                                            color = Color(0xFFCB651B),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold)
                                        Text(episode.name, color = Color(0xFF173D2E),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis)
                                    }
                                    Icon(Icons.Filled.PlayArrow,
                                        contentDescription = "Lire l’épisode",
                                        tint = Color(0xFF15523B))
                                }
                            }
                        }
                    }
                }
            } else if (s.section == 2 || s.section == 3) {
                val vodItems = if (s.section == 2) s.movies.orEmpty() else s.series.orEmpty()
                val categoryNames = (if (s.section == 2) s.movieCategories else s.seriesCategories)
                    .plus(vodItems.map { it.group }).filter { it.isNotBlank() }.distinct()
                val resume = s.continueWatching.filter { it.kind == s.section - 1 }.take(30)
                val displayed = when {
                    s.query.isNotBlank() -> vodItems.filter {
                        it.name.contains(s.query, ignoreCase = true) ||
                            it.group.contains(s.query, ignoreCase = true)
                    }
                    s.group == "Continuez à Regarder" -> resume
                    s.group != null -> vodItems.filter { it.group == s.group }
                    else -> emptyList()
                }.let { entries ->
                    if (s.group == "Continuez à Regarder") entries
                    else when (s.vodSort) {
                        1 -> entries.sortedWith(compareByDescending<Item> { it.modifiedAt }.thenBy { it.name })
                        2 -> entries.sortedBy { it.name.lowercase() }
                        else -> entries
                    }
                }
                val columnsCount = if (wide) 5 else 3
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columnsCount),
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 18.dp)
                ) {
                    if (s.group == null && s.query.isBlank()) {
                        item {
                            VodIconTile("Continuez à Regarder", "", true, isContinue = true) {
                                s.group = "Continuez à Regarder"
                            }
                        }
                        items(categoryNames) { name ->
                            VodIconTile(name, "", true) { s.requestGroup(name) }
                        }
                    } else {
                        items(displayed, key = { it.kind.toString() + ":" + it.id }) { item ->
                            VodIconTile(item.name, item.logo, false) {
                                s.requestAdultItem(item) { s.openVodDetails(item) }
                            }
                        }
                    }
                }
            } else if (s.section == 0 && useSideRail) {
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.weight(if (s.previewChannelId != null) 0.64f else 1f).fillMaxHeight()) {
                        val visibleItems = if (s.query.isNotBlank()) {
                            list.filter { it.name.contains(s.query, ignoreCase = true) || it.group.contains(s.query, ignoreCase = true) }
                        } else if (s.group == null) emptyList()
                        else specialGroups[s.group] ?: list.filter { it.group == s.group }
                        if (s.group == null && s.query.isBlank()) {
                            val groups = (specialGroups.keys + list.map { it.group }).filter { it.isNotBlank() }.distinct()
                            BoxWithConstraints(Modifier.fillMaxSize()) {
                                val cardHeight = (maxHeight - 42.dp) / 3
                                LaunchedEffect(s.group, s.section, lastBouquet, groups.size) {
                                    if (s.group == null && s.section == 0 && restoreBouquetPending && lastBouquet != null) {
                                        val index = groups.indexOf(lastBouquet)
                                        if (index >= 0) {
                                            bouquetGridState.scrollToItem((index / 3) * 3)
                                            kotlinx.coroutines.delay(180)
                                            runCatching { restoreBouquetFocus.requestFocus() }
                                        }
                                        restoreBouquetPending = false
                                    }
                                }
                                LazyVerticalGrid(columns = GridCells.Fixed(3),
                                    modifier = Modifier.fillMaxSize().clipToBounds(),
                                    state = bouquetGridState,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    contentPadding = PaddingValues(bottom = 12.dp)) {
                                    items(groups, key = { it }) { groupName ->
                                        var cardFocused by remember { mutableStateOf(false) }
                                        val cardShape = RoundedCornerShape(18.dp)
                                        Column(Modifier.fillMaxWidth().height(cardHeight)
                                            .onFocusChanged { cardFocused = it.isFocused }
                                            .clip(cardShape)
                                            .background(if (cardFocused) Color(0xFFD6EAE0) else Color.White.copy(alpha = 0.88f))
                                            .border(if (cardFocused) 4.dp else 2.dp,
                                                if (cardFocused) Color(0xFF145A43) else Color(0xFFB2C9C3), cardShape)
                                            .clickable { lastBouquet = groupName; s.requestGroup(groupName) }
                                            .then(if (groupName == lastBouquet) Modifier.focusRequester(restoreBouquetFocus) else Modifier)
                                            .padding(10.dp)) {
                                            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Text(bouquetIcon(groupName), fontSize = if (tvWide) 37.sp else 28.sp)
                                                    Spacer(Modifier.height(6.dp))
                                                    Text(groupName, fontSize = if (tvWide) 19.sp else 15.sp,
                                                        maxLines = 3, color = Color(0xFF17332B), textAlign = TextAlign.Center,
                                                        fontWeight = FontWeight.Bold,
                                                        lineHeight = if (tvWide) 24.sp else 19.sp)
                                                }
                                            }
                                            Text("Explorer  →", fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                                color = Color(0xFF15523B), modifier = Modifier.align(Alignment.Start))
                                        }
                                    }
                                }
                            }
                        } else {
                            val firstChannelFocus = remember { FocusRequester() }
                            LaunchedEffect(s.group, visibleItems.firstOrNull()?.id) {
                                if (s.group != null && visibleItems.isNotEmpty()) {
                                    kotlinx.coroutines.delay(180)
                                    runCatching { firstChannelFocus.requestFocus() }
                                }
                            }
                            LazyVerticalGrid(columns = GridCells.Adaptive(if (tvWide) 180.dp else 150.dp),
                                horizontalArrangement = Arrangement.spacedBy(7.dp),
                                verticalArrangement = Arrangement.spacedBy(7.dp),
                                contentPadding = PaddingValues(bottom = 12.dp)) {
                                items(visibleItems, key = { item -> item.id }) { item ->
                                    var channelFocused by remember { mutableStateOf(false) }
                                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                                        .onFocusChanged { channelFocused = it.isFocused }
                                        .background(if (channelFocused) Color(0xFFD6EAE0) else Color.White.copy(alpha = if (s.previewChannelId == item.id) 1f else 0.96f))
                                        .border(if (channelFocused) 4.dp else 1.dp,
                                            if (channelFocused) Color(0xFF145A43) else Color(0xFFB2C9C3), RoundedCornerShape(12.dp))
                                        .clickable {
                                            s.selectPlaybackBouquet(s.group, visibleItems)
                                            s.requestAdultItem(item) { s.pick(item) }
                                        }.then(if (visibleItems.firstOrNull()?.id == item.id) Modifier.focusRequester(firstChannelFocus) else Modifier)
                                        .padding(7.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally) {
                                        Logo(item)
                                        Spacer(Modifier.height(4.dp))
                                        Text(item.name, fontSize = if (tvWide) 14.sp else 12.sp, color = c.onSurface,
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
                        .clickable(enabled = s.previewUrl != null) { s.playLive(selected, fromPreview = true) },
                    contentAlignment = Alignment.Center
                ) {
                    val url = s.previewUrl
                    if (url != null) VideoPlayer(url, onError = { s.onPlayerError(it) },
                        modifier = Modifier.fillMaxSize(), showControls = false,
                        onVideoResolution = { w, h -> resolution = if (w > 0 && h > 0) "$w × $h" else "—" }, sharedPlayer = s.livePlayer)
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
                        TextButton(onClick = { s.playLive(selected, fromPreview = true) },
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

@Composable
private fun VodIconTile(title: String, poster: String, isBouquet: Boolean, isContinue: Boolean = false, onClick: () -> Unit) {
    val c = MaterialTheme.colorScheme
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp))
            .background(Color.White.copy(alpha = .95f))
            .clickable(onClick = onClick).padding(7.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isBouquet) {
            Column(
                Modifier.fillMaxWidth().aspectRatio(.82f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF4F8F5))
                    .border(1.dp, Color(0xFFD6E6DC), RoundedCornerShape(12.dp))
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Spacer(Modifier.height(2.dp))
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(title, color = Color(0xFF163C2E), fontWeight = FontWeight.Bold,
                        fontSize = if (title.length > 25) 10.sp else if (title.length > 17) 11.sp else 13.sp,
                        maxLines = 6, overflow = TextOverflow.Ellipsis,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = if (title.length > 25) 12.sp else 15.sp)
                }
                Spacer(Modifier.height(7.dp))
                Text("Explorer  ›", color = Color(0xFF15523B),
                    fontWeight = FontWeight.Medium, fontSize = 10.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    maxLines = 1)
            }
        } else {
            Box(Modifier.fillMaxWidth().aspectRatio(.75f)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFFE7EEE9)), contentAlignment = Alignment.Center) {
                if (poster.startsWith("http")) {
                    AsyncImage(model = poster, contentDescription = title,
                        contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                } else {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null,
                        tint = c.primary, modifier = Modifier.size(38.dp))
                }
            }
            Spacer(Modifier.height(7.dp))
            Text(title, color = Color(0xFF173D2E), fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center, lineHeight = 16.sp)
        }
    }
}

@Composable
private fun VodDetailsPage(s: AppState) {
    val item = s.selectedVod ?: return
    val info = s.selectedVodDetails
    val ctx = LocalContext.current
    val c = MaterialTheme.colorScheme
    val favorite = s.favs.any { it.url == item.url }
    Dialog(onDismissRequest = { s.closeVodDetails() },
        properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = Color(0xFFFAFCFA)) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                Box(Modifier.fillMaxWidth().height(230.dp)) {
                    AsyncImage(
                        model = info?.backdrop?.takeIf { it.startsWith("http") } ?: item.logo,
                        contentDescription = item.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(Modifier.fillMaxSize().background(
                        Brush.verticalGradient(listOf(Color.Black.copy(alpha = .16f),
                            Color.Transparent, Color(0xFF143B2D).copy(alpha = .88f)))))
                    IconButton(onClick = { s.closeVodDetails() },
                        modifier = Modifier.align(Alignment.TopStart).padding(12.dp)
                            .background(Color.White.copy(alpha = .94f), RoundedCornerShape(50))) {
                        Icon(Icons.Filled.Close, contentDescription = "Retour")
                    }
                    Text(item.name, Modifier.align(Alignment.BottomStart).padding(20.dp),
                        fontSize = 24.sp, fontWeight = FontWeight.Bold,
                        color = Color.White, maxLines = 3, lineHeight = 28.sp)
                }
                Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(if (item.kind == 2) "SÉRIE" else "FILM", color = Color(0xFFCB651B), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    val metadata = listOfNotNull(
                        info?.rating?.takeIf { it.isNotBlank() }?.let { "★ $it" },
                        info?.releaseDate?.takeIf { it.isNotBlank() },
                        info?.genre?.takeIf { it.isNotBlank() }
                    ).joinToString("   ·   ")
                    if (metadata.isNotEmpty()) Text(metadata, color = c.primary,
                        fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Button(onClick = { s.launchSelectedVod() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF15523B)),
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(16.dp)) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (item.kind == 2) "Voir les épisodes" else "Lancer le film")
                    }
                    OutlinedButton(onClick = { s.toggleFav(item) },
                        border = BorderStroke(1.dp, Color(0xFFB7D3C3)),
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(16.dp)) {
                        Icon(Icons.Filled.Star, contentDescription = null,
                            tint = if (favorite) c.secondary else c.primary)
                        Spacer(Modifier.width(8.dp))
                        Text(if (favorite) "Retirer des favoris" else "Ajouter aux favoris")
                    }
                    if (!info?.trailer.isNullOrBlank()) {
                        OutlinedButton(onClick = {
                            val raw = info!!.trailer.trim()
                            val url = if (raw.startsWith("https://") || raw.startsWith("http://"))
                                raw else "https://www.youtube.com/watch?v=" + raw
                            runCatching {
                                ctx.startActivity(android.content.Intent(
                                    android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url)))
                            }
                        }, modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(16.dp)) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Voir la bande-annonce")
                        }
                    }
                    Text("Synopsis", fontWeight = FontWeight.Bold, fontSize = 19.sp,
                        color = c.onSurface)
                    Text(
                        if (s.vodDetailsLoading) "Chargement de la description…"
                        else info?.description?.takeIf { it.isNotBlank() }
                            ?: "Aucun résumé disponible pour ce titre.",
                        fontSize = 14.sp, color = c.onSurfaceVariant, lineHeight = 21.sp
                    )
                }
            }
        }
    }
}
