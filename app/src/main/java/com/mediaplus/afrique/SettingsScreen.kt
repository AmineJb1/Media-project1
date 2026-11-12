package com.mediaplus.afrique

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun ParentalGate(s: AppState) {
    var pin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = { s.pendingAdultGroup = null; s.pendingAdultItem = null },
        title = { Text(if (s.parentalPinExists) "Contrôle parental" else "Créer un code parental") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(if (s.parentalPinExists) "Saisissez votre code à 4 chiffres pour accéder aux chaînes Adultes." else "Créez un code PIN à 4 chiffres avant d'ouvrir les chaînes Adultes.")
                OutlinedTextField(pin, { pin = it.filter(Char::isDigit).take(4) }, label = { Text("Code PIN") }, visualTransformation = PasswordVisualTransformation(), singleLine = true)
                if (!s.parentalPinExists) OutlinedTextField(confirm, { confirm = it.filter(Char::isDigit).take(4) }, label = { Text("Confirmer le code") }, visualTransformation = PasswordVisualTransformation(), singleLine = true)
                if (error.isNotEmpty()) Text(error, color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (!s.parentalPinExists) {
                    if (pin.length != 4 || pin != confirm || !s.setParentalPin(pin)) error = "Code invalide ou confirmation différente"
                    else { s.unlockAdult(pin); s.pendingAdultItem?.let { s.pick(it) }; s.pendingAdultItem = null }
                } else if (s.unlockAdult(pin)) { s.pendingAdultItem?.let { s.pick(it) }; s.pendingAdultItem = null }
                else error = "Code incorrect"
            }) { Text(if (s.parentalPinExists) "Déverrouiller" else "Créer le code") }
        },
        dismissButton = { TextButton(onClick = { s.pendingAdultGroup = null; s.pendingAdultItem = null }) { Text("Annuler") } }
    )
}

@Composable
fun SettingsScreen(s: AppState, modifier: Modifier = Modifier) {
    var pin by remember { mutableStateOf("") }
    var pinConfirm by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var mode by remember { mutableIntStateOf(0) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var mac by remember { mutableStateOf("") }
    var deleteIndex by remember { mutableIntStateOf(-1) }
    var expandedSection by remember { mutableStateOf<String?>(null) }
    Column(modifier.verticalScroll(rememberScrollState()).padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Paramètres", style = MaterialTheme.typography.headlineMedium)
        Text("Choisissez une rubrique pour afficher ses options.")
        listOf("Informations d’abonnement", "Contrôle parental", "Mes comptes", "Lecture et stockage", "À propos").forEach { section ->
            OutlinedCard(onClick = { expandedSection = if (expandedSection == section) null else section }, modifier = Modifier.fillMaxWidth()) {
                Text((if (expandedSection == section) "⌄  " else "›  ") + section, modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleMedium)
            }
        }
        if (expandedSection == "Informations d’abonnement") {
            val days = s.subscriptionExpirySeconds?.let { ((it * 1000L - System.currentTimeMillis()).coerceAtLeast(0L) + 86399999L) / 86400000L }
            Text("Durée restante : " + (days?.let { "$it jours" } ?: "inconnue"))
            Text("Connexion : " + if (s.mode == 0) "Xtream Codes" else "Adresse MAC")
        }
        if (expandedSection == "Lecture et stockage") {
            Text("Cache vidéo : ${s.cacheMb} Mo")
            OutlinedButton(onClick = { s.clearCache() }) { Text("Vider le cache") }
        }
        if (expandedSection == "À propos") { Text("Media+ Afrique · Version 2.1.0") }
        if (expandedSection == "Contrôle parental") {
        HorizontalDivider()
        Text("Contrôle parental", style = MaterialTheme.typography.titleLarge)
        Text(if (s.parentalPinExists) "Code parental configuré" else "Aucun code parental. Il sera obligatoire au premier accès aux chaînes Adultes.")
        if (s.adultUnlocked) OutlinedButton(onClick = { s.lockAdult() }) { Text("Verrouiller les chaînes Adultes") }
        if (!s.parentalPinExists) {
            OutlinedTextField(pin, { pin = it.filter(Char::isDigit).take(4) }, label = { Text("Créer un PIN à 4 chiffres") }, visualTransformation = PasswordVisualTransformation())
            OutlinedTextField(pinConfirm, { pinConfirm = it.filter(Char::isDigit).take(4) }, label = { Text("Confirmer le PIN") }, visualTransformation = PasswordVisualTransformation())
            Button(onClick = {
                message = if (pin.length == 4 && pin == pinConfirm && s.setParentalPin(pin)) "Code parental créé" else "Vérifiez les deux codes PIN"
                pin = ""; pinConfirm = ""
            }) { Text("Enregistrer le code") }
        }
        }
        if (expandedSection == "Mes comptes") {
        HorizontalDivider()
        Text("Mes comptes", style = MaterialTheme.typography.titleLarge)
        s.accounts.forEachIndexed { index, account ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(account.name, Modifier.weight(1f))
                TextButton(onClick = { s.activateAccount(index) }) { Text("Utiliser") }
                TextButton(onClick = { deleteIndex = index }) { Text("Supprimer") }
            }
        }
        Text("Ajouter un abonnement", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(name, { name = it }, label = { Text("Nom du compte") }, singleLine = true)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(mode == 0, onClick = { mode = 0 }, label = { Text("Xtream Codes") })
            FilterChip(mode == 1, onClick = { mode = 1 }, label = { Text("Adresse MAC") })
        }
        if (mode == 0) {
            OutlinedTextField(username, { username = it }, label = { Text("Identifiant") }, singleLine = true)
            OutlinedTextField(password, { password = it }, label = { Text("Mot de passe") }, visualTransformation = PasswordVisualTransformation(), singleLine = true)
        } else OutlinedTextField(mac, { mac = it }, label = { Text("Adresse MAC") }, singleLine = true)
        Button(onClick = {
            if (s.addAccount(name, mode, username, password, mac)) {
                name = ""; username = ""; password = ""; mac = ""; message = "Compte enregistré"
            } else message = "Complétez les champs obligatoires"
        }) { Text("Ajouter le compte") }
        if (message.isNotEmpty()) Text(message)
        }
    }
    if (deleteIndex >= 0) AlertDialog(
        onDismissRequest = { deleteIndex = -1 },
        title = { Text("Supprimer ce compte ?") },
        text = { Text("L'abonnement enregistré sera retiré de cette application.") },
        confirmButton = { TextButton(onClick = { s.removeAccount(deleteIndex); deleteIndex = -1 }) { Text("Supprimer") } },
        dismissButton = { TextButton(onClick = { deleteIndex = -1 }) { Text("Annuler") } }
    )
}
