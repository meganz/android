package mega.privacy.android.shared.sync.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText
import mega.privacy.android.shared.original.core.ui.theme.values.TextColor

/**
 * A [Composable] which displays an empty state
 *
 * @param iconId            [DrawableRes] ID to query the image file from.
 * @param iconSize          Size of the icon square in [Dp].
 * @param iconDescription   [String] used by accessibility services to describe what this image represents.
 * @param textId            [StringRes] ID of the text to display.
 * @param testTag           Tag to allow modified element to be found in tests.
 */
@Composable
fun SyncEmptyState(
    @DrawableRes iconId: Int,
    iconSize: Dp,
    iconDescription: String,
    @StringRes textId: Int,
    testTag: String,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = iconId),
            contentDescription = iconDescription,
            modifier = Modifier
                .size(size = iconSize)
                .padding(bottom = 8.dp)
        )
        MegaText(
            text = stringResource(id = textId),
            textColor = TextColor.Primary,
            style = MaterialTheme.typography.subtitle2,
        )
    }
}
