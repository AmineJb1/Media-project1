package com.mediaplus.afrique

import android.app.Activity
import android.os.Bundle
import android.content.pm.ActivityInfo
import android.view.View
import android.graphics.drawable.ColorDrawable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

class MainActivity : FragmentActivity() {
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
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(s, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) s.stopLivePlayback()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            s.stopLivePlayback()
            if (s.isLivePlayerInitialized()) s.livePlayer.release()
        }
    }
    var confirmExit by remember { mutableStateOf(false) }
    var settingsOpen by remember { mutableStateOf(false) }
    var settingsPage by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(s.showLogin) {
        if (!s.showLogin) {
            settingsOpen = false
            settingsPage = null
            s.pickSection(0)
        }
    }

    BackHandler(enabled = confirmExit) { confirmExit = false }
    BackHandler(enabled = !confirmExit && (s.playing != null || !s.showLogin)) {
        if (settingsOpen && settingsPage != null) {
            settingsPage = if (settingsPage == "Ajouter un compte") "Mon compte" else null
        } else if (settingsOpen) {
            settingsOpen = false; settingsPage = null; s.pickSection(0)
        } else if (s.playing != null || s.previewChannelId != null || s.query.isNotEmpty() || s.episodes != null || s.selectedVod != null || s.group != null) {
            s.back()
        } else if (s.section != 0) {
            s.pickSection(0)
        } else {
            confirmExit = true
        }
    }

    val p = s.playing
    val vodFullscreen = p != null && s.playingChannel == null
    DisposableEffect(activity, vodFullscreen) {
        if (activity != null && vodFullscreen) {
            activity.window.setBackgroundDrawable(ColorDrawable(android.graphics.Color.BLACK))
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            activity.window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        }
        onDispose {
            if (activity != null && vodFullscreen) {
                activity.window.setBackgroundDrawable(ColorDrawable(android.graphics.Color.WHITE))
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                activity.window.decorView.systemUiVisibility = 0
            }
        }
    }
    if (p != null) {
        if (s.playingChannel != null) TvPlayer(s) else Box(Modifier.fillMaxSize().background(Color.Black)) {
            VideoPlayer(p, { s.onPlayerError(it) }, modifier = Modifier.fillMaxSize())
        }
    } else if (s.showLogin) {
        LoginScreen(s)
    } else {
        MainScreen(s, settingsOpen, settingsPage, { settingsOpen = it }, { settingsPage = it })
    }

    if (confirmExit) {
        AlertDialog(
            onDismissRequest = { confirmExit = false },
            modifier = Modifier.fillMaxWidth(0.72f),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(22.dp),
            title = {
                Text("Quitter l’application ?",
                    color = Color(0xFF0B4F32),
                    style = MaterialTheme.typography.titleLarge)
            },
            text = {
                Text("Voulez-vous vraiment fermer Media+ Côte d’Ivoire ?",
                    color = Color(0xFF234336),
                    style = MaterialTheme.typography.bodyMedium)
            },
            confirmButton = {
                var quitFocused by remember { mutableStateOf(false) }
                Button(
                    onClick = { s.stopLivePlayback(); confirmExit = false; activity?.finish() },
                    modifier = Modifier.onFocusChanged { quitFocused = it.isFocused },
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        if (quitFocused) 2.dp else 1.dp,
                        if (quitFocused) Color(0xFF42D77B) else Color(0xFF0B4F32)),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0B4F32))
                ) { Text("Oui, quitter", color = Color.White) }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { confirmExit = false },
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color(0xFFFF8A00), contentColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE87500))
                ) { Text("Non", color = Color.White) }
            },
            containerColor = Color.White.copy(alpha = 0.88f)
        )
    }
}
