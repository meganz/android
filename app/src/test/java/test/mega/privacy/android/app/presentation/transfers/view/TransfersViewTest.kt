package test.mega.privacy.android.app.presentation.transfers.view

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.presentation.transfers.model.TransferMenuAction.Companion.TEST_TAG_PAUSE_ACTION
import mega.privacy.android.app.presentation.transfers.model.TransferMenuAction.Companion.TEST_TAG_RESUME_ACTION
import mega.privacy.android.app.presentation.transfers.model.TransfersUiState
import mega.privacy.android.app.presentation.transfers.view.IN_PROGRESS_TAB_INDEX
import mega.privacy.android.app.presentation.transfers.view.TransfersView
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class TransfersViewTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val onPlayPauseTransfer: (Int) -> Unit = mock()
    private val onResumeTransfers: () -> Unit = mock()
    private val onPauseTransfers: () -> Unit = mock()

    @Test
    fun `test that pause TransferMenuAction is displayed if transfers are not already paused and click action invokes correctly`() {
        initComposeTestRule(uiState = TransfersUiState(areTransfersPaused = false))
        with(composeTestRule) {
            onNodeWithTag(TEST_TAG_PAUSE_ACTION).apply {
                assertIsDisplayed()
                performClick()
                verify(onPauseTransfers).invoke()
            }
        }
    }

    @Test
    fun `test that resume TransferMenuAction is displayed if transfers are already paused and click action invokes correctly`() {
        initComposeTestRule(uiState = TransfersUiState(areTransfersPaused = true))
        with(composeTestRule) {
            onNodeWithTag(TEST_TAG_RESUME_ACTION).apply {
                assertIsDisplayed()
                performClick()
                verify(onResumeTransfers).invoke()
            }
        }
    }

    private fun initComposeTestRule(
        selectedTabIndex: Int = IN_PROGRESS_TAB_INDEX,
        uiState: TransfersUiState,
    ) {
        composeTestRule.setContent {
            TransfersView(
                selectedTabIndex = selectedTabIndex,
                uiState = uiState,
                onPlayPauseTransfer = onPlayPauseTransfer,
                onResumeTransfers = onResumeTransfers,
                onPauseTransfers = onPauseTransfers,
            )
        }
    }
}