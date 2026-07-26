package org.bileichat.mesh.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.bileichat.mesh.SendTier

// Messenger palette: dark neutral canvas, one accent for outgoing messages, tier colors
// demoted to small conversation avatars. The previous industrial scheme painted every
// message in its tier colour, which put three saturated hues on one screen at once.
val AmoledBlack = Color(0xFF0B0D10)
val Panel = Color(0xFF15181D)
val PanelRaised = Color(0xFF1E222A)
val Hairline = Color(0xFF262B33)
val TextBright = Color(0xFFE7E9EC)
val TextDim = Color(0xFF8E949E)
val TierLocal = Color(0xFF3FBF9F)
val TierBroadcast = Color(0xFF5A8DEF)
val TierPrivate = Color(0xFF9E7BEC)
val TrustAmber = Color(0xFFE0A93E)
val PanicRed = Color(0xFFE4443A)

/** Incoming bubble. Neutral: the sender's identity is the conversation, not the colour. */
val BubbleIn = Color(0xFF20242B)
/** Outgoing bubble. One accent everywhere, like Signal/WhatsApp. */
val BubbleOut = Color(0xFF2C5FD6)
val OnBubbleOut = Color(0xFFF2F4F8)
/** Unread pill / active affordances. */
val Accent = Color(0xFF5A8DEF)

// Sans-serif text styles. Monospace is kept below for the debug drawer, where columns of
// hex and counters genuinely need it — not for chat.
fun sansTitle(color: Color = TextBright) = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontSize = 17.sp,
    fontWeight = FontWeight.SemiBold,
    color = color
)

fun sansRowTitle(color: Color = TextBright) = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontSize = 16.sp,
    fontWeight = FontWeight.Medium,
    color = color
)

fun sansBody(color: Color = TextBright) = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontSize = 15.5.sp,
    lineHeight = 21.sp,
    color = color
)

fun sansSub(color: Color = TextDim) = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontSize = 13.5.sp,
    color = color
)

fun sansMeta(color: Color = TextDim) = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontSize = 11.5.sp,
    color = color
)

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
    primary = Accent,
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
