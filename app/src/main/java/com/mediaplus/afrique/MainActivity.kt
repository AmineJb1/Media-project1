package com.mediaplus.afrique

import android.app.Activity
import android.os.Bundle
import android.content.pm.ActivityInfo
import android.view.View
import android.graphics.drawable.ColorDrawable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.Alignment
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

    BackHandler(enabled = s.pendingResumeItem != null) { s.pendingResumeItem = null }
    BackHandler(enabled = confirmExit) { confirmExit = false }
    BackHandler(enabled = !confirmExit && s.pendingResumeItem == null && (s.playing != null || !s.showLogin)) {
        if (settingsOpen && settingsPage != null) {
            settingsPage = if (settingsPage == "Ajouter un compte") "Mon compte" else null
        } else if (settingsOpen) {
            settingsOpen = false; settingsPage = null; s.pickSection(0)
        } else if (s.playing != null && s.playingChannel != null) {
            s.returnLiveToPreview()
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
            VideoPlayer(p, { s.onPlayerError(it) }, modifier = Modifier.fillMaxSize(), onBack = { s.back() }, resumePositionMs = s.resumePosition(p), onSavePosition = { position, duration -> s.savePosition(p, position, duration) })
        }
    } else if (s.restoringSession) {
        // Saved credentials are being verified in the background. Do not flash the
        // interactive login form before opening the user's already-active session.
        Box(Modifier.fillMaxSize().background(Color(0xFFF1F8FC)),
            contentAlignment = Alignment.Center) {
            androidx.compose.foundation.layout.Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Text("Media+ Côte d’Ivoire", color = Color(0xFF0B4F32),
                    style = MaterialTheme.typography.headlineMedium)
                CircularProgressIndicator(color = Color(0xFF0B4F32))
                Text("Ouverture de votre session…", color = Color(0xFF385B72))
            }
        }
    } else if (s.showLogin) {
        LoginScreen(s)
    } else {
        MainScreen(s, settingsOpen, settingsPage, { settingsOpen = it }, { settingsPage = it })
    }

    if (s.pendingResumeItem != null) {
        val item = s.pendingResumeItem!!
        val position = s.resumePosition(item.url)
        val minutes = position / 60000L
        val seconds = (position / 1000L) % 60L
        Dialog(onDismissRequest = { s.pendingResumeItem = null }) {
            Surface(
                shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                color = Color(0xFF10233E),
                modifier = Modifier.fillMaxWidth()
            ) {
                androidx.compose.foundation.layout.Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("Reprendre la lecture ?", color = Color.White,
                        style = MaterialTheme.typography.titleLarge)
                    Text(item.name, color = Color.White,
                        style = MaterialTheme.typography.bodyLarge)
                    Text("Dernière position : %02d:%02d".format(minutes, seconds),
                        color = Color(0xFFB7D1F5))
                    Button(onClick = { s.chooseResume(true) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1766DE))) {
                        Text("▶ Continuer à regarder")
                    }
                    OutlinedButton(onClick = { s.chooseResume(false) },
                        modifier = Modifier.fillMaxWidth()) {
                        Text("↺ Recommencer depuis le début", color = Color.White)
                    }
                }
            }
        }
    }

    if (confirmExit) {
        val noFocusRequester = remember { FocusRequester() }
        var noFocused by remember { mutableStateOf(false) }
        var yesFocused by remember { mutableStateOf(false) }
        LaunchedEffect(confirmExit) {
            kotlinx.coroutines.delay(120)
            noFocusRequester.requestFocus()
        }
        Dialog(onDismissRequest = { confirmExit = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Surface(
                modifier = Modifier.fillMaxWidth(.72f).widthIn(max = 430.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(22.dp),
                color = Color.White.copy(alpha = .91f),
                shadowElevation = 12.dp
            ) {
                androidx.compose.foundation.layout.Column(
                    modifier = Modifier.padding(horizontal = 22.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text("Quitter l’application ?",
                        color = Color(0xFF0B4F32),
                        style = MaterialTheme.typography.titleLarge)
                    Text("Voulez-vous vraiment fermer Media+ Côte d’Ivoire ?",
                        color = Color(0xFF234336),
                        style = MaterialTheme.typography.bodyMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { confirmExit = false },
                            modifier = Modifier.weight(1f).height(44.dp)
                                .focusRequester(noFocusRequester)
                                .onFocusChanged { noFocused = it.isFocused },
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (noFocused) Color(0xFF0B4F32) else Color(0xFFFF8A00)),
                            border = androidx.compose.foundation.BorderStroke(
                                if (noFocused) 3.dp else 1.dp,
                                if (noFocused) Color(0xFF42D77B) else Color(0xFFE87500))
                        ) { Text("Non", color = Color.White) }
                        Button(
                            onClick = { s.stopLivePlayback(); confirmExit = false; activity?.finish() },
                            modifier = Modifier.weight(1f).height(44.dp)
                                .onFocusChanged { yesFocused = it.isFocused },
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (yesFocused) Color(0xFF0B4F32) else Color(0xFFFF8A00)),
                            border = androidx.compose.foundation.BorderStroke(
                                if (yesFocused) 3.dp else 1.dp,
                                if (yesFocused) Color(0xFF42D77B) else Color(0xFFE87500))
                        ) { Text("Oui, quitter", color = Color.White) }
                    }
                }
            }
        }
    }
}
