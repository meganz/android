package mega.privacy.android.app.presentation.transfers.view

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import mega.privacy.android.app.presentation.transfers.model.TransferMenuAction.Companion.TEST_TAG_CANCEL_ACTION
import mega.privacy.android.app.presentation.transfers.model.TransferMenuAction.Companion.TEST_TAG_CLEAR_ACTION
import mega.privacy.android.app.presentation.transfers.model.TransferMenuAction.Companion.TEST_TAG_MORE_ACTION
import mega.privacy.android.app.presentation.transfers.model.TransferMenuAction.Companion.TEST_TAG_PAUSE_ACTION
import mega.privacy.android.app.presentation.transfers.model.TransferMenuAction.Companion.TEST_TAG_RESUME_ACTION
import mega.privacy.android.app.presentation.transfers.model.TransferMenuAction.Companion.TEST_TAG_RETRY_ACTION
import mega.privacy.android.app.presentation.transfers.model.TransferMenuAction.Companion.TEST_TAG_SELECT_ALL_ACTION
import mega.privacy.android.app.presentation.transfers.model.TransfersUiState
import mega.privacy.android.app.presentation.transfers.model.image.ActiveTransferImageViewModel
import mega.privacy.android.app.presentation.transfers.model.image.CompletedTransferImageViewModel
import mega.privacy.android.app.presentation.transfers.model.image.TransferImageUiState
import mega.privacy.android.app.presentation.transfers.starttransfer.StartTransfersComponentViewModel
import mega.privacy.android.app.presentation.transfers.starttransfer.model.StartTransferViewState
import mega.privacy.android.domain.entity.Progress
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.transfer.CompletedTransfer
import mega.privacy.android.domain.entity.transfer.InProgressTransfer
import mega.privacy.android.domain.entity.transfer.TransferState
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.shared.resources.R as sharedR
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import java.math.BigInteger

