package com.mediaplus.afrique

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp

val Africa = lightColorScheme(
    primary = Color(0xFFDE6A12), onPrimary = Color.White,
    secondary = Color(0xFFCE5B09), tertiary = Color(0xFFFF8A00),
    primaryContainer = Color(0xFFFFEBDD), onPrimaryContainer = Color(0xFF72350D),
    background = Color.White, surface = Color.White, onSurface = Color(0xFF103D2B),
    surfaceVariant = Color(0xFFFFF8F2), onSurfaceVariant = Color(0xFF606660),
    outline = Color(0xFFD9DCDD)
)

// Decorative, translucent 3D-inspired Côte d’Ivoire ribbons shared by every screen.
@Composable
fun IvoireRibbons(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val band = (h * 0.095f).coerceAtMost(160f)
        fun ribbon(top: Boolean, lift: Float, color: Color, alpha: Float) {
            val y = if (top) 0f else h
            val sign = if (top) 1f else -1f
            val path = Path().apply {
                moveTo(0f, y)
                cubicTo(w * .28f, y + sign * (band * .70f + lift),
                    w * .65f, y + sign * (band * .05f + lift), w, y + sign * (band * .90f + lift))
                lineTo(w, y)
                close()
            }
            drawPath(path, color.copy(alpha = alpha))
        }
        ribbon(true, band * .16f, Color(0xFFFF7600), .73f)
        ribbon(true, 0f, Color(0xFF087F3D), .44f)
        ribbon(false, band * .20f, Color(0xFFFF8500), .45f)
        ribbon(false, 0f, Color(0xFF078B46), .23f)
        fun highlight(top: Boolean) {
            val y = if (top) 0f else h
            val sign = if (top) 1f else -1f
            val path = Path().apply {
                moveTo(0f, y + sign * (band * .18f))
                cubicTo(w * .30f, y + sign * (band * .91f),
                    w * .67f, y + sign * (band * .22f), w, y + sign * band)
            }
            drawPath(path, Color.White.copy(alpha = .72f), style = Stroke(width = 3.dp.toPx()))
        }
        highlight(true)
        highlight(false)
    }
}

@Composable
fun AfricaTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Africa) {
        Box(Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color.White, Color(0xFFFFFBF7), Color.White))
        )) {
            val ctx = LocalContext.current
            val premiumBackground = remember {
                try {
                    ctx.assets.open("bg_cote_ivoire_3d.jpg").use { stream ->
                        BitmapFactory.decodeStream(stream)?.asImageBitmap()
                    }
                } catch (_: Exception) { null }
            }
            if (premiumBackground != null) {
                Image(
                    bitmap = premiumBackground,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().graphicsLayer {
                        // Enlarge Africa while keeping Côte d’Ivoire near the screen center.
                        scaleX = 1.48f
                        scaleY = 1.48f
                        translationX = size.width * 0.28f
                    },
                    contentScale = ContentScale.Fit
                )
            } else {
                Image(
                    painter = painterResource(R.drawable.africa_3d_white),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
            Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.08f)))
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
