package test.mega.privacy.android.app.presentation.rubbishbin

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.rubbishbin.model.RubbishBinState
import mega.privacy.android.app.presentation.rubbishbin.view.RubbishBinComposeView
import mega.privacy.android.app.presentation.view.NODES_EMPTY_VIEW_VISIBLE
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RubbishBinComposeFragmentTest {
    @get:Rule
    var composeRule = createComposeRule()

    @Test
    fun `test that NodesView not displayed when list is empty`() {
        composeRule.setContent {
            RubbishBinComposeView(
                uiState = RubbishBinState(),
                onMenuClick = {},
                onItemClicked = {},
                onLongClick = {},
                onSortOrderClick = {},
                onChangeViewTypeClick = {},
                sortOrder = "Name",
                emptyState = Pair(R.drawable.rubbish_bin_empty, R.string.context_empty_rubbish_bin),
                onLinkClicked = {},
                onDisputeTakeDownClicked = {}
            )
        }
        composeRule.onNodeWithTag(NODES_EMPTY_VIEW_VISIBLE).assertIsDisplayed()
    }
}