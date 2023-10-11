package mega.privacy.android.core.ui.controls.lists

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import mega.privacy.android.core.ui.controls.controlssliders.MegaRadioButton
import mega.privacy.android.core.ui.preview.BooleanProvider
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.MegaTheme

/**
 * List 1 line item, variant "Settings item with RadioButton"
 */
@Composable
fun SettingsItemWithRadioButton(
    title: String,
    selected: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clickable(enabled = onClick != null, onClick = { onClick?.invoke() })
            .padding(start = 12.dp, end = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)

    ) {
        MegaRadioButton(selected = selected, onClick = onClick)
        Text(
            text = title,
            color = MegaTheme.colors.text.primary,
            textAlign = TextAlign.Start,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.subtitle1,
        )
    }
}

@CombinedThemePreviews
@Composable
private fun SettingsItemPreview(
    @PreviewParameter(BooleanProvider::class) initialValue: Boolean,
) {
    var selected by remember { mutableStateOf(initialValue) }
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        SettingsItemWithRadioButton(
            title = "Settings name",
            selected = selected,
            onClick = { selected = !selected })
    }
}