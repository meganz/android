package mega.privacy.android.app.presentation.fileinfo.view

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.R
import mega.privacy.android.core.ui.preview.CombinedTextAndThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme

/**
 * Show a clickable button with the total amount of versions of this file
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun FileVersionsView(
    versions: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    textStartPadding: Dp = 72.dp,
) = Row(
    modifier = modifier
        .fillMaxWidth()
        .height(48.dp),
    verticalAlignment = Alignment.CenterVertically,
) {
    Image(
        modifier = Modifier
            .sizeIn(minWidth = textStartPadding)
            .padding(start = 16.dp),
        painter = painterResource(id = R.drawable.ic_g_version),
        alignment = Alignment.CenterStart,
        contentDescription = "versions icon"
    )
    TextButton(
        modifier = Modifier
            .widthIn(min = 100.dp)
            .testTag(TEST_TAG_VERSIONS_BUTTON),
        onClick = onClick,
        border = BorderStroke(1.dp, MaterialTheme.colors.secondary)
    ) {
        Text(
            text = pluralStringResource(
                id = R.plurals.number_of_versions,
                count = versions,
                versions
            ),
            style = MaterialTheme.typography.subtitle2.copy(color = MaterialTheme.colors.secondary),
        )
    }
}

/**
 * Preview for [FileVersionsView]
 */
@CombinedTextAndThemePreviews
@Composable
private fun FileVersionsPreview() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        FileVersionsView(versions = 5,
            onClick = {})
    }
}