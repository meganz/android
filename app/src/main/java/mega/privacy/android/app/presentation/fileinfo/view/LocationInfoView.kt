package mega.privacy.android.app.presentation.fileinfo.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.R
import mega.privacy.android.shared.theme.MegaAppTheme
import mega.privacy.android.core.ui.preview.CombinedTextAndThemePreviews
import mega.privacy.android.core.ui.theme.extensions.subtitle2medium
import mega.privacy.android.core.ui.theme.extensions.textColorPrimary


/**
 * Shows the location info of the node
 */
@Composable
internal fun LocationInfoView(
    location: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) = Column(
    modifier = modifier
        .fillMaxWidth()
) {
    Text(
        text = stringResource(id = R.string.file_properties_info_location),
        style = MaterialTheme.typography.subtitle2medium.copy(color = MaterialTheme.colors.textColorPrimary),
    )
    Text(
        text = location,
        style = MaterialTheme.typography.subtitle2medium.copy(color = MaterialTheme.colors.secondary),
        modifier = Modifier
            .clickable(onClick = onClick)
            .testTag(TEST_TAG_LOCATION)
    )
    Spacer(modifier = Modifier.height(verticalSpace.dp))
}

/**
 * Preview for [LocationInfoView]
 */
@CombinedTextAndThemePreviews
@Composable
private fun LocationInfoViewPreview() {
    var counter by remember { mutableStateOf(0) }
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        LocationInfoView(location = "CloudDrive $counter", onClick = {
            counter++
        })
    }
}