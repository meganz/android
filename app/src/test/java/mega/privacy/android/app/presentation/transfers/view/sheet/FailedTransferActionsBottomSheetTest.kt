package mega.privacy.android.app.presentation.transfers.view.sheet

import androidx.activity.ComponentActivity
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.app.R
import mega.privacy.android.app.onNodeWithText
import mega.privacy.android.app.presentation.transfers.model.completed.CompletedTransferActionsUiState
import mega.privacy.android.analytics.test.AnalyticsTestRule
import mega.privacy.android.domain.entity.transfer.CompletedTransfer
import mega.privacy.android.domain.entity.transfer.TransferState
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.mobile.analytics.event.FailedTransfersItemClearMenuItemEvent
import mega.privacy.mobile.analytics.event.FailedTransfersItemRetryMenuItemEvent
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w1080dp-h1920dp")
class FailedTransferActionsBottomSheetTest {

    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val analyticsRule = AnalyticsTestRule()

    @get:Rule
    val ruleChain: RuleChain = RuleChain.outerRule(analyticsRule).around(composeTestRule)

    private val onRetryTransfer = mock<(CompletedTransfer) -> Unit>()
    private val onClearTransfer = mock<(CompletedTransfer) -> Unit>()
    private val onDismissSheet = mock<() -> Unit>()
    private val fileName = "2023-03-24 00.13.20_1.pdf"
    private val failedDownload = CompletedTransfer(
        id = 0,
        fileName = fileName,
        type = TransferType.DOWNLOAD,
        state = TransferState.STATE_FAILED,
        size = "3.57 MB",
        handle = 27169983390750L,
        path = "content://com.android.externalstorage.documents/tree/primary%Download//2023-03-24 00.13.20_1.pdf",
        displayPath = "storage/emulated/0/Download",
        isOffline = false,
        timestamp = 1684228012974L,
        error = "Read error",
        errorCode = 0,
        originalPath = "/original/path/2023-03-24 00.13.20_1.pdf",
        parentHandle = 11622336899311L,
        appData = emptyList(),
    )
    private val cancelledUpload = CompletedTransfer(
        id = 0,
        fileName = fileName,
        type = TransferType.GENERAL_UPLOAD,
        state = TransferState.STATE_CANCELLED,
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
    fun `test that sheet is correctly shown for a failed download`() {
        val error = String.format(
            "%s: %s",
            composeTestRule.activity.getString(R.string.failed_label),
            failedDownload.error
        )
        initComposeTestRule(failedDownload)

        with(composeTestRule) {
            onNodeWithTag(TEST_TAG_FAILED_TRANSFER_ACTIONS_PANEL).assertIsDisplayed()
            onNodeWithTag(TEST_TAG_FAILED_TRANSFER).assertIsDisplayed()
            onNodeWithText(fileName).assertIsDisplayed()
            onNodeWithText(error).assertIsDisplayed()
            onNodeWithTag(TEST_TAG_RETRY_ACTION).assertIsDisplayed()
            onNodeWithText(R.string.general_retry).assertIsDisplayed()
            onNodeWithTag(TEST_TAG_CLEAR_FAILED_TRANSFER_ACTION).assertIsDisplayed()
            onNodeWithText(R.string.general_clear).assertIsDisplayed()
        }
    }

    @Test
    fun `test that sheet is correctly shown for an upload`() {
        initComposeTestRule(cancelledUpload)

        with(composeTestRule) {
            onNodeWithTag(TEST_TAG_FAILED_TRANSFER_ACTIONS_PANEL).assertIsDisplayed()
            onNodeWithTag(TEST_TAG_FAILED_TRANSFER).assertIsDisplayed()
            onNodeWithText(fileName).assertIsDisplayed()
            onNodeWithText(R.string.transfer_cancelled).assertIsDisplayed()
            onNodeWithTag(TEST_TAG_RETRY_ACTION).assertIsDisplayed()
            onNodeWithText(R.string.general_retry).assertIsDisplayed()
            onNodeWithTag(TEST_TAG_CLEAR_FAILED_TRANSFER_ACTION).assertIsDisplayed()
            onNodeWithText(R.string.general_clear).assertIsDisplayed()
        }
    }

    @Test
    fun `test that click on retry invokes correctly`() {
        initComposeTestRule(failedDownload)

        with(composeTestRule) {
            onNodeWithTag(TEST_TAG_RETRY_ACTION).performSemanticsAction(SemanticsActions.OnClick)

            verify(onRetryTransfer).invoke(failedDownload)
            verify(onDismissSheet).invoke()
        }
    }

    @Test
    fun `test that click on clear invokes correctly`() {
        initComposeTestRule(cancelledUpload)

        with(composeTestRule) {
            onNodeWithTag(TEST_TAG_CLEAR_FAILED_TRANSFER_ACTION)
                .performSemanticsAction(SemanticsActions.OnClick)

            verify(onClearTransfer).invoke(cancelledUpload)
            verify(onDismissSheet).invoke()
        }
    }

    @Test
    fun `test that retry analytics event is tracked when retry action is clicked`() {
        initComposeTestRule(failedDownload)

        composeTestRule.onNodeWithTag(TEST_TAG_RETRY_ACTION)
            .performSemanticsAction(SemanticsActions.OnClick)

        assertThat(analyticsRule.events).contains(FailedTransfersItemRetryMenuItemEvent)
    }

    @Test
    fun `test that clear analytics event is tracked when clear action is clicked`() {
        initComposeTestRule(cancelledUpload)

        composeTestRule.onNodeWithTag(TEST_TAG_CLEAR_FAILED_TRANSFER_ACTION)
            .performSemanticsAction(SemanticsActions.OnClick)

        assertThat(analyticsRule.events).contains(FailedTransfersItemClearMenuItemEvent)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    private fun initComposeTestRule(failedTransfer: CompletedTransfer) {
        composeTestRule.setContent {
            FailedTransferActionsBottomSheet(
                failedTransfer = failedTransfer,
                fileTypeResId = iconPackR.drawable.ic_pdf_medium_solid,
                previewUri = null,
                uiState = CompletedTransferActionsUiState(),
                onRetryTransfer = onRetryTransfer,
                onClearTransfer = onClearTransfer,
                onDismissSheet = onDismissSheet,
            )
        }
    }
}