package com.mediaplus.afrique

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

val Africa = lightColorScheme(
    primary = Color(0xFF08723A), onPrimary = Color.White,
    secondary = Color(0xFFBD5900), tertiary = Color(0xFFFF8A00),
    primaryContainer = Color(0xFFE2F3E9), onPrimaryContainer = Color(0xFF064729),
    background = Color(0xFFFFFEFB), surface = Color(0xFFFFFFFF), onSurface = Color(0xFF17241D),
    surfaceVariant = Color(0xFFF2F5F1), onSurfaceVariant = Color(0xFF58645C),
    outline = Color(0xFFCDD9CF)
)

private fun loadBackground(ctx: Context): ImageBitmap? = try {
    val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    ctx.assets.open("bg.jpg").use { BitmapFactory.decodeStream(it, null, o) }
    var s = 1
    while (o.outWidth / s > 1600) s *= 2
    val o2 = BitmapFactory.Options().apply { inSampleSize = s }
    ctx.assets.open("bg.jpg").use { BitmapFactory.decodeStream(it, null, o2) }?.asImageBitmap()
} catch (e: Exception) { null }

@Composable
fun AfricaTheme(content: @Composable () -> Unit) {
    val ctx = LocalContext.current
    val bg = remember { loadBackground(ctx) }
    MaterialTheme(colorScheme = Africa) {
        Box(Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0xFFFFFFFF), Color(0xFFFAFAF8), Color(0xFFFFFFFF)))
        )) {
            if (bg != null) {
                Image(bg, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.97f)))
            }
            Surface(Modifier.fillMaxSize(), color = Color.Transparent) { content() }
        }
    }
}

@Composable
fun KenteBand() {
    val cols = listOf(Color(0xFFFF8A00), Color.White, Color(0xFF08723A), Color(0xFFE7E7E7))
    Canvas(Modifier.fillMaxWidth().height(5.dp)) {
        val w = size.height
        var x = 0f
        var i = 0
        while (x < size.width) {
            val p = Path().apply { moveTo(x, size.height); lineTo(x + w / 2, 0f); lineTo(x + w, size.height); close() }
            drawPath(p, cols[i % 4])
            x += w; i++
        }
    }
}
