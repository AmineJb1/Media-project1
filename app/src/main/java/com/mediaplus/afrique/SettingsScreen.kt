package com.mediaplus.afrique

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
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

    Column(modifier.verticalScroll(rememberScrollState()).padding(horizontal = 10.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        if (page == null) {
            Text("Paramètres", style = MaterialTheme.typography.headlineMedium,
                color = Color(0xFF153E30), fontWeight = FontWeight.Bold)
            Text("Gérez votre abonnement et personnalisez votre expérience.",
                style = MaterialTheme.typography.bodyMedium, color = Color(0xFF65726C))
            val sections = listOf(
                Triple("Mon compte", "Gérer vos profils et connexions", "♙"),
                Triple("Informations d’abonnement", "Validité, expiration et accès", "◷"),
                Triple("Contrôle parental", "Sécuriser l’accès aux contenus", "◈"),
                Triple("À propos", "Version et informations de l’application", "ⓘ")
            )
            sections.forEach { (section, subtitle, symbol) ->
                Surface(
                    onClick = { onPage(section); message = "" },
                    modifier = Modifier.fillMaxWidth().shadow(3.dp, RoundedCornerShape(18.dp)),
                    color = Color.White.copy(alpha = 0.94f),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, Color(0xFFE1E7E3))
                ) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 17.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(13.dp)) {
                        Surface(shape = RoundedCornerShape(13.dp), color = Color(0xFFEAF3EE)) {
                            Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                                Text(symbol, fontSize = 23.sp, color = Color(0xFF1A6345))
                            }
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(section, style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold, color = Color(0xFF193D30))
                            Text(subtitle, style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF6D7772))
                        }
                        Text("›", fontSize = 27.sp, color = Color(0xFFB66A26))
                    }
                }
            }
        } else {
            TextButton(onClick = { onPage(if (page == "Ajouter un compte") "Mon compte" else null); message = "" }) {
                Text("‹  Retour")
            }
            Text(page, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color(0xFF153E30))
            when (page) {
                "Informations d’abonnement" -> {
                    Text("Nom d’utilisateur : " + if (s.mode == 0) s.user.ifBlank { "Non renseigné" } else "Adresse MAC : " + s.mac)
                    val expiry = s.subscriptionExpirySeconds
                    val date = expiry?.let { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(it * 1000L)) } ?: "Non communiquée par le serveur"
                    Text("Date d’expiration : $date")
                    val days = expiry?.let { ((it * 1000L - System.currentTimeMillis()).coerceAtLeast(0L) + 86399999L) / 86400000L }
                    Text("Jours restants : " + (days?.toString() ?: "Inconnu"))
                    Text("Connexion : " + if (s.mode == 0) "Xtream Codes" else "Adresse MAC")
                    OutlinedButton(onClick = { s.refreshSubscription() }) { Text("Actualiser l’expiration") }
                }
                "Contrôle parental" -> {
                    Text("Code initial : 0000. Pour le modifier, saisissez le code actuel.")
                    OutlinedTextField(oldPin, { oldPin = it.filter(Char::isDigit).take(4) },
                        label = { Text("Code actuel") }, visualTransformation = PasswordVisualTransformation(), singleLine = true)
                    OutlinedTextField(newPin, { newPin = it.filter(Char::isDigit).take(4) },
                        label = { Text("Nouveau code à 4 chiffres") }, visualTransformation = PasswordVisualTransformation(), singleLine = true)
                    OutlinedTextField(confirmPin, { confirmPin = it.filter(Char::isDigit).take(4) },
                        label = { Text("Confirmer le nouveau code") }, visualTransformation = PasswordVisualTransformation(), singleLine = true)
                    Button(onClick = {
                        message = if (newPin == confirmPin && s.changeParentalPin(oldPin, newPin)) "Code parental modifié" else "Vérifiez le code actuel et la confirmation"
                        oldPin = ""; newPin = ""; confirmPin = ""
                    }) { Text("Modifier le code") }
                    if (s.adultUnlocked) OutlinedButton(onClick = { s.lockAdult() }) { Text("Verrouiller les chaînes Adultes") }
                }
                "Mon compte" -> {
                    Text("Compte actuellement connecté", style = MaterialTheme.typography.titleMedium, color = Color(0xFF153E30), fontWeight = FontWeight.SemiBold)
                    Text(if (s.mode == 0) "Utilisateur : " + s.user.ifBlank { "Non renseigné" } else "Adresse MAC : " + s.mac)
                    s.accounts.forEachIndexed { index, account ->
                        OutlinedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.outlinedCardColors(containerColor = Color.White.copy(alpha = 0.94f))) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                                Text(account.name, fontWeight = FontWeight.Bold)
                                Text(if (account.mode == 0) "Utilisateur : ${account.user}" else "MAC : ${account.mac}")
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    TextButton(onClick = { s.activateAccount(index) }) { Text("Utiliser") }
                                    TextButton(onClick = { deleteIndex = index }) { Text("Supprimer") }
                                }
                            }
                        }
                    }
                    Button(onClick = { onPage("Ajouter un compte") }, modifier = Modifier.fillMaxWidth()) { Text("+ Ajouter un nouveau compte") }
                    OutlinedButton(onClick = { confirmDisconnect = true }, modifier = Modifier.fillMaxWidth()) { Text("Déconnecter") }
                }
                "Ajouter un compte" -> {
                    OutlinedTextField(name, { name = it }, label = { Text("Nom du compte") }, singleLine = true)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(accountMode == 0, onClick = { accountMode = 0 }, label = { Text("Xtream") })
                        FilterChip(accountMode == 1, onClick = { accountMode = 1 }, label = { Text("MAC") })
                    }
                    if (accountMode == 0) {
                        OutlinedTextField(username, { username = it }, label = { Text("Nom d’utilisateur") }, singleLine = true)
                        OutlinedTextField(password, { password = it }, label = { Text("Mot de passe") }, visualTransformation = if (showAccountPassword) VisualTransformation.None else PasswordVisualTransformation(), singleLine = true, trailingIcon = { TextButton(onClick = { showAccountPassword = !showAccountPassword }) { Text(if (showAccountPassword) "Masquer" else "Voir") } })
                    } else OutlinedTextField(mac, { mac = it }, label = { Text("Adresse MAC") }, singleLine = true)
                    Button(onClick = {
                        if (s.addAccount(name, accountMode, username, password, mac)) {
                            name = ""; username = ""; password = ""; mac = ""
                            onPage("Mon compte"); message = "Compte ajouté"
                        } else message = "Complétez tous les champs"
                    }) { Text("Enregistrer le compte") }
                }
                "À propos" -> Text("Media+ Côte d’Ivoire · Version ${BuildConfig.VERSION_NAME}")
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
    if (confirmDisconnect) AlertDialog(
        onDismissRequest = { confirmDisconnect = false },
        title = { Text("Déconnexion") },
        text = { Text("Voulez-vous revenir à l’écran de connexion ?") },
        confirmButton = { TextButton(onClick = { confirmDisconnect = false; onPage(null); s.disconnect() }) { Text("Déconnecter") } },
        dismissButton = { TextButton(onClick = { confirmDisconnect = false }) { Text("Annuler") } }
    )
}
