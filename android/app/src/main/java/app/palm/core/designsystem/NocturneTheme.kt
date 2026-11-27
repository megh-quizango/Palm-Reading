package app.palm.core.designsystem

import android.provider.Settings
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/** The 11 token text styles by token name; [byName] keeps token order ("Display XL" .. "Overline"). */
@Immutable
data class NocturneTextStyles(
    val displayXl: TextStyle,
    val displayL: TextStyle,
    val displayM: TextStyle,
    val displayS: TextStyle,
    val titleM: TextStyle,
    val bodyL: TextStyle,
    val bodyM: TextStyle,
    val labelL: TextStyle,
    val labelM: TextStyle,
    val caption: TextStyle,
    val overline: TextStyle,
    val byName: Map<String, TextStyle>,
)

/** D-07: font-metric line height, so Devanagari ascenders and matras are never clipped by token heights. */
fun TextStyle.devanagariSafe(): TextStyle = copy(lineHeight = TextUnit.Unspecified)

private fun nocturneTextStyles(style: (NocturneTypeSpec) -> TextStyle) = with(NocturneTypeSpecs) {
    NocturneTextStyles(
        displayXl = style(DisplayXl),
        displayL = style(DisplayL),
        displayM = style(DisplayM),
        displayS = style(DisplayS),
        titleM = style(TitleM),
        bodyL = style(BodyL),
        bodyM = style(BodyM),
        labelL = style(LabelL),
        labelM = style(LabelM),
        caption = style(Caption),
        overline = style(Overline),
        byName = ByName.mapValues { style(it.value) },
    )
}

private fun NocturneTextStyles.toTypography() = Typography(
    displayLarge = displayXl,
    displayMedium = displayL,
    displaySmall = displayM,
    headlineSmall = displayS,
    titleMedium = titleM,
    bodyLarge = bodyL,
    bodyMedium = bodyM,
    labelLarge = labelL,
    labelMedium = labelM,
    bodySmall = caption,
    labelSmall = overline,
)

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
    val type: NocturneTextStyles,
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
    // The AppCompat per-app locale (D-06) recreates the activity, so this follows the chosen language
    val devanagari = LocalConfiguration.current.locales[0].language == "hi"
    val type = remember(devanagari) {
        nocturneTextStyles { spec ->
            val style = TextStyle(
                fontFamily = NocturneFonts.family(context, spec.role, spec.weight),
                fontWeight = FontWeight(spec.weight),
                fontSize = spec.sizeSp.sp,
                lineHeight = spec.lineHeightSp.sp,
                letterSpacing = spec.letterSpacingEm.em,
                fontSynthesis = FontSynthesis.None,
            )
            if (devanagari) style.devanagariSafe() else style
        }
    }
    val extras = remember(reduceMotion, type) { NocturneExtras(reduceMotion = reduceMotion, type = type) }
    MaterialTheme(colorScheme = NocturneColorScheme, shapes = NocturneShapes, typography = type.toTypography()) {
        CompositionLocalProvider(LocalNocturne provides extras, content = content)
    }
}
