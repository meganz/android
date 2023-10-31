package mega.privacy.android.feature.sync.ui.synclist.stalledissues

import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.extensions.textColorSecondary
import mega.privacy.android.feature.sync.R
import mega.privacy.android.feature.sync.ui.synclist.SyncListScreen
import mega.privacy.android.feature.sync.ui.synclist.TAG_SYNC_LIST_SCREEN_NO_ITEMS

@Composable
internal fun NoStalledIssuesPlaceholder(modifier: Modifier) {
    Column(
        modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painterResource(R.drawable.no_stalled_issues_temporary_image),
            contentDescription = null,
            modifier = Modifier
                .testTag(TAG_SYNC_LIST_SCREEN_NO_ITEMS)
        )
        Text(
            text = "No stalled issues",
            modifier = Modifier
                .padding(top = 8.dp),
            style = MaterialTheme.typography.subtitle2.copy(color = MaterialTheme.colors.textColorSecondary),
        )
    }
}

@CombinedThemePreviews
@Composable
private fun PreviewEmptyScreen() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        NoStalledIssuesPlaceholder(
            Modifier
        )
    }
}