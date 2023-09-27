package mega.privacy.android.feature.sync.ui.synclist

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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.extensions.textColorPrimary
import mega.privacy.android.core.ui.theme.extensions.textColorSecondary
import mega.privacy.android.feature.sync.R

@Composable
internal fun NoItemsPlaceholder(modifier: Modifier) {
    Column(
        modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painterResource(R.drawable.no_syncs_placeholder),
            contentDescription = null,
            modifier = Modifier
                .testTag(TAG_SYNC_LIST_SCREEN_NO_ITEMS)
        )
        val annotatedString = buildAnnotatedString {
            append("No")
            withStyle(SpanStyle(color = MaterialTheme.colors.textColorPrimary)) {
                append(" Syncs")
            }
        }
        Text(
            text = annotatedString,
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
        SyncListScreen(
            stalledIssuesCount = 3,
            addFolderClicked = {}
        )
    }
}