package com.mediaplus.afrique

import android.graphics.BitmapFactory
import android.content.res.Configuration
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
            val tvBackground = (ctx.resources.configuration.uiMode and Configuration.UI_MODE_TYPE_MASK) == Configuration.UI_MODE_TYPE_TELEVISION || ctx.resources.configuration.screenWidthDp >= 800 && ctx.resources.configuration.screenWidthDp > ctx.resources.configuration.screenHeightDp
            val premiumBackground = remember {
                try {
                    ctx.assets.open("bg_cote_ivoire_3d.jpg").use { stream ->
                        BitmapFactory.decodeStream(stream)?.let { original ->
                            // Intensify only the orange and green of the Ivorian flag.
                            // Keep the original Africa image, its contours and white areas.
                            val bitmap = original.copy(android.graphics.Bitmap.Config.ARGB_8888, true)
                            val width = bitmap.width
                            val pixels = IntArray(width * bitmap.height)
                            bitmap.getPixels(pixels, 0, width, 0, 0, width, bitmap.height)
                            val hsv = FloatArray(3)
                            for (i in pixels.indices) {
                                val pixel = pixels[i]
                                android.graphics.Color.colorToHSV(pixel, hsv)
                                val hue = hsv[0]
                                // Include pale orange pixels previously missed by the saturation threshold.
                                val red = android.graphics.Color.red(pixel)
                                val greenChannel = android.graphics.Color.green(pixel)
                                val blue = android.graphics.Color.blue(pixel)
                                // Target the tan / ochre orange flag stripe, including
                                // shaded 3D pixels; leave green, white and the logo intact.
                                val orange = hue in 9f..67f && hsv[1] > .075f &&
                                    red > blue * 1.12f &&
                                    greenChannel > blue * 1.035f &&
                                    red >= greenChannel * 1.025f
                                if (orange) {
                                    hsv[0] = 25f // vivid Côte d'Ivoire orange
                                    hsv[1] = (hsv[1] * 1.75f).coerceIn(.86f, 1f)
                                    hsv[2] = (hsv[2] * 1.26f).coerceAtMost(1f)
                                    pixels[i] = android.graphics.Color.HSVToColor(
                                        android.graphics.Color.alpha(pixel), hsv)
                                }
                            }
                            bitmap.setPixels(pixels, 0, width, 0, 0, width, bitmap.height)
                            bitmap.asImageBitmap()
                        }
                    }
                } catch (_: Exception) { null }
            }
            if (premiumBackground != null) {
                Image(
                    bitmap = premiumBackground,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().graphicsLayer {
                        // Enlarge Africa while keeping Côte d’Ivoire near the screen center.
                        scaleX = if (tvBackground) 0.84f else 1.48f
                        scaleY = if (tvBackground) 0.84f else 1.48f
                        translationX = size.width * (if (tvBackground) -0.12f else 0.28f)
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
            Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = if (tvBackground) 0.36f else 0.08f)))
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
