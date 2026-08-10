package mega.privacy.mobile.home.presentation.continuewhereleftoff

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import de.palm.composestateevents.triggered
import kotlinx.coroutines.flow.MutableStateFlow
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.navigation.contract.TransferHandler
import mega.privacy.android.navigation.contract.menu.CommonMenuAction
import mega.privacy.android.shared.resources.R as sharedR
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class ContinueWhereLeftOffListScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private val viewModel = mock<ContinueWhereLeftOffListViewModel>()
    private val transferHandler = mock<TransferHandler>()

    @Test
    fun `test that clear history option is displayed when more menu is clicked`() {
        setContent(ContinueWhereLeftOffListUiState(isLoading = false, showOptionsSheet = true))

        composeRule.waitForIdle()
        composeRule.onNodeWithTag(CLEAR_HISTORY_TAG, useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun `test that clear confirmation dialog is not displayed by default`() {
        setContent(ContinueWhereLeftOffListUiState(isLoading = false, showOptionsSheet = true))

        composeRule.onAllNodesWithTag(CLEAR_HISTORY_DIALOG_TAG, useUnmergedTree = true)
            .assertCountEquals(0)
    }

    @Test
    fun `test that tapping clear history shows the confirmation dialog and does not call clearAll`() {
        setContent(ContinueWhereLeftOffListUiState(isLoading = false, showOptionsSheet = true))

        composeRule.onNodeWithTag(CLEAR_HISTORY_TAG, useUnmergedTree = true).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(CLEAR_HISTORY_DIALOG_TAG, useUnmergedTree = true)
            .assertIsDisplayed()
        verify(viewModel).dismissOptionsSheet()
        verify(viewModel, never()).clearAll()
    }

    @Test
    fun `test that tapping clear in the confirmation dialog calls clearAll`() {
        setContent(ContinueWhereLeftOffListUiState(isLoading = false, showOptionsSheet = true))

        composeRule.onNodeWithTag(CLEAR_HISTORY_TAG, useUnmergedTree = true).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText(context.getString(sharedR.string.general_clear)).performClick()
        composeRule.waitForIdle()

        verify(viewModel).clearAll()
    }

    @Test
    fun `test that tapping dismiss in the confirmation dialog hides it without calling clearAll`() {
        setContent(ContinueWhereLeftOffListUiState(isLoading = false, showOptionsSheet = true))

        composeRule.onNodeWithTag(CLEAR_HISTORY_TAG, useUnmergedTree = true).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText(context.getString(sharedR.string.general_dismiss_dialog))
            .performClick()
        composeRule.waitForIdle()

        composeRule.onAllNodesWithTag(CLEAR_HISTORY_DIALOG_TAG, useUnmergedTree = true)
            .assertCountEquals(0)
        verify(viewModel, never()).clearAll()
    }

    @Test
    fun `test that navigates back when clear history completed event is triggered`() {
        var navigatedBack = false
        setContent(
            ContinueWhereLeftOffListUiState(
                isLoading = false,
                clearHistoryCompletedEvent = triggered,
            ),
            onBack = { navigatedBack = true },
        )

        composeRule.waitForIdle()

        assert(navigatedBack)
        verify(viewModel).onClearHistoryCompletedEventConsumed()
    }

    private fun setContent(
        uiState: ContinueWhereLeftOffListUiState,
        onBack: () -> Unit = {},
    ) {
        whenever(viewModel.uiState).thenReturn(MutableStateFlow(uiState))
        composeRule.setContent {
            AndroidThemeForPreviews {
                ContinueWhereLeftOffListScreen(
                    viewModel = viewModel,
                    onNavigate = {},
                    transferHandler = transferHandler,
                    onBack = onBack,
                )
            }
        }
    }
}
