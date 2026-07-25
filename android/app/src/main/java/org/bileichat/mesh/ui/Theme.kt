package org.bileichat.mesh.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import org.bileichat.mesh.SendTier

// AMOLED industrial palette: true black canvas, near-black panels, hairline borders,
// tier colors as the only saturated accents.
val AmoledBlack = Color(0xFF000000)
val Panel = Color(0xFF0A0A0C)
val PanelRaised = Color(0xFF121216)
val Hairline = Color(0xFF1F1F26)
val TextBright = Color(0xFFE9EBEE)
val TextDim = Color(0xFF8A8F98)
val TierLocal = Color(0xFF37C8A6)
val TierBroadcast = Color(0xFF4C8DF6)
val TierPrivate = Color(0xFFA66BF0)
val TrustAmber = Color(0xFFE0A93E)
val PanicRed = Color(0xFFE4443A)

fun tierColor(tier: SendTier): Color = when (tier) {
    SendTier.LOCAL -> TierLocal
    SendTier.BROADCAST -> TierBroadcast
    SendTier.PRIVATE -> TierPrivate
}

/** Micro uppercase monospace label — the industrial workhorse of this UI. */
fun monoMicro(color: Color = TextDim) = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontSize = 10.sp,
    letterSpacing = 1.5.sp,
    color = color
)

fun monoLabel(color: Color = TextBright) = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontSize = 11.sp,
    letterSpacing = 2.5.sp,
    color = color
)

fun monoBody(color: Color = TextBright) = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontSize = 12.sp,
    letterSpacing = 0.5.sp,
    color = color
)

private val scheme = darkColorScheme(
    primary = TierLocal,
    onPrimary = AmoledBlack,
    background = AmoledBlack,
    onBackground = TextBright,
    surface = AmoledBlack,
    onSurface = TextBright,
    surfaceVariant = Panel,
    onSurfaceVariant = TextDim,
    outline = Hairline,
    error = PanicRed
)

@Composable
fun MeshTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, content = content)
}
