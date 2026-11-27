package app.palm.core.designsystem.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import app.palm.R
import app.palm.core.designsystem.LocalNocturne
import app.palm.core.designsystem.NocturneBorderWidth
import app.palm.core.designsystem.NocturneColors
import app.palm.core.designsystem.NocturneRadius
import app.palm.core.designsystem.NocturneSemantic
import app.palm.core.designsystem.NocturneSize
import app.palm.core.designsystem.NocturneSpace

private val FieldShape = RoundedCornerShape(NocturneRadius.Md)
private val FieldMinHeight = 56.dp
private const val DISABLED_ALPHA = 0.38f
private val HairlineBorder = NocturneColors.BorderHairline.copy(alpha = 0.4f)

@Composable
fun nocturneTextFieldColors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedTextColor = NocturneSemantic.Foreground,
    unfocusedTextColor = NocturneSemantic.Foreground,
    errorTextColor = NocturneSemantic.Foreground,
    focusedContainerColor = NocturneSemantic.Card,
    unfocusedContainerColor = NocturneSemantic.Card,
    errorContainerColor = NocturneSemantic.Card,
    cursorColor = NocturneColors.BrandLink,
    errorCursorColor = NocturneColors.BrandLink,
    focusedBorderColor = NocturneColors.BrandLink,
    unfocusedBorderColor = HairlineBorder,
    errorBorderColor = NocturneColors.StatusError,
    focusedPlaceholderColor = NocturneSemantic.MutedForeground,
    unfocusedPlaceholderColor = NocturneSemantic.MutedForeground,
    errorPlaceholderColor = NocturneSemantic.MutedForeground,
    focusedSupportingTextColor = NocturneSemantic.MutedForeground,
    unfocusedSupportingTextColor = NocturneSemantic.MutedForeground,
    errorSupportingTextColor = NocturneColors.StatusError,
)

@Composable
private fun FieldLabel(label: String) {
    Text(label, style = LocalNocturne.current.type.labelM, color = NocturneSemantic.MutedForeground)
    Spacer(Modifier.height(NocturneSpace.S8))
}

/** M3 outlined field with the label above and helper or error below (UI-SPEC S04). */
@Composable
fun NocturneTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    helper: String? = null,
    error: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    textStyle: TextStyle = LocalNocturne.current.type.bodyL,
) {
    val type = LocalNocturne.current.type
    Column(modifier) {
        FieldLabel(label)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = FieldMinHeight)
                .semantics { if (error != null) error(error) },
            textStyle = textStyle.copy(color = NocturneSemantic.Foreground),
            placeholder = placeholder?.let { { Text(it, style = type.bodyL) } },
            supportingText = when {
                error != null -> {
                    {
                        Row(horizontalArrangement = Arrangement.spacedBy(NocturneSpace.S4), verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painterResource(R.drawable.ic_alert),
                                contentDescription = null,
                                colorFilter = ColorFilter.tint(NocturneColors.StatusError),
                                modifier = Modifier.size(16.dp),
                            )
                            Text(error, style = type.caption)
                        }
                    }
                }
                helper != null -> { { Text(helper, style = type.caption) } }
                else -> null
            },
            isError = error != null,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            singleLine = true,
            shape = FieldShape,
            colors = nocturneTextFieldColors(),
        )
    }
}

/** A tappable field that opens a picker (UI-SPEC S05 date and time). */
@Composable
fun NocturneSelectField(
    label: String,
    value: String?,
    placeholder: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val type = LocalNocturne.current.type
    Column(modifier.alpha(if (enabled) 1f else DISABLED_ALPHA)) {
        FieldLabel(label)
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = FieldMinHeight)
                .background(NocturneSemantic.Card, FieldShape)
                .border(NocturneBorderWidth, HairlineBorder, FieldShape)
                .clip(FieldShape)
                .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
                .padding(horizontal = NocturneSpace.S16, vertical = NocturneSpace.S8),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (value != null) {
                Text(value, style = type.titleM, color = NocturneSemantic.Foreground, modifier = Modifier.weight(1f))
            } else {
                Text(placeholder, style = type.bodyL, color = NocturneSemantic.MutedForeground, modifier = Modifier.weight(1f))
            }
            Image(
                painterResource(R.drawable.ic_chevron_down),
                contentDescription = null,
                colorFilter = ColorFilter.tint(NocturneSemantic.MutedForeground),
                modifier = Modifier.size(NocturneSize.Icon),
            )
        }
    }
}