@RunWith(AndroidJUnit4::class)
class TransfersViewTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val onPlayPauseTransfer: (Int) -> Unit = mock()
    private val onResumeTransfers: () -> Unit = mock()
    private val onPauseTransfers: () -> Unit = mock()
    private val tag1 = 1
    private val tag2 = 2
    private val tag3 = 3
    private val state =
        TransferImageUiState(fileTypeResId = iconPackR.drawable.ic_text_medium_solid)
    private val activeTransferViewModel = mock<ActiveTransferImageViewModel> {
        on { getUiStateFlow(tag1) } doReturn MutableStateFlow(state)
        on { getUiStateFlow(tag2) } doReturn MutableStateFlow(state)
    }
    private val completedTransferImageViewModel = mock<CompletedTransferImageViewModel> {
        on { getUiStateFlow(tag1) } doReturn MutableStateFlow(state)
        on { getUiStateFlow(tag2) } doReturn MutableStateFlow(state)
        on { getUiStateFlow(tag3) } doReturn MutableStateFlow(state)
    }
    private val startTransfersComponentViewModel = mock<StartTransfersComponentViewModel> {
        on { uiState } doReturn MutableStateFlow(StartTransferViewState())
    }
    private val viewModelStore = mock<ViewModelStore> {
        on { get(argThat<String> { contains(ActiveTransferImageViewModel::class.java.canonicalName.orEmpty()) }) } doReturn activeTransferViewModel
        on { get(argThat<String> { contains(CompletedTransferImageViewModel::class.java.canonicalName.orEmpty()) }) } doReturn completedTransferImageViewModel
        on { get(argThat<String> { contains(StartTransfersComponentViewModel::class.java.canonicalName.orEmpty()) }) } doReturn startTransfersComponentViewModel
    }
    private val viewModelStoreOwner = mock<ViewModelStoreOwner> {
        on { viewModelStore } doReturn viewModelStore
    }
    private val inProgressTransfers = listOf(
        getActiveTransfer(tag = tag1),
        getActiveTransfer(tag = tag2),
    ).toImmutableList()
    private val completedTransfers = listOf(
        getCompletedTransfer(tag1, TransferState.STATE_COMPLETED),
    ).toImmutableList()
    private val failedTransfers = listOf(
        getCompletedTransfer(tag2, TransferState.STATE_FAILED),
        getCompletedTransfer(tag3, TransferState.STATE_FAILED),
    ).toImmutableList()

    @Test
    fun `test that view is displayed correctly if there are no transfers`() {
        initComposeTestRule(uiState = TransfersUiState())
        with(composeTestRule) {
            val emptyText = activity.getString(sharedR.string.transfers_no_transfers_empty_text)
                .replace("[A]", "").replace("[/A]", "")

            onNodeWithTag(TEST_TAG_TRANSFERS_VIEW).assertIsDisplayed()
            onNodeWithTag(TEST_TAG_EMPTY_TRANSFERS_VIEW).assertIsDisplayed()
            onNodeWithText(emptyText).assertIsDisplayed()
        }
    }

    @Test
    fun `test that no TransferMenuAction is displayed if it is in the active tab but there are no active transfers`() {
        initComposeTestRule(
            uiState = TransfersUiState(
                selectedTab = ACTIVE_TAB_INDEX,
                activeTransfers = emptyList<InProgressTransfer>().toImmutableList(),
            )
        )
        with(composeTestRule) {
            onNodeWithTag(TEST_TAG_MORE_ACTION).assertDoesNotExist()
            onNodeWithTag(TEST_TAG_PAUSE_ACTION).assertDoesNotExist()
            onNodeWithTag(TEST_TAG_RESUME_ACTION).assertDoesNotExist()
        }
    }

    @Test
    fun `test that pause TransferMenuAction is displayed if transfers are not already paused and click action invokes correctly`() {
        initComposeTestRule(
            uiState = TransfersUiState(
                activeTransfers = inProgressTransfers,
                areTransfersPaused = false
            )
        )
        with(composeTestRule) {
            onNodeWithTag(TEST_TAG_PAUSE_ACTION).apply {
                assertIsDisplayed()
                performClick()
                verify(onPauseTransfers).invoke()
            }
        }
    }

    @Test
    fun `test that resume TransferMenuAction is displayed if it is in the active tab, are transfers already paused and click action invokes correctly`() {
        initComposeTestRule(
            uiState = TransfersUiState(
                activeTransfers = inProgressTransfers,
                areTransfersPaused = true,
            )
        )
        with(composeTestRule) {
            onNodeWithTag(TEST_TAG_RESUME_ACTION).apply {
                assertIsDisplayed()
                performClick()
                verify(onResumeTransfers).invoke()
            }
        }
    }

    @Test
    fun `test that more TransferMenuAction is displayed if it is the active tab, there are active transfers`() {
        initComposeTestRule(
            uiState = TransfersUiState(
                selectedTab = ACTIVE_TAB_INDEX,
                activeTransfers = inProgressTransfers
            )
        )

        composeTestRule.onNodeWithTag(TEST_TAG_MORE_ACTION).assertIsDisplayed()
    }

    @Test
    fun `test that no TransferMenuAction is displayed if it is in the completed tab but there are no completed transfers`() {
        initComposeTestRule(
            uiState = TransfersUiState(
                selectedTab = COMPLETED_TAB_INDEX,
                completedTransfers = emptyList<CompletedTransfer>().toImmutableList(),
            )
        )
        with(composeTestRule) {
            onNodeWithTag(TEST_TAG_MORE_ACTION).assertDoesNotExist()
            onNodeWithTag(TEST_TAG_PAUSE_ACTION).assertDoesNotExist()
            onNodeWithTag(TEST_TAG_RESUME_ACTION).assertDoesNotExist()
        }
    }

    @Test
    fun `test that more TransferMenuAction is displayed if it is the completed tab, there are completed transfers`() {
        initComposeTestRule(
            uiState = TransfersUiState(
                selectedTab = COMPLETED_TAB_INDEX,
                completedTransfers = completedTransfers,
            )
        )

        composeTestRule.onNodeWithTag(TEST_TAG_MORE_ACTION).assertIsDisplayed()
    }

    @Test
    fun `test that no TransferMenuAction is displayed if it is in the failed tab but there are no failed transfers`() {
        initComposeTestRule(
            uiState = TransfersUiState(
                selectedTab = FAILED_TAB_INDEX,
                failedTransfers = emptyList<CompletedTransfer>().toImmutableList(),
            )
        )
        with(composeTestRule) {
            onNodeWithTag(TEST_TAG_MORE_ACTION).assertDoesNotExist()
            onNodeWithTag(TEST_TAG_PAUSE_ACTION).assertDoesNotExist()
            onNodeWithTag(TEST_TAG_RESUME_ACTION).assertDoesNotExist()
        }
    }

    @Test
    fun `test that more TransferMenuAction is displayed if it is the failed tab, there are failed transfers`() {
        initComposeTestRule(
            uiState = TransfersUiState(
                selectedTab = FAILED_TAB_INDEX,
                failedTransfers = failedTransfers,
            )
        )

        composeTestRule.onNodeWithTag(TEST_TAG_MORE_ACTION).assertIsDisplayed()
    }

    @Test
    fun `test that select all TransferMenuAction is displayed if it is the active tab and there are selected transfers`() {
        initComposeTestRule(
            uiState = TransfersUiState(
                selectedTab = ACTIVE_TAB_INDEX,
                activeTransfers = inProgressTransfers,
                selectedActiveTransfersIds = inProgressTransfers
                    .take(1)
                    .map { it.uniqueId }
                    .toImmutableList(),
            )
        )

        composeTestRule.onNodeWithTag(TEST_TAG_SELECT_ALL_ACTION).assertIsDisplayed()
    }

    @Test
    fun `test that cancel selected TransferMenuAction is displayed if it is the active tab and there are selected transfers`() {
        initComposeTestRule(
            uiState = TransfersUiState(
                selectedTab = ACTIVE_TAB_INDEX,
                activeTransfers = inProgressTransfers,
                selectedActiveTransfersIds = inProgressTransfers
                    .take(1)
                    .map { it.uniqueId }
                    .toImmutableList(),
            )
        )

        composeTestRule.onNodeWithTag(TEST_TAG_CANCEL_ACTION).assertIsDisplayed()
    }

    @Test
    fun `test that select all TransferMenuAction is displayed if it is the failed tab and there are selected and unselected transfers`() {
        initComposeTestRule(
            uiState = TransfersUiState(
                selectedTab = FAILED_TAB_INDEX,
                failedTransfers = failedTransfers,
                selectedFailedTransfersIds = failedTransfers
                    .take(1)
                    .mapNotNull { it.id }
                    .toImmutableList(),
            )
        )

        composeTestRule.onNodeWithTag(TEST_TAG_SELECT_ALL_ACTION).assertIsDisplayed()
    }

    @Test
    fun `test that clear selected TransferMenuAction is displayed if it is the failed tab and there are selected transfers`() {
        initComposeTestRule(
            uiState = TransfersUiState(
                selectedTab = FAILED_TAB_INDEX,
                failedTransfers = failedTransfers,
                selectedFailedTransfersIds = failedTransfers
                    .take(1)
                    .mapNotNull { it.id }
                    .toImmutableList(),
            )
        )

        composeTestRule.onNodeWithTag(TEST_TAG_CLEAR_ACTION).assertIsDisplayed()
    }

    @Test
    fun `test that retry selected TransferMenuAction is displayed if it is the failed tab and there are selected transfers`() {
        initComposeTestRule(
            uiState = TransfersUiState(
                selectedTab = FAILED_TAB_INDEX,
                failedTransfers = failedTransfers,
                selectedFailedTransfersIds = failedTransfers
                    .take(1)
                    .mapNotNull { it.id }
                    .toImmutableList(),
            )
        )

        composeTestRule.onNodeWithTag(TEST_TAG_RETRY_ACTION).assertIsDisplayed()
    }

    private fun initComposeTestRule(uiState: TransfersUiState) {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalViewModelStoreOwner provides viewModelStoreOwner) {
                TransfersView(
                    onBackPress = {},
                    onNavigateToStorageSettings = {},
                    onNavigateToUpgradeAccount = {},
                    uiState = uiState,
                    onTabSelected = {},
                    onPlayPauseTransfer = onPlayPauseTransfer,
                    onResumeTransfers = onResumeTransfers,
                    onPauseTransfers = onPauseTransfers,
                    onRetryFailedTransfers = {},
                    onCancelAllFailedTransfers = {},
                    onClearAllCompletedTransfers = {},
                    onClearAllFailedTransfers = {},
                    onActiveTransfersReorderPreview = { _, _ -> },
                    onActiveTransfersReorderConfirmed = {},
                    onConsumeStartEvent = {},
                    onSelectActiveTransfers = {},
                    onSelectCompletedTransfers = {},
                    onSelectFailedTransfers = {},
                    onSelectTransfersClose = {},
                    onActiveTransferSelected = {},
                    onCompletedTransferSelected = {},
                    onFailedTransferSelected = {},
                    onCancelSelectedActiveTransfers = {},
                    onClearSelectedCompletedTransfers = {},
                    onClearSelectedFailedTransfers = {},
                    onRetrySelectedFailedTransfers = {},
                    onSelectAllActiveTransfers = {},
                    onSelectAllCompletedTransfers = {},
                    onSelectAllFailedTransfers = {},
                    onRetryTransfer = {},
                    onConsumeQuotaWarning = {},
                )
            }
        }
    }

    private fun getActiveTransfer(tag: Int) = InProgressTransfer.Download(
        uniqueId = tag.toLong(),
        tag = tag,
        totalBytes = 100,
        isPaused = false,
        fileName = "name",
        speed = 100,
        state = TransferState.STATE_ACTIVE,
        priority = BigInteger.ONE,
        progress = Progress(0.5F),
        nodeId = NodeId(1),
    )

    private fun getCompletedTransfer(id: Int, state: TransferState) = CompletedTransfer(
        id = id,
        fileName = "$id-fileName",
        type = TransferType.DOWNLOAD,
        state = state,
        size = "3.57 MB",
        handle = 27169983390750L + id,
        path = "Cloud drive",
        isOffline = false,
        timestamp = 1684228012974L + id,
        error = "Error",
        originalPath = "/data/user/0/mega.privacy.android.app/DCIM/Camera/$id-fileName",
        parentHandle = 11622336899311L,
        appData = emptyList(),
        displayPath = null,
        errorCode = null,
    )
}