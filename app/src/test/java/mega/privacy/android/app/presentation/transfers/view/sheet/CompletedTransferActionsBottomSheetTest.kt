package mega.privacy.android.app.presentation.transfers.view.sheet

import androidx.activity.ComponentActivity
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.core.net.toUri
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.app.R
import mega.privacy.android.app.onNodeWithText
import mega.privacy.android.app.presentation.transfers.model.completed.CompletedTransferActionsUiState
import mega.privacy.android.app.utils.TimeUtils
import mega.privacy.android.analytics.test.AnalyticsTestRule
import mega.privacy.android.domain.entity.transfer.CompletedTransfer
import mega.privacy.android.domain.entity.transfer.TransferState
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.mobile.analytics.event.CompletedTransfersItemClearMenuItemEvent
import mega.privacy.mobile.analytics.event.CompletedTransfersItemOpenMenuItemEvent
import mega.privacy.mobile.analytics.event.CompletedTransfersItemShareMenuItemEvent
import mega.privacy.mobile.analytics.event.CompletedTransfersItemViewInFolderMenuItemEvent
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.robolectric.annotation.Config
import kotlin.time.Duration.Companion.milliseconds

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w1080dp-h1920dp")
class CompletedTransferActionsBottomSheetTest {

    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val analyticsRule = AnalyticsTestRule()

    @get:Rule
    val ruleChain: RuleChain = RuleChain.outerRule(analyticsRule).around(composeTestRule)

    private val onOpenWith = mock<(CompletedTransfer) -> Unit>()
    private val onShareLink = mock<(Long) -> Unit>()
    private val onClearTransfer = mock<(CompletedTransfer) -> Unit>()
    private val onDismissSheet = mock<() -> Unit>()
    private val fileName = "2023-03-24 00.13.20_1.pdf"
    private val completedDownload = CompletedTransfer(
        id = 0,
        fileName = fileName,
        type = TransferType.DOWNLOAD,
        state = TransferState.STATE_COMPLETED,
        size = "3.57 MB",
        handle = 27169983390750L,
        path = "content://com.android.externalstorage.documents/tree/primary%Download//2023-03-24 00.13.20_1.pdf",
        displayPath = "storage/emulated/0/Download",
        isOffline = false,
        timestamp = 1684228012974L,
        error = "No error",
        errorCode = 0,
        originalPath = "/original/path/2023-03-24 00.13.20_1.pdf",
        parentHandle = 11622336899311L,
        appData = emptyList(),
    )
    private val completedUpload = CompletedTransfer(
        id = 0,
        fileName = fileName,
        type = TransferType.GENERAL_UPLOAD,
        state = TransferState.STATE_COMPLETED,
        size = "3.57 MB",
        handle = 27169983390750L,
        path = "Cloud drive/Camera uploads",
        displayPath = null,
        isOffline = false,
        timestamp = 1684228012974L,
        error = "No error",
        errorCode = 0,
        originalPath = "/original/path/2023-03-24 00.13.20_1.pdf",
        parentHandle = 11622336899311L,
        appData = emptyList(),
    )

    @Test
    fun `test that sheet is correctly shown for a download`() {
        initComposeTestRule(
            completedDownload, CompletedTransferActionsUiState(
                completedTransfer = completedDownload,
                fileUri = "fileUri".toUri(),
                amINodeOwner = true,
            )
        )

        with(composeTestRule) {
            onNodeWithTag(TEST_TAG_COMPLETED_TRANSFER_ACTIONS_PANEL).assertIsDisplayed()
            onNodeWithTag(TEST_TAG_COMPLETED_TRANSFER).assertIsDisplayed()
            onNodeWithText(fileName).assertIsDisplayed()
            onNodeWithText(completedDownload.size, substring = true).assertIsDisplayed()
            onNodeWithText(
                TimeUtils.formatLongDateTime(completedDownload.timestamp.milliseconds.inWholeSeconds),
                substring = true
            ).assertIsDisplayed()
            onNodeWithText(completedDownload.path).assertIsNotDisplayed()
            onNodeWithTag(TEST_TAG_VIEW_IN_FOLDER_ACTION).assertIsDisplayed()
            onNodeWithText(R.string.view_in_folder_label).assertIsDisplayed()
            onNodeWithTag(TEST_TAG_OPEN_WITH_ACTION).assertIsDisplayed()
            onNodeWithText(R.string.external_play).assertIsDisplayed()
            onNodeWithTag(TEST_TAG_SHARE_LINK_ACTION).assertIsDisplayed()
            onNodeWithText(R.string.context_get_link).assertIsDisplayed()
            onNodeWithTag(TEST_TAG_CLEAR_ACTION).assertIsDisplayed()
            onNodeWithText(R.string.general_clear).assertIsDisplayed()
        }
    }

    @Test
    fun `test that sheet is correctly shown for an upload`() {
        initComposeTestRule(
            completedUpload, CompletedTransferActionsUiState(
                completedTransfer = completedUpload,
                amINodeOwner = true,
            )
        )

        with(composeTestRule) {
            onNodeWithTag(TEST_TAG_COMPLETED_TRANSFER_ACTIONS_PANEL).assertIsDisplayed()
            onNodeWithTag(TEST_TAG_COMPLETED_TRANSFER).assertIsDisplayed()
            onNodeWithText(fileName).assertIsDisplayed()
            onNodeWithText(completedDownload.size, substring = true).assertIsDisplayed()
            onNodeWithText(
                TimeUtils.formatLongDateTime(completedDownload.timestamp.milliseconds.inWholeSeconds),
                substring = true
            ).assertIsDisplayed()
            onNodeWithTag(TEST_TAG_VIEW_IN_FOLDER_ACTION).assertIsDisplayed()
            onNodeWithText(R.string.view_in_folder_label).assertIsDisplayed()
            onNodeWithTag(TEST_TAG_OPEN_WITH_ACTION).assertIsNotDisplayed()
            onNodeWithText(R.string.external_play).assertIsNotDisplayed()
            onNodeWithTag(TEST_TAG_SHARE_LINK_ACTION).assertIsDisplayed()
            onNodeWithText(R.string.context_get_link).assertIsDisplayed()
            onNodeWithTag(TEST_TAG_CLEAR_ACTION).assertIsDisplayed()
            onNodeWithText(R.string.general_clear).assertIsDisplayed()
        }
    }

