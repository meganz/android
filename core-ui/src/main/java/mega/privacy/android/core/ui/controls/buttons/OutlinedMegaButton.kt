package mega.privacy.android.core.ui.controls.buttons

import androidx.appcompat.R
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import mega.privacy.android.core.ui.preview.CombinedTextAndThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.extensions.subtitle2medium

/**
 * Plain button with an outlined border
 * @param textId the text resource for this button
 * @param onClick lambda to receive clicks on this button
 * @param modifier
 * @param enabled
 */
@Composable
fun OutlinedMegaButton(
    textId: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) = OutlinedMegaButton(
    text = stringResource(id = textId),
    onClick = onClick,
    modifier = modifier,
    enabled = enabled
)

/**
 * Plain button with an outlined border
 * @param text the text for this button
 * @param onClick lambda to receive clicks on this button
 * @param modifier
 * @param enabled
 */
@Composable
fun OutlinedMegaButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val color =
        if (enabled) MaterialTheme.colors.secondary else MaterialTheme.colors.secondary.copy(alpha = 0.38f)
    TextButton(
        modifier = modifier
            .widthIn(min = 100.dp),
        enabled = enabled,
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, color)
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 0.dp),
            text = text,
            style = MaterialTheme.typography.subtitle2medium.copy(color = color),
        )
    }
}


@CombinedTextAndThemePreviews
@Composable
private fun PreviewOutlinedMegaButton() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            listOf(true, false).forEach {
                Text(
                    style = MaterialTheme.typography.caption.copy(color = MaterialTheme.colors.onSurface),
                    text = if (it) "Enabled" else "Disabled",
                )
                var count by remember { mutableStateOf(0) }
                OutlinedMegaButton(
                    text = stringResource(id = R.string.search_menu_title) + if (count > 0) " $count" else "",
                    onClick = { count++ },
                    enabled = it
                )
            }
        }
    }
}