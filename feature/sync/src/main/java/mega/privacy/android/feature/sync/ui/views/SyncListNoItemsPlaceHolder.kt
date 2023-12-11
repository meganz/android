package mega.privacy.android.feature.sync.ui.views

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import mega.privacy.android.shared.theme.MegaAppTheme
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.extensions.textColorSecondary
import mega.privacy.android.feature.sync.R

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
                .testTag(TAG_SYNC_LIST_SCREEN_NO_ITEMS)
        )
        Text(
            text = placeholderText,
            modifier = Modifier
                .padding(top = 8.dp),
            style = MaterialTheme.typography.subtitle2.copy(color = MaterialTheme.colors.textColorSecondary),
        )
    }
}

@CombinedThemePreviews
@Composable
private fun SyncListNoItemsPlaceholderPreview() {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        SyncListNoItemsPlaceHolder(
            "No Stalled Issues",
            R.drawable.ic_no_stalled_issues
        )
    }
}

internal const val TAG_SYNC_LIST_SCREEN_NO_ITEMS = "sync_list_screen_no_items"
