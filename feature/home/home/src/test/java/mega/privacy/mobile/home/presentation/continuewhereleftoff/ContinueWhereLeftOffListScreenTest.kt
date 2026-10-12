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
import com.google.common.truth.Truth.assertThat
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.analytics.test.AnalyticsTestRule
import mega.privacy.android.domain.entity.continuewhereleftoff.ContinueWhereLeftOffItem
import mega.privacy.android.domain.entity.continuewhereleftoff.RecentlyUsedType
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.navigation.contract.TransferHandler
import mega.privacy.android.navigation.contract.menu.CommonMenuAction
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.mobile.analytics.event.ContinueWhereLeftOffClearHistoryDialogConfirmedEvent
import mega.privacy.mobile.analytics.event.ContinueWhereLeftOffClearHistoryMenuItemEvent
import mega.privacy.mobile.analytics.event.ContinueWhereLeftOffListItemPressedEvent
import mega.privacy.mobile.analytics.event.ContinueWhereLeftOffMoreButtonPressedEvent
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class ContinueWhereLeftOffListScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val analyticsRule = AnalyticsTestRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private val viewModel = mock<ContinueWhereLeftOffListViewModel>()
    private val transferHandler = mock<TransferHandler>()

    private val pdfItem = ContinueWhereLeftOffItem(
        nodeHandle = 1L,
        title = "Document.pdf",
        type = RecentlyUsedType.PDF,
        lastAccessedTimestamp = 1712880000L,
    )

    @Test
    fun `test that more button pressed event is tracked when more menu is clicked`() {
        setContent(ContinueWhereLeftOffListUiState(isLoading = false))

        composeRule.onNodeWithTag(CommonMenuAction.More.testTag, useUnmergedTree = true)
            .performClick()

        assertThat(analyticsRule.events).contains(ContinueWhereLeftOffMoreButtonPressedEvent)
        verify(viewModel).showOptionsSheet()
    }

    @Test
    fun `test that clear history menu item event is tracked when clear history is clicked`() {
        setContent(ContinueWhereLeftOffListUiState(isLoading = false, showOptionsSheet = true))

        composeRule.onNodeWithTag(CLEAR_HISTORY_TAG, useUnmergedTree = true).performClick()
        composeRule.waitForIdle()

        assertThat(analyticsRule.events).contains(ContinueWhereLeftOffClearHistoryMenuItemEvent)
        assertThat(analyticsRule.events)
            .doesNotContain(ContinueWhereLeftOffClearHistoryDialogConfirmedEvent)
    }

    @Test
    fun `test that clear history dialog confirmed event is tracked when clear is confirmed`() {
        setContent(ContinueWhereLeftOffListUiState(isLoading = false, showOptionsSheet = true))

        composeRule.onNodeWithTag(CLEAR_HISTORY_TAG, useUnmergedTree = true).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText(context.getString(sharedR.string.general_clear)).performClick()
        composeRule.waitForIdle()

        assertThat(analyticsRule.events)
            .contains(ContinueWhereLeftOffClearHistoryDialogConfirmedEvent)
    }

    @Test
    fun `test that clear history dialog confirmed event is not tracked when the dialog is dismissed`() {
        setContent(ContinueWhereLeftOffListUiState(isLoading = false, showOptionsSheet = true))

        composeRule.onNodeWithTag(CLEAR_HISTORY_TAG, useUnmergedTree = true).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText(context.getString(sharedR.string.general_dismiss_dialog))
            .performClick()
        composeRule.waitForIdle()

        assertThat(analyticsRule.events)
            .doesNotContain(ContinueWhereLeftOffClearHistoryDialogConfirmedEvent)
    }

    @Test
    fun `test that list item pressed event is tracked when an item is clicked in list view`() {
        setContent(ContinueWhereLeftOffListUiState(isLoading = false, items = listOf(pdfItem)))

        composeRule.onNodeWithText(pdfItem.title).performClick()

        assertThat(analyticsRule.events).contains(ContinueWhereLeftOffListItemPressedEvent)
        verify(viewModel).onItemClicked(pdfItem.nodeHandle, pdfItem.type)
    }

    @Test
    fun `test that list item pressed event is tracked when an item is clicked in grid view`() {
        setContent(
            ContinueWhereLeftOffListUiState(
                isLoading = false,
                items = listOf(pdfItem),
                currentViewType = ViewType.GRID,
            )
        )

        composeRule.onNodeWithText(pdfItem.title).performClick()

        assertThat(analyticsRule.events).contains(ContinueWhereLeftOffListItemPressedEvent)
        verify(viewModel).onItemClicked(pdfItem.nodeHandle, pdfItem.type)
    }

    @Test
    fun `test that list item pressed event is not tracked when a taken down item is clicked`() {
        val takenDownItem = pdfItem.copy(isTakenDown = true)
        setContent(ContinueWhereLeftOffListUiState(isLoading = false, items = listOf(takenDownItem)))

        composeRule.onNodeWithText(takenDownItem.title).performClick()

        assertThat(analyticsRule.events).doesNotContain(ContinueWhereLeftOffListItemPressedEvent)
        verify(viewModel, never()).onItemClicked(any(), any())
    }

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
