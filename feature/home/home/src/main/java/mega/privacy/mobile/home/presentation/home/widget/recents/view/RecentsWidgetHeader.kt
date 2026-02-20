package mega.privacy.mobile.home.presentation.home.widget.recents.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.IconColor
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.shared.resources.R as sharedR

@Composable
internal fun RecentsWidgetHeader(
    modifier: Modifier = Modifier,
    onOptionsClicked: () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MegaText(
            stringResource(sharedR.string.section_recents),
            style = AppTheme.typography.titleMedium.copy(
                fontSize = 18.sp
            ),
            modifier = Modifier
                .weight(1f)
                .testTag(TITLE_TEST_TAG)
        )
        Box(
            modifier = Modifier
                .size(16.dp)
                .wrapContentSize(unbounded = true, align = Alignment.Center)
                .size(48.dp)
                .clickable { onOptionsClicked() }
                .testTag(RECENTS_MENU_TEST_TAG),
            contentAlignment = Alignment.Center
        ) {
            MegaIcon(
                imageVector = IconPack.Medium.Thin.Outline.MoreVertical,
                contentDescription = "3 dots",
                tint = IconColor.Secondary,
                modifier = Modifier
                    .size(16.dp)
            )
        }
    }
}

internal const val TITLE_TEST_TAG = "recents_widget:title"
internal const val RECENTS_MENU_TEST_TAG = "recents_widget:menu"

@CombinedThemePreviews
@Composable
private fun RecentsWidgetHeaderPreview() {
    AndroidThemeForPreviews {
        RecentsWidgetHeader(
            onOptionsClicked = {}
        )
    }
}