    @Test
    fun `test that click on view in folder invokes correctly`() {
        initComposeTestRule(
            completedUpload, CompletedTransferActionsUiState(
                completedTransfer = completedUpload,
                amINodeOwner = true,
                isOnline = true,
            )
        )

        with(composeTestRule) {
            onNodeWithTag(TEST_TAG_VIEW_IN_FOLDER_ACTION)
                .performSemanticsAction(SemanticsActions.OnClick)

            verify(onDismissSheet).invoke()
        }
    }

    @Test
    fun `test that click on open with invokes correctly`() {
        initComposeTestRule(
            completedDownload, CompletedTransferActionsUiState(
                completedTransfer = completedDownload,
                fileUri = "fileUri".toUri(),
                amINodeOwner = true,
            )
        )

        with(composeTestRule) {
            onNodeWithTag(TEST_TAG_OPEN_WITH_ACTION)
                .performSemanticsAction(SemanticsActions.OnClick)

            verify(onOpenWith).invoke(completedDownload)
            verifyNoInteractions(onDismissSheet)
        }
    }

    @Test
    fun `test that click on share link invokes correctly`() {
        initComposeTestRule(
            completedDownload, CompletedTransferActionsUiState(
                completedTransfer = completedDownload,
                fileUri = "fileUri".toUri(),
                amINodeOwner = true,
                isOnline = true,
            )
        )

        with(composeTestRule) {
            onNodeWithTag(TEST_TAG_SHARE_LINK_ACTION)
                .performSemanticsAction(SemanticsActions.OnClick)

            verify(onShareLink).invoke(completedDownload.handle)
            verifyNoInteractions(onDismissSheet)
        }
    }

    @Test
    fun `test that click on clear invokes correctly`() {
        initComposeTestRule(
            completedUpload, CompletedTransferActionsUiState(
                completedTransfer = completedUpload,
                amINodeOwner = true,
            )
        )

        with(composeTestRule) {
            onNodeWithTag(TEST_TAG_CLEAR_ACTION)
                .performSemanticsAction(SemanticsActions.OnClick)

            verify(onClearTransfer).invoke(completedUpload)
            verify(onDismissSheet).invoke()
        }
    }


    @Test
    fun `test that view in folder analytics event is tracked when view in folder action is clicked`() {
        initComposeTestRule(
            completedUpload, CompletedTransferActionsUiState(
                completedTransfer = completedUpload,
                amINodeOwner = true,
                isOnline = true,
            )
        )

        with(composeTestRule) {
            onNodeWithTag(TEST_TAG_VIEW_IN_FOLDER_ACTION)
                .performSemanticsAction(SemanticsActions.OnClick)

            assertThat(analyticsRule.events).contains(
                CompletedTransfersItemViewInFolderMenuItemEvent
            )
        }
    }

    @Test
    fun `test that open with analytics event is tracked when open with action is clicked`() {
        initComposeTestRule(
            completedDownload, CompletedTransferActionsUiState(
                completedTransfer = completedDownload,
                fileUri = "fileUri".toUri(),
                amINodeOwner = true,
            )
        )

        with(composeTestRule) {
            onNodeWithTag(TEST_TAG_OPEN_WITH_ACTION)
                .performSemanticsAction(SemanticsActions.OnClick)

            assertThat(analyticsRule.events).contains(CompletedTransfersItemOpenMenuItemEvent)
        }
    }

    @Test
    fun `test that share link analytics event is tracked when share link action is clicked`() {
        initComposeTestRule(
            completedDownload, CompletedTransferActionsUiState(
                completedTransfer = completedDownload,
                fileUri = "fileUri".toUri(),
                amINodeOwner = true,
                isOnline = true,
            )
        )

        composeTestRule.onNodeWithTag(TEST_TAG_SHARE_LINK_ACTION)
            .performSemanticsAction(SemanticsActions.OnClick)

        assertThat(analyticsRule.events).contains(CompletedTransfersItemShareMenuItemEvent)
    }

    @Test
    fun `test that clear analytics event is tracked when clear action is clicked`() {
        initComposeTestRule(
            completedUpload, CompletedTransferActionsUiState(
                completedTransfer = completedUpload,
                amINodeOwner = true,
            )
        )

        with(composeTestRule) {
            onNodeWithTag(TEST_TAG_CLEAR_ACTION)
                .performSemanticsAction(SemanticsActions.OnClick)

            assertThat(analyticsRule.events).contains(CompletedTransfersItemClearMenuItemEvent)
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    private fun initComposeTestRule(
        completedTransfer: CompletedTransfer,
        uiState: CompletedTransferActionsUiState,
    ) {
        composeTestRule.setContent {
            CompletedTransferActionsBottomSheet(
                completedTransfer = completedTransfer,
                fileTypeResId = iconPackR.drawable.ic_pdf_medium_solid,
                previewUri = null,
                uiState = uiState,
                onOpenWith = onOpenWith,
                onShareLink = onShareLink,
                onClearTransfer = onClearTransfer,
                onConsumeOpenWithEvent = {},
                onConsumeShareLinkEvent = {},
                onDismissSheet = onDismissSheet,
            )
        }
    }
}