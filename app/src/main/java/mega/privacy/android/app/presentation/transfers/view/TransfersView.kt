package mega.privacy.android.app.presentation.transfers.view

import android.content.res.Configuration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.collections.immutable.persistentListOf
import mega.privacy.android.app.R
import mega.privacy.android.shared.original.core.ui.controls.tab.Tabs
import mega.privacy.android.shared.original.core.ui.controls.tab.TextCell
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun TransfersView(
    modifier: Modifier = Modifier,
) = Column(modifier = modifier.semantics {
    testTagsAsResourceId = true
}) {
    Tabs(
        cells = persistentListOf(
            TextCell(
                text = stringResource(id = R.string.title_tab_in_progress_transfers),
                tag = TEST_TAG_IN_PROGRESS_TAB,
            ) { InProgressTransfersView() },
            TextCell(
                text = stringResource(id = R.string.title_tab_completed_transfers),
                tag = TEST_TAG_COMPLETED_TAB,
            ) { CompletedTransfersView() }
        )
    )
}

@Composable
internal fun InProgressTransfersView() {

}

@Composable
internal fun CompletedTransfersView() {

}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, name = "DarkTransfersViewPreview")
@Composable
private fun TransfersViewPreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        TransfersView()
    }
}

/**
 * Tag for the in progress tab
 */
const val TEST_TAG_IN_PROGRESS_TAB = "transfers_view:tab_in_progress"
private const val IN_PROGRESS_TAB_INDEX = 0

/**
 * Tag for the completed tab
 */
const val TEST_TAG_COMPLETED_TAB = "transfers_view:tab_completed"
private const val COMPLETED_TAB_INDEX = 1