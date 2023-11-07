package mega.privacy.android.app.presentation.fileinfo.view

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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.R
import mega.privacy.android.core.ui.preview.BooleanProvider
import mega.privacy.android.core.ui.preview.CombinedTextAndThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.extensions.textColorPrimary
import mega.privacy.android.legacy.core.ui.controls.controlssliders.MegaSwitch

/**
 * Available offline row
 */
@Composable
internal fun AvailableOfflineView(
    enabled: Boolean,
    available: Boolean,
    onCheckChanged: (checked: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) = Row(
    modifier = modifier
        .fillMaxWidth()
        .height(48.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
) {
    Text(
        text = stringResource(id = R.string.file_properties_available_offline),
        style = MaterialTheme.typography.subtitle1.copy(color = MaterialTheme.colors.textColorPrimary),
        modifier = Modifier.alpha(if (enabled) 1f else 0.26f)
    )
    MegaSwitch(
        checked = available,
        enabled = enabled,
        onCheckedChange = onCheckChanged,
        modifier = Modifier.testTag(TEST_TAG_AVAILABLE_OFFLINE_SWITCH)
    )
}

/**
 * Preview enabled
 */
@CombinedTextAndThemePreviews
@Composable
private fun AvailableOfflinePreview(
    @PreviewParameter(BooleanProvider::class) initialValue: Boolean,
) = AndroidTheme(isDark = isSystemInDarkTheme()) {
    var available by remember { mutableStateOf(initialValue) }
    AvailableOfflineView(
        modifier = Modifier.padding(horizontal = 16.dp),
        enabled = true, available = available, onCheckChanged = { available = !available },
    )
}