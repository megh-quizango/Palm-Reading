package app.palm.core.designsystem

import android.content.Context
import android.content.res.Resources
import android.graphics.Typeface
import android.graphics.fonts.FontStyle
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import app.palm.R
import java.util.concurrent.ConcurrentHashMap

/**
 * Bundled static fonts (D-05) with glyph-level Devanagari fallback (D-06): Latin glyphs come from
 * Manrope / Cormorant Garamond, anything they lack from Noto Sans / Serif Devanagari of the same weight.
 * No downloadable fonts: everything is in res/font.
 */
object NocturneFonts {
    private val cache = ConcurrentHashMap<Pair<NocturneFontRole, Int>, FontFamily>()

    fun family(context: Context, role: NocturneFontRole, weight: Int): FontFamily =
        cache.getOrPut(role to weight) { build(context.applicationContext.resources, role, weight) }

    private fun build(res: Resources, role: NocturneFontRole, weight: Int): FontFamily {
        val (latin, devanagari) = fontRes(role, weight)
        return if (Build.VERSION.SDK_INT >= 29) {
            FontFamily(fallbackTypeface(res, latin, devanagari, weight, if (role == NocturneFontRole.Display) "serif" else "sans-serif"))
        } else {
            // API 26-28: no CustomFallbackBuilder, Devanagari falls back to the system font
            FontFamily(Font(latin, FontWeight(weight)))
        }
    }

    private fun fontRes(role: NocturneFontRole, weight: Int): Pair<Int, Int> = when (role) {
        NocturneFontRole.Display -> when (weight) {
            600 -> R.font.cormorant_garamond_semibold to R.font.noto_serif_devanagari_semibold
            else -> error("no bundled Display font for weight $weight")
        }
        NocturneFontRole.Ui -> when (weight) {
            400 -> R.font.manrope_regular to R.font.noto_sans_devanagari_regular
            500 -> R.font.manrope_medium to R.font.noto_sans_devanagari_medium
            600 -> R.font.manrope_semibold to R.font.noto_sans_devanagari_semibold
            700 -> R.font.manrope_bold to R.font.noto_sans_devanagari_bold
            else -> error("no bundled Ui font for weight $weight")
        }
    }

    @RequiresApi(29)
    private fun fallbackTypeface(res: Resources, latin: Int, devanagari: Int, weight: Int, system: String): Typeface {
        fun family(id: Int) = android.graphics.fonts.FontFamily.Builder(
            android.graphics.fonts.Font.Builder(res, id).setWeight(weight).build(),
        ).build()
        return Typeface.CustomFallbackBuilder(family(latin))
            .addCustomFallback(family(devanagari))
            .setSystemFallback(system)
            .setStyle(FontStyle(weight, FontStyle.FONT_SLANT_UPRIGHT))
            .build()
    }
}
