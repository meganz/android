package mega.privacy.android.app.presentation.fileinfo.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.R
import mega.privacy.android.shared.original.core.ui.controls.buttons.MegaButtonWithIcon
import mega.privacy.android.shared.original.core.ui.preview.CombinedTextAndThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.extensions.subtitle2medium
import mega.privacy.android.shared.original.core.ui.theme.extensions.textColorPrimary
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme


/**
 * Shows the location info of the node
 */
@Composable
internal fun LocationInfoView(
    location: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) = Row(
    modifier = modifier
        .fillMaxWidth()
        .clickable(onClick = onClick)
) {
    Column(
        modifier = Modifier
            .weight(1f)
    ) {
        Text(
            text = stringResource(id = R.string.file_properties_info_location),
            style = MaterialTheme.typography.subtitle2medium.copy(color = MaterialTheme.colors.textColorPrimary),
            modifier = Modifier
                .fillMaxWidth()
                .testTag(TEST_TAG_LOCATION_TITLE)
        )
        Text(
            text = location,
            style = MaterialTheme.typography.subtitle2medium.copy(color = MaterialTheme.colors.secondary),
            modifier = Modifier
                .testTag(TEST_TAG_LOCATION)
        )
        Spacer(modifier = Modifier.height(verticalSpace.dp))
    }
    Icon(
        painter = painterResource(mega.privacy.android.icon.pack.R.drawable.ic_chevron_right_medium_regular_outline),
        modifier = Modifier.padding(top = 4.dp),
        tint = MaterialTheme.colors.onPrimary,
        contentDescription = null,
    )
}

/**
 * Preview for [LocationInfoView]
 */
@CombinedTextAndThemePreviews
@Composable
private fun LocationInfoViewPreview() {
    var counter by remember { mutableStateOf(0) }
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        LocationInfoView(
            modifier = Modifier.systemBarsPadding(),
            location = "CloudDrive $counter", onClick = {
            counter++
        })
    }
}