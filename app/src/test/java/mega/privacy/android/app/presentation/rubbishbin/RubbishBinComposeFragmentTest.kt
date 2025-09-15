package mega.privacy.android.app.presentation.rubbishbin

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.rubbishbin.model.LegacyRubbishBinState
import mega.privacy.android.app.presentation.rubbishbin.view.RubbishBinComposeView
import mega.privacy.android.app.presentation.view.NODES_EMPTY_VIEW_VISIBLE
import mega.privacy.android.core.nodecomponents.mapper.FileTypeIconMapper
import mega.privacy.android.icon.pack.R as iconPackR
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock

@RunWith(AndroidJUnit4::class)
class RubbishBinComposeFragmentTest {
    @get:Rule
    var composeRule = createComposeRule()
    private val fileTypeIconMapper: FileTypeIconMapper = mock()

    @Test
    fun `test that NodesView not displayed when list is empty`() {
        composeRule.setContent {
            RubbishBinComposeView(
                uiState = LegacyRubbishBinState(
                    isLoading = false,
                ),
                onMenuClick = {},
                onItemClicked = {},
                onLongClick = {},
                onSortOrderClick = {},
                onChangeViewTypeClick = {},
                sortOrder = "Name",
                emptyState = Pair(iconPackR.drawable.ic_empty_trash_glass, R.string.context_empty_rubbish_bin),
                onLinkClicked = {},
                onDisputeTakeDownClicked = {},
                fileTypeIconMapper = fileTypeIconMapper,
                onResetScrollPositionEventConsumed = { }
            )
        }
        composeRule.onNodeWithTag(NODES_EMPTY_VIEW_VISIBLE).assertIsDisplayed()
    }
}