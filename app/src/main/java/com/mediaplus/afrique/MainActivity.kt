package com.mediaplus.afrique

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AfricaTheme { App() } }
    }
}

@Composable
fun App() {
    val ctx = LocalContext.current
    val activity = ctx as? Activity
    val scope = rememberCoroutineScope()
    val s = remember { AppState(ctx.applicationContext, scope) }
    var confirmExit by remember { mutableStateOf(false) }

    BackHandler(enabled = confirmExit) { confirmExit = false }
    BackHandler(enabled = !confirmExit && (s.playing != null || !s.showLogin)) {
        if (s.playing != null || s.previewChannelId != null || s.query.isNotEmpty() || s.episodes != null || s.group != null) {
            s.back()
        } else if (s.section != 0) {
            s.pickSection(0)
        } else {
            confirmExit = true
        }
    }

    val p = s.playing
    if (p != null) {
        if (s.playingChannel != null) TvPlayer(s) else VideoPlayer(p) { s.onPlayerError(it) }
    } else if (s.showLogin) {
        LoginScreen(s)
    } else {
        MainScreen(s, onExit = { confirmExit = true })
    }

    if (confirmExit) {
        AlertDialog(
            onDismissRequest = { confirmExit = false },
            title = { Text("Quitter l’application ?", color = Color(0xFF173A25)) },
            text = { Text("Voulez-vous vraiment fermer Media+ Côte d’Ivoire ?", color = Color(0xFF343A35)) },
            confirmButton = {
                Button(
                    onClick = { confirmExit = false; activity?.finish() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF58216))
                ) { Text("Oui, quitter", color = Color.White) }
            },
            dismissButton = {
                OutlinedButton(onClick = { confirmExit = false }) { Text("Non", color = Color(0xFF173A25)) }
            },
            containerColor = Color.White
        )
    }
}
