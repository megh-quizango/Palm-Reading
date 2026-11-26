package app.palm.core.designsystem

import android.provider.Settings
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/** Nocturne values that have no Material 3 slot. Every value comes from the generated NocturneTokens.kt. */
@Immutable
data class NocturneExtras(
    val palmHeart: Color = NocturneColors.PalmHeart,
    val palmHead: Color = NocturneColors.PalmHead,
    val palmLife: Color = NocturneColors.PalmLife,
    val palmFate: Color = NocturneColors.PalmFate,
    val palmSun: Color = NocturneColors.PalmSun,
    val statusSuccess: Color = NocturneColors.StatusSuccess,
    val statusWarning: Color = NocturneColors.StatusWarning,
    val bgDeep: Color = NocturneColors.BgDeep,
    val bgRaised: Color = NocturneColors.BgRaised,
    val bgCamera: Color = NocturneColors.BgCamera,
    val focusRing: Color = NocturneColors.FocusRing,
    val adOutline: Color = NocturneColors.AdOutline,
    val gradients: NocturneGradients = NocturneGradients,
    val glows: NocturneShadows = NocturneShadows,
    val durations: NocturneDurations = NocturneDurations,
    val easings: NocturneEasings = NocturneEasings,
    val reduceMotion: Boolean = false,
)

val LocalNocturne = staticCompositionLocalOf<NocturneExtras> { error("NocturneTheme is not in the composition") }

private val NocturneColorScheme = darkColorScheme(
    primary = NocturneSemantic.Primary,
    onPrimary = NocturneSemantic.OnPrimary,
    secondary = NocturneSemantic.Secondary,
    onSecondary = NocturneSemantic.OnSecondary,
    tertiary = NocturneSemantic.Accent,
    onTertiary = NocturneSemantic.OnAccent,
    background = NocturneSemantic.Background,
    onBackground = NocturneSemantic.Foreground,
    surface = NocturneSemantic.Background,
    onSurface = NocturneSemantic.Foreground,
    surfaceContainer = NocturneSemantic.Card,
    surfaceVariant = NocturneSemantic.Muted,
    onSurfaceVariant = NocturneSemantic.MutedForeground,
    outline = NocturneSemantic.Border,
    error = NocturneSemantic.Destructive,
    onError = NocturneSemantic.OnDestructive,
    scrim = NocturneColors.SurfaceScrim,
)

private val NocturneShapes = Shapes(
    extraSmall = RoundedCornerShape(NocturneRadius.Xs),
    small = RoundedCornerShape(NocturneRadius.Sm),
    medium = RoundedCornerShape(NocturneRadius.Md),
    large = RoundedCornerShape(NocturneRadius.Lg),
    extraLarge = RoundedCornerShape(NocturneRadius.Xl),
)

/** Dark-only theme (no dynamic colour). */
@Composable
fun NocturneTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    // Unset on some phones, so the default must be 1f (animations on)
    val reduceMotion = remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
    val extras = remember(reduceMotion) { NocturneExtras(reduceMotion = reduceMotion) }
    MaterialTheme(colorScheme = NocturneColorScheme, shapes = NocturneShapes) {
        CompositionLocalProvider(LocalNocturne provides extras, content = content)
    }
}
