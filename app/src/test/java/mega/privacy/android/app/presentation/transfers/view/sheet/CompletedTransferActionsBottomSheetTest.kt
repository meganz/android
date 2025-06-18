package mega.privacy.android.app.presentation.transfers.view.sheet

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.R
import mega.privacy.android.app.onNodeWithText
import mega.privacy.android.app.utils.TimeUtils
import mega.privacy.android.domain.entity.transfer.CompletedTransfer
import mega.privacy.android.domain.entity.transfer.TransferState
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.icon.pack.R as iconPackR
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.annotation.Config
import kotlin.time.Duration.Companion.milliseconds

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w1080dp-h1920dp")
class CompletedTransferActionsBottomSheetTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val onViewInFolder = mock<() -> Unit>()
    private val onOpenWith = mock<() -> Unit>()
    private val onShareLink = mock<() -> Unit>()
    private val onClearTransfer = mock<() -> Unit>()
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
        initComposeTestRule(completedDownload)

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
        initComposeTestRule(completedUpload)

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
        initComposeTestRule(completedUpload)

        with(composeTestRule) {
            onNodeWithTag(TEST_TAG_VIEW_IN_FOLDER_ACTION)
                .performSemanticsAction(SemanticsActions.OnClick)

            verify(onViewInFolder).invoke()
            verify(onDismissSheet).invoke()
        }
    }

    @Test
    fun `test that click on open with invokes correctly`() {
        initComposeTestRule(completedDownload)

        with(composeTestRule) {
            onNodeWithTag(TEST_TAG_OPEN_WITH_ACTION)
                .performSemanticsAction(SemanticsActions.OnClick)

            verify(onOpenWith).invoke()
            verify(onDismissSheet).invoke()
        }
    }

    @Test
    fun `test that click on share link invokes correctly`() {
        initComposeTestRule(completedDownload)

        with(composeTestRule) {
            onNodeWithTag(TEST_TAG_SHARE_LINK_ACTION)
                .performSemanticsAction(SemanticsActions.OnClick)

            verify(onShareLink).invoke()
            verify(onDismissSheet).invoke()
        }
    }

    @Test
    fun `test that click on clear invokes correctly`() {
        initComposeTestRule(completedUpload)

        with(composeTestRule) {
            onNodeWithTag(TEST_TAG_CLEAR_ACTION)
                .performSemanticsAction(SemanticsActions.OnClick)

            verify(onClearTransfer).invoke()
            verify(onDismissSheet).invoke()
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    private fun initComposeTestRule(completedTransfer: CompletedTransfer) {
        composeTestRule.setContent {
            CompletedTransferActionsBottomSheet(
                completedTransfer = completedTransfer,
                fileTypeResId = iconPackR.drawable.ic_pdf_medium_solid,
                previewUri = null,
                onViewInFolder = onViewInFolder,
                onOpenWith = onOpenWith,
                onShareLink = onShareLink,
                onClearTransfer = onClearTransfer,
                onDismissSheet = onDismissSheet,
            )
        }
    }
}