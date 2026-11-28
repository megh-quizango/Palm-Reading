package app.palm.feature.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.unit.dp
import app.palm.R
import app.palm.contract.HandSide
import app.palm.core.designsystem.LocalNocturne
import app.palm.core.designsystem.NocturneBorderWidth
import app.palm.core.designsystem.NocturneColors
import app.palm.core.designsystem.NocturneRadius
import app.palm.core.designsystem.NocturneSpace
import app.palm.core.designsystem.components.NocturneCard
import app.palm.core.designsystem.devanagariSafe
import app.palm.feature.handselect.handLabel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** The newest saved reading for Home (D-19); [thumbnail] null while decoding or when the photo is missing. */
data class LastReading(val id: String, val hand: HandSide, val createdAt: Long, val thumbnail: ImageBitmap?)

/** Home "Your last reading" (UI-SPEC section 5): one TalkBack node, opens the reading. */
@Composable
fun LastReadingCard(reading: LastReading, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val type = LocalNocturne.current.type
    val locale = LocalConfiguration.current.locales[0]
    val date = remember(reading.createdAt, locale) {
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
            .format(Instant.ofEpochMilli(reading.createdAt).atZone(ZoneId.systemDefault()).toLocalDate())
    }
    val hand = stringResource(handLabel(reading.hand))
    val description = stringResource(R.string.home_last_reading_cd, hand, date)
    val discStroke = remember { Brush.linearGradient(listOf(NocturneColors.AccentGold, NocturneColors.BrandLink)) }
    NocturneCard(
        modifier
            .fillMaxWidth()
            .heightIn(min = 88.dp)
            .clearAndSetSemantics {
                contentDescription = description
                role = Role.Button
                onClick { onClick(); true }
            },
        onClick = onClick,
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(NocturneSpace.S16),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val thumbShape = RoundedCornerShape(NocturneRadius.Md)
            val thumb = reading.thumbnail
            if (thumb != null) {
                Image(thumb, null, Modifier.size(56.dp).clip(thumbShape), contentScale = ContentScale.Crop)
            } else {
                Box(Modifier.size(56.dp).background(NocturneColors.SurfaceCardHi, thumbShape), contentAlignment = Alignment.Center) {
                    Icon(painterResource(R.drawable.ic_hand), null, Modifier.size(24.dp), tint = NocturneColors.TextMuted)
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(NocturneSpace.S4)) {
                Text(stringResource(R.string.home_last_reading_overline), style = type.overline.devanagariSafe(), color = NocturneColors.AccentGold)
                Text(stringResource(R.string.feature_palm), style = type.titleM, color = NocturneColors.TextPrimary)
                Text(stringResource(R.string.home_last_reading_meta, hand, date), style = type.caption, color = NocturneColors.TextMuted)
            }
            Box(
                Modifier
                    .size(32.dp)
                    .background(NocturneColors.SurfaceCardHi, CircleShape)
                    .border(NocturneBorderWidth, discStroke, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(R.drawable.ic_arrow), null, Modifier.size(16.dp), tint = NocturneColors.AccentGold)
            }
        }
    }
}
