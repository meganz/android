package mega.privacy.android.feature.sync.ui.views

import mega.privacy.android.icon.pack.R as iconPackR
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import mega.privacy.android.shared.original.core.ui.theme.values.TextColor
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme

@Composable
internal fun SyncListNoItemsPlaceHolder(
    placeholderText: String,
    @DrawableRes placeholderIcon: Int,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painterResource(placeholderIcon),
            contentDescription = null,
            modifier = Modifier
                .size(128.dp)
                .testTag(TAG_SYNC_LIST_SCREEN_NO_ITEMS)
        )
        MegaText(
            text = placeholderText,
            textColor = TextColor.Secondary,
            modifier = Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.subtitle2,
        )
    }
}

@CombinedThemePreviews
@Composable
private fun SyncListNoItemsPlaceholderPreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        SyncListNoItemsPlaceHolder(
            placeholderText = "No issues",
            placeholderIcon = iconPackR.drawable.ic_check_circle_color,
        )
    }
}

internal const val TAG_SYNC_LIST_SCREEN_NO_ITEMS = "sync_list_screen_no_items"
