package com.mediaplus.afrique

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ParentalGate(s: AppState) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = { s.pendingAdultGroup = null; s.pendingAdultItem = null },
        title = { Text("Contrôle parental") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Saisissez votre code parental. Le code initial est 0000 ; vous pouvez le modifier dans Paramètres.")
                OutlinedTextField(pin, { pin = it.filter(Char::isDigit).take(4) },
                    label = { Text("Code PIN") }, visualTransformation = PasswordVisualTransformation(), singleLine = true)
                if (error.isNotEmpty()) Text(error, color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (s.unlockAdult(pin)) {
                    s.pendingAdultItem?.let { s.pick(it) }
                    s.pendingAdultItem = null
                } else error = "Code incorrect"
            }) { Text("Déverrouiller") }
        },
        dismissButton = { TextButton(onClick = { s.pendingAdultGroup = null; s.pendingAdultItem = null }) { Text("Annuler") } }
    )
}

@Composable
fun SettingsScreen(s: AppState, page: String?, onPage: (String?) -> Unit, modifier: Modifier = Modifier) {
    var oldPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var accountMode by remember { mutableIntStateOf(0) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showAccountPassword by remember { mutableStateOf(false) }
    var mac by remember { mutableStateOf("") }
    var deleteIndex by remember { mutableIntStateOf(-1) }
    var confirmDisconnect by remember { mutableStateOf(false) }
    var confirmClearCache by remember { mutableStateOf(false) }
    val firstSettingFocus = remember { FocusRequester() }
    LaunchedEffect(page) {
        if (page == null) {
            kotlinx.coroutines.delay(180)
            runCatching { firstSettingFocus.requestFocus() }
        }
    }

    Column(modifier.verticalScroll(rememberScrollState()).padding(horizontal = 10.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        if (page == null) {
            val sections = listOf(
                Triple("Mon compte", "Gérer vos profils et connexions", "♙"),
                Triple("Informations d’abonnement", "Validité, expiration et accès", "◷"),
                Triple("Contrôle parental", "Sécuriser l’accès aux contenus", "◈"),
                Triple("Vider le cache", "Effacer l’historique récent et les données temporaires", "⌫"),
                Triple("À propos", "Version et informations de l’application", "ⓘ")
            )
            sections.forEachIndexed { index, (section, subtitle, symbol) ->
                var focused by remember(section) { mutableStateOf(false) }
                Surface(
                    onClick = { onPage(section); message = "" },
                    modifier = Modifier.fillMaxWidth(0.72f)
                        .widthIn(max = 610.dp)
                        .then(if (index == 0) Modifier.focusRequester(firstSettingFocus) else Modifier)
                        .onFocusChanged { focused = it.isFocused }
                        .shadow(if (focused) 7.dp else 3.dp, RoundedCornerShape(18.dp)),
                    color = if (focused) Color(0xFF0B4F32).copy(alpha = 0.94f)
                        else Color.White.copy(alpha = 0.82f),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(if (focused) 3.dp else 1.dp,
                        if (focused) Color(0xFF42D77B) else Color(0xFF9BB6A8))
                ) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(13.dp)) {
                        Surface(shape = RoundedCornerShape(13.dp), color = if (focused) Color(0xFF126B42) else Color(0xFFEAF3EE)) {
                            Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                                Text(symbol, fontSize = 23.sp, color = if (focused) Color(0xFFFFA02A) else Color(0xFF1A6345))
                            }
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(section, style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold, color = if (focused) Color.White else Color(0xFF193D30))
                            Text(subtitle, style = MaterialTheme.typography.bodySmall,
                                color = if (focused) Color.White else Color(0xFF6D7772))
                        }
                        Text("›", fontSize = 27.sp, color = if (focused) Color(0xFFFFA02A) else Color(0xFFB66A26))
                    }
                }
            }
        } else {
            // Detail pages keep their content headings inside cards; no redundant back/title header.
            when (page) {
                "Informations d’abonnement" -> {
                    val expiry = s.subscriptionExpirySeconds
                    val date = expiry?.let { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(it * 1000L)) } ?: "Non communiquée"
                    val days = expiry?.let { ((it * 1000L - System.currentTimeMillis()).coerceAtLeast(0L) + 86399999L) / 86400000L }
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(0.78f).widthIn(max = 700.dp),
                            shape = RoundedCornerShape(24.dp),
                            color = Color(0xFFF6FAF8).copy(alpha = 0.73f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.85f)),
                            shadowElevation = 6.dp
                        ) {
                            Column(Modifier.padding(horizontal = 22.dp, vertical = 18.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("▣  Informations d’abonnement",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold, color = Color(0xFF174A35))
                                Surface(
                                    color = Color.White.copy(alpha = 0.52f),
                                    shape = RoundedCornerShape(18.dp),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.75f))
                                ) {
                                    Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        SettingsDetail("Identifiant", if (s.mode == 0) s.user.ifBlank { "Non renseigné" } else s.mac)
                                        HorizontalDivider(color = Color(0xFFB9CCC3))
                                        SettingsDetail("Expiration", date)
                                        HorizontalDivider(color = Color(0xFFB9CCC3))
                                        SettingsDetail("Jours restants", days?.let { "$it jours" } ?: "Inconnu", emphasized = true)
                                        HorizontalDivider(color = Color(0xFFB9CCC3))
                                        SettingsDetail("Type de connexion", if (s.mode == 0) "Xtream Codes" else "Adresse MAC")
                                    }
                                }
                                OutlinedButton(onClick = { s.refreshSubscription() },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(18.dp),
                                    border = BorderStroke(1.dp, Color(0xFF075A36)),
                                    colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFFE4F1E9).copy(alpha = 0.82f))) {
                                    Text("⟳  Actualiser les informations", color = Color(0xFF075A36),
                                        fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
                "Contrôle parental" -> {
                    var showOldPin by remember { mutableStateOf(false) }
                    var showNewPin by remember { mutableStateOf(false) }
                    var showConfirmPin by remember { mutableStateOf(false) }
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(0.88f).widthIn(max = 760.dp),
                            shape = RoundedCornerShape(24.dp),
                            color = Color(0xFFF6FAF8).copy(alpha = 0.83f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.85f)),
                            shadowElevation = 6.dp
                        ) {
                            Column(
                                Modifier.padding(horizontal = 22.dp, vertical = 18.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text("🔒  Sécurité des contenus",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color(0xFF174A35), fontWeight = FontWeight.Bold)
                                Text("Le code initial est 0000. Saisissez votre code actuel pour le modifier.",
                                    style = MaterialTheme.typography.bodyMedium, color = Color(0xFF334B40))
                                OutlinedTextField(oldPin, { oldPin = it.filter(Char::isDigit).take(4) },
                                    label = { Text("Code actuel") },
                                    visualTransformation = if (showOldPin) VisualTransformation.None else PasswordVisualTransformation(),
                                    trailingIcon = { TextButton(onClick = { showOldPin = !showOldPin }) {
                                        Text(if (showOldPin) "Masquer" else "Voir")
                                    } },
                                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(18.dp))
                                OutlinedTextField(newPin, { newPin = it.filter(Char::isDigit).take(4) },
                                    label = { Text("Nouveau code (4 chiffres)") },
                                    visualTransformation = if (showNewPin) VisualTransformation.None else PasswordVisualTransformation(),
                                    trailingIcon = { TextButton(onClick = { showNewPin = !showNewPin }) {
                                        Text(if (showNewPin) "Masquer" else "Voir")
                                    } },
                                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(18.dp))
                                OutlinedTextField(confirmPin, { confirmPin = it.filter(Char::isDigit).take(4) },
                                    label = { Text("Confirmer le code") },
                                    visualTransformation = if (showConfirmPin) VisualTransformation.None else PasswordVisualTransformation(),
                                    trailingIcon = { TextButton(onClick = { showConfirmPin = !showConfirmPin }) {
                                        Text(if (showConfirmPin) "Masquer" else "Voir")
                                    } },
                                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(18.dp))
                                Button(onClick = {
                                    message = if (newPin == confirmPin && s.changeParentalPin(oldPin, newPin)) "Code parental modifié" else "Vérifiez le code actuel et la confirmation"
                                    oldPin = ""; newPin = ""; confirmPin = ""
                                }, modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF075A36)),
                                    shape = RoundedCornerShape(18.dp)) {
                                    Text("▣  Enregistrer le nouveau code")
                                }
                                if (s.adultUnlocked) OutlinedButton(
                                    onClick = { s.lockAdult() }, modifier = Modifier.fillMaxWidth()
                                ) { Text("Verrouiller les chaînes Adultes") }
                            }
                        }
                    }
                }
                "Mon compte" -> {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                        Column(
                            modifier = Modifier.fillMaxWidth(0.80f).widthIn(max = 760.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(24.dp),
                                color = Color(0xFFF6FAF8).copy(alpha = 0.76f),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.9f)),
                                shadowElevation = 5.dp
                            ) {
                                Column(Modifier.fillMaxWidth().padding(20.dp),
                                    verticalArrangement = Arrangement.spacedBy(9.dp)) {
                                    Text("Compte actuellement connecté",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = Color(0xFF174A35), fontWeight = FontWeight.Bold)
                                    SettingsDetail(if (s.mode == 0) "Identifiant" else "Adresse MAC",
                                        if (s.mode == 0) s.user.ifBlank { "Non renseigné" } else s.mac)
                                }
                            }
                            s.accounts.forEachIndexed { index, account ->
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = Color(0xFFF6FAF8).copy(alpha = 0.76f),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.85f))
                                ) {
                                    Column(Modifier.fillMaxWidth().padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(account.name, fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF174A35))
                                        SettingsDetail(if (account.mode == 0) "Identifiant" else "Adresse MAC",
                                            if (account.mode == 0) account.user else account.mac)
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            TextButton(onClick = { s.activateAccount(index) }) { Text("Utiliser") }
                                            TextButton(onClick = { deleteIndex = index }) { Text("Supprimer") }
                                        }
                                    }
                                }
                            }
                            Button(
                                onClick = { onPage("Ajouter un compte") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF075A36))
                            ) { Text("＋  Ajouter un compte", fontWeight = FontWeight.Bold) }
                            OutlinedButton(
                                onClick = { confirmDisconnect = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp),
                                border = BorderStroke(1.dp, Color(0xFF075A36)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = Color.White.copy(alpha = 0.68f))
                            ) { Text("Se déconnecter", color = Color(0xFF075A36), fontWeight = FontWeight.SemiBold) }
                        }
                    }
                }
                "Ajouter un compte" -> {
                    SettingsPanel {
                        OutlinedTextField(name, { name = it }, label = { Text("Nom du compte") },
                            singleLine = true, modifier = Modifier.fillMaxWidth())
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(accountMode == 0, onClick = { accountMode = 0 }, label = { Text("Xtream") })
                            FilterChip(accountMode == 1, onClick = { accountMode = 1 }, label = { Text("MAC") })
                        }
                        if (accountMode == 0) {
                            OutlinedTextField(username, { username = it }, label = { Text("Nom d’utilisateur") },
                                singleLine = true, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(password, { password = it }, label = { Text("Mot de passe") },
                                visualTransformation = if (showAccountPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                singleLine = true, modifier = Modifier.fillMaxWidth(),
                                trailingIcon = { TextButton(onClick = { showAccountPassword = !showAccountPassword }) {
                                    Text(if (showAccountPassword) "Masquer" else "Voir")
                                } })
                        } else OutlinedTextField(mac, { mac = it }, label = { Text("Adresse MAC") },
                            singleLine = true, modifier = Modifier.fillMaxWidth())
                        Button(onClick = {
                            if (s.addAccount(name, accountMode, username, password, mac)) {
                                name = ""; username = ""; password = ""; mac = ""
                                onPage("Mon compte"); message = "Compte ajouté"
                            } else message = "Complétez tous les champs"
                        }, modifier = Modifier.fillMaxWidth()) { Text("Enregistrer le compte") }
                    }
                }
                "Vider le cache" -> {
                    val bottleGreen = Color(0xFF0B4F32)
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(0.80f).widthIn(max = 680.dp)
                                .shadow(5.dp, RoundedCornerShape(20.dp)),
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFFF9FBFF).copy(alpha = 0.76f),
                            border = BorderStroke(1.dp, bottleGreen.copy(alpha = 0.25f))
                        ) {
                            BoxWithConstraints(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                                if (maxWidth < 430.dp) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Text("Vider le cache", color = bottleGreen,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleMedium)
                                        Button(
                                            onClick = { confirmClearCache = true },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF7900)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) { Text("Vider le cache", color = Color.White) }
                                    }
                                } else {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        Text("Vider le cache", color = bottleGreen,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleLarge,
                                            modifier = Modifier.weight(1f))
                                        Button(
                                            onClick = { confirmClearCache = true },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF7900)),
                                            modifier = Modifier.widthIn(min = 170.dp)
                                        ) { Text("Vider le cache", color = Color.White) }
                                    }
                                }
                            }
                        }
                    }
                }
                "À propos" -> {
                    val bottleGreen = Color(0xFF0B4F32)
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(0.82f).widthIn(max = 780.dp)
                                .shadow(6.dp, RoundedCornerShape(22.dp)),
                            shape = RoundedCornerShape(22.dp),
                            color = Color(0xFFF9FBFF).copy(alpha = 0.78f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.85f))
                        ) {
                            BoxWithConstraints(Modifier.padding(18.dp)) {
                                if (maxWidth < 480.dp) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Image(painterResource(R.drawable.media_plus_brand),
                                            contentDescription = "Logo Media+ Afrique",
                                            modifier = Modifier.size(95.dp), contentScale = ContentScale.Fit)
                                        AboutInfo()
                                    }
                                } else {
                                    Row(verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                                        Image(painterResource(R.drawable.media_plus_brand),
                                            contentDescription = "Logo Media+ Afrique",
                                            modifier = Modifier.size(125.dp), contentScale = ContentScale.Fit)
                                        Box(Modifier.width(2.dp).height(120.dp)
                                            .background(bottleGreen.copy(alpha = 0.7f)))
                                        Box(Modifier.weight(1f)) { AboutInfo() }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (message.isNotEmpty()) Text(message, color = MaterialTheme.colorScheme.primary)
        }
    }
    if (deleteIndex >= 0) AlertDialog(
        onDismissRequest = { deleteIndex = -1 },
        title = { Text("Supprimer ce compte ?") },
        text = { Text("L’abonnement enregistré sera supprimé de l’application.") },
        confirmButton = { TextButton(onClick = { s.removeAccount(deleteIndex); deleteIndex = -1 }) { Text("Supprimer") } },
        dismissButton = { TextButton(onClick = { deleteIndex = -1 }) { Text("Annuler") } }
    )
    if (confirmClearCache) AlertDialog(
        onDismissRequest = { confirmClearCache = false },
        title = { Text("Vider le cache ?") },
        text = { Text("L’historique récent et les fichiers temporaires seront supprimés. Vos favoris et vos comptes ne seront pas effacés.") },
        confirmButton = { TextButton(onClick = {
            s.clearCache()
            confirmClearCache = false
            message = "Cache et historique récent supprimés"
        }) { Text("Effacer") } },
        dismissButton = { TextButton(onClick = { confirmClearCache = false }) { Text("Annuler") } }
    )
    if (confirmDisconnect) AlertDialog(
        onDismissRequest = { confirmDisconnect = false },
        title = { Text("Déconnexion") },
        text = { Text("Voulez-vous revenir à l’écran de connexion ?") },
        confirmButton = { TextButton(onClick = { confirmDisconnect = false; onPage(null); s.disconnect() }) { Text("Déconnecter") } },
        dismissButton = { TextButton(onClick = { confirmDisconnect = false }) { Text("Annuler") } }
    )
}

@Composable
private fun SettingsPanel(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(20.dp)),
        color = Color.White.copy(alpha = 0.97f),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Color(0xFFE1E8E3))
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(13.dp), content = content)
    }
}

@Composable
private fun SettingsDetail(label: String, value: String, emphasized: Boolean = false) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = Color(0xFF6C7972))
        Text(value, style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (emphasized) FontWeight.Bold else FontWeight.Medium,
            color = if (emphasized) Color(0xFFBF6A1C) else Color(0xFF173D2E))
    }
}

@Composable
private fun AboutInfo() {
    val green = Color(0xFF0B4F32)
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Text("À propos", color = green,
            fontSize = 25.sp, fontWeight = FontWeight.ExtraBold)
        Text("Version de l’application", color = Color(0xFF283E32),
            fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Surface(color = green, shape = RoundedCornerShape(50)) {
            Text("2.3.12", modifier = Modifier.padding(horizontal = 22.dp, vertical = 5.dp),
                color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(13.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Text("▣ Direct", color = green, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text("│", color = Color(0xFF8A9EAB))
            Text("▶ Films", color = green, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text("│", color = Color(0xFF8A9EAB))
            Text("▤ Séries", color = green, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}
