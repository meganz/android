package mega.privacy.android.app.presentation.transfers.view.completed

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import mega.privacy.android.domain.entity.transfer.CompletedTransfer
import mega.privacy.android.shared.resources.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock

@RunWith(AndroidJUnit4::class)
class CompletedTransfersViewTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `test that view is displayed correctly if there are no completed transfers`() {
        initComposeTestRule()
        with(composeTestRule) {
            val emptyText = activity.getString(R.string.transfers_no_completed_transfers_empty_text)
                .replace("[A]", "").replace("[/A]", "")

            onNodeWithTag(TEST_TAG_COMPLETED_TRANSFERS_VIEW).assertDoesNotExist()
            onNodeWithTag(TEST_TAG_COMPLETED_TRANSFERS_EMPTY_VIEW).assertIsDisplayed()
            onNodeWithText(emptyText).assertIsDisplayed()
        }
    }

    private fun initComposeTestRule(
        completedTransfers: ImmutableList<CompletedTransfer> = emptyList<CompletedTransfer>().toImmutableList(),
    ) {
        composeTestRule.setContent {
            CompletedTransfersView(
                completedTransfers = completedTransfers,
                lazyListState = mock(),
                selectedCompletedTransfersIds = mock(),
                onCompletedTransferSelected = mock(),
                onViewInFolder = {},
                onOpenWith = {},
                onShareLink = {},
                onClearTransfer = {},
                modifier = mock(),
            )
        }
    }
}