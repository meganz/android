package mega.privacy.android.core.nodecomponents.sheet.home

import androidx.activity.ComponentActivity
import androidx.annotation.StringRes
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import mega.privacy.android.analytics.test.AnalyticsTestRule
import mega.privacy.android.core.nodecomponents.R
import mega.privacy.android.shared.resources.R as sharedResR
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class HomeFabOptionsBottomSheetTest {
    private val composeTestRule = createAndroidComposeRule<ComponentActivity>()
    private val analyticsRule = AnalyticsTestRule()

    @get:Rule
    val ruleChain: RuleChain = RuleChain.outerRule(analyticsRule).around(composeTestRule)

    private val onUploadFilesClicked = mock<() -> Unit>()
    private val onUploadFolderClicked = mock<() -> Unit>()
    private val onScanDocumentClicked = mock<() -> Unit>()
    private val onCaptureClicked = mock<() -> Unit>()
    private val onCreateNewTextFileClicked = mock<() -> Unit>()
    private val onAddNewSyncClicked = mock<() -> Unit>()
    private val onAddNewBackupClicked = mock<() -> Unit>()
    private val onNewChatClicked = mock<() -> Unit>()

    @Test
    fun `test that sheet shows correctly`() {
        initComposeTestRule()

        with(composeTestRule) {
            onNodeWithTag(TEST_TAG_HOME_FAB_OPTIONS_SHEET).assertExists()
            onNodeWithTag(TEST_TAG_UPLOAD_SECTION_HEADER).assertExists()
            onNodeWithTag(TEST_TAG_UPLOAD_FILES_ACTION).assertExists()
            onNodeWithTag(TEST_TAG_UPLOAD_FOLDER_ACTION).assertExists()
            onNodeWithTag(TEST_TAG_SCAN_DOCUMENT_ACTION).assertExists()
            onNodeWithTag(TEST_TAG_CAPTURE_ACTION).assertExists()
            onNodeWithTag(TEST_TAG_NEW_TEXT_FILE_ACTION).assertExists()
            onNodeWithTag(TEST_TAG_SYNC_SECTION_HEADER).assertExists()
            onNodeWithTag(TEST_TAG_ADD_NEW_SYNC_ACTION).assertExists()
            onNodeWithTag(TEST_TAG_ADD_NEW_BACKUP_ACTION).assertExists()
            onNodeWithTag(TEST_TAG_CHAT_SECTION_HEADER).assertExists()
            onNodeWithTag(TEST_TAG_NEW_CHAT_ACTION).assertExists()
        }
    }

    @Test
    fun `test that all text items are displayed`() {
        initComposeTestRule()

        with(composeTestRule) {
            onNodeWithText(R.string.upload_files).assertExists()
            onNodeWithText(R.string.upload_folder).assertExists()
            onNodeWithText(R.string.menu_scan_document).assertExists()
            onNodeWithText(R.string.menu_take_picture).assertExists()
            onNodeWithText(R.string.action_create_txt).assertExists()
            onNodeWithText(sharedResR.string.device_center_sync_add_new_syn_button_option).assertExists()
            onNodeWithText(sharedResR.string.device_center_sync_add_new_backup_button_option).assertExists()
        }
    }

    @Test
    fun `test that section headers are displayed`() {
        initComposeTestRule()

        with(composeTestRule) {
            onNodeWithText(R.string.context_upload).assertExists()
            onNodeWithText(sharedResR.string.settings_section_sync).assertExists()
            onNodeWithText(sharedResR.string.general_chat).assertExists()
            onNodeWithText(R.string.fab_label_new_chat).assertExists()
        }
    }

    @Test
    fun `test that clicking on upload files option invokes onUploadFilesClicked and onDismissSheet`() {
        initComposeTestRule()

        composeTestRule.onNodeWithTag(TEST_TAG_UPLOAD_FILES_ACTION)
            .performSemanticsAction(SemanticsActions.OnClick)

        verify(onUploadFilesClicked).invoke()
    }

    @Test
    fun `test that clicking on upload folder option invokes onUploadFolderClicked and onDismissSheet`() {
        initComposeTestRule()

        composeTestRule.onNodeWithTag(TEST_TAG_UPLOAD_FOLDER_ACTION)
            .performSemanticsAction(SemanticsActions.OnClick)

        verify(onUploadFolderClicked).invoke()
    }

    @Test
    fun `test that clicking on scan document option invokes onScanDocumentClicked and onDismissSheet`() {
        initComposeTestRule()

        composeTestRule.onNodeWithTag(TEST_TAG_SCAN_DOCUMENT_ACTION)
            .performSemanticsAction(SemanticsActions.OnClick)

        verify(onScanDocumentClicked).invoke()
    }

    @Test
    fun `test that clicking on capture option invokes onCaptureClicked and onDismissSheet`() {
        initComposeTestRule()

        composeTestRule.onNodeWithTag(TEST_TAG_CAPTURE_ACTION)
            .performSemanticsAction(SemanticsActions.OnClick)

        verify(onCaptureClicked).invoke()
    }

    @Test
    fun `test that clicking on new text file option invokes onCreateNewTextFileClicked and onDismissSheet`() {
        initComposeTestRule()

        composeTestRule.onNodeWithTag(TEST_TAG_NEW_TEXT_FILE_ACTION)
            .performSemanticsAction(SemanticsActions.OnClick)

        verify(onCreateNewTextFileClicked).invoke()
    }

    @Test
    fun `test that clicking on add new sync option invokes onAddNewSyncClicked and onDismissSheet`() {
        initComposeTestRule()

        composeTestRule.onNodeWithTag(TEST_TAG_ADD_NEW_SYNC_ACTION)
            .performSemanticsAction(SemanticsActions.OnClick)

        verify(onAddNewSyncClicked).invoke()
    }

    @Test
    fun `test that clicking on add new backup option invokes onAddNewBackupClicked and onDismissSheet`() {
        initComposeTestRule()

        composeTestRule.onNodeWithTag(TEST_TAG_ADD_NEW_BACKUP_ACTION)
            .performSemanticsAction(SemanticsActions.OnClick)

        verify(onAddNewBackupClicked).invoke()
    }

    @Test
    fun `test that clicking on new chat option invokes onNewChatClicked and onDismissSheet`() {
        initComposeTestRule()

        composeTestRule.onNodeWithTag(TEST_TAG_NEW_CHAT_ACTION)
            .performSemanticsAction(SemanticsActions.OnClick)

        verify(onNewChatClicked).invoke()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    private fun initComposeTestRule() {
        composeTestRule.setContent {
            HomeFabOptionsBottomSheet(
                onUploadFilesClicked = onUploadFilesClicked,
                onUploadFolderClicked = onUploadFolderClicked,
                onScanDocumentClicked = onScanDocumentClicked,
                onCaptureClicked = onCaptureClicked,
                onCreateNewTextFileClicked = onCreateNewTextFileClicked,
                onAddNewSyncClicked = onAddNewSyncClicked,
                onAddNewBackupClicked = onAddNewBackupClicked,
                onNewChatClicked = onNewChatClicked,
            )
        }
    }

    internal fun SemanticsNodeInteractionsProvider.onNodeWithText(
        @StringRes id: Int,
    ) = onNodeWithText(fromId(id = id))

    internal fun fromId(@StringRes id: Int) =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id)
}

