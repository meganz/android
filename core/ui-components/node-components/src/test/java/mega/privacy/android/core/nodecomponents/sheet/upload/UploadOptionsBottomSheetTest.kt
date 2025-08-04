package mega.privacy.android.core.nodecomponents.sheet.upload

import androidx.activity.ComponentActivity
import androidx.annotation.StringRes
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import mega.privacy.android.core.nodecomponents.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class UploadOptionsBottomSheetTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val onUploadFilesClicked = mock<() -> Unit>()
    private val onUploadFolderClicked = mock<() -> Unit>()
    private val onScanDocumentClicked = mock<() -> Unit>()
    private val onCaptureClicked = mock<() -> Unit>()
    private val onNewFolderClicked = mock<() -> Unit>()
    private val onNewTextFileClicked = mock<() -> Unit>()
    private val onDismissSheet = mock<() -> Unit>()

    @Test
    fun `test that sheet shows correctly`() {
        initComposeTestRule()

        with(composeTestRule) {
            onNodeWithTag(TEST_TAG_UPLOAD_OPTIONS_SHEET).assertIsDisplayed()
            onNodeWithTag(TEST_TAG_UPLOAD_FILES_ACTION).assertIsDisplayed()
            onNodeWithTag(TEST_TAG_UPLOAD_FOLDER_ACTION).assertIsDisplayed()
            onNodeWithTag(TEST_TAG_SCAN_DOCUMENT_ACTION).assertIsDisplayed()
            onNodeWithTag(TEST_TAG_CAPTURE_ACTION).assertIsDisplayed()
            onNodeWithTag(TEST_TAG_NEW_FOLDER_ACTION).assertIsDisplayed()
            onNodeWithTag(TEST_TAG_NEW_TEXT_FILE_ACTION).assertIsDisplayed()
        }
    }

    @Test
    fun `test that all text items are displayed`() {
        initComposeTestRule()

        with(composeTestRule) {
            onNodeWithText(R.string.upload_files).assertIsDisplayed()
            onNodeWithText(R.string.upload_folder).assertIsDisplayed()
            onNodeWithText(R.string.menu_scan_document).assertIsDisplayed()
            onNodeWithText(R.string.menu_take_picture).assertIsDisplayed()
            onNodeWithText(R.string.menu_new_folder).assertIsDisplayed()
            onNodeWithText(R.string.action_create_txt).assertIsDisplayed()
        }
    }

    @Test
    fun `test that clicking on upload files option invokes onUploadFilesClicked and onDismissSheet`() {
        initComposeTestRule()

        composeTestRule.onNodeWithTag(TEST_TAG_UPLOAD_FILES_ACTION)
            .performSemanticsAction(SemanticsActions.OnClick)

        verify(onUploadFilesClicked).invoke()
        verify(onDismissSheet).invoke()
    }

    @Test
    fun `test that clicking on upload folder option invokes onUploadFolderClicked and onDismissSheet`() {
        initComposeTestRule()

        composeTestRule.onNodeWithTag(TEST_TAG_UPLOAD_FOLDER_ACTION)
            .performSemanticsAction(SemanticsActions.OnClick)

        verify(onUploadFolderClicked).invoke()
        verify(onDismissSheet).invoke()
    }

    @Test
    fun `test that clicking on scan document option invokes onScanDocumentClicked and onDismissSheet`() {
        initComposeTestRule()

        composeTestRule.onNodeWithTag(TEST_TAG_SCAN_DOCUMENT_ACTION)
            .performSemanticsAction(SemanticsActions.OnClick)

        verify(onScanDocumentClicked).invoke()
        verify(onDismissSheet).invoke()
    }

    @Test
    fun `test that clicking on capture option invokes onCaptureClicked and onDismissSheet`() {
        initComposeTestRule()

        composeTestRule.onNodeWithTag(TEST_TAG_CAPTURE_ACTION)
            .performSemanticsAction(SemanticsActions.OnClick)

        verify(onCaptureClicked).invoke()
        verify(onDismissSheet).invoke()
    }

    @Test
    fun `test that clicking on new folder option invokes onNewFolderClicked and onDismissSheet`() {
        initComposeTestRule()

        composeTestRule.onNodeWithTag(TEST_TAG_NEW_FOLDER_ACTION)
            .performSemanticsAction(SemanticsActions.OnClick)

        verify(onNewFolderClicked).invoke()
        verify(onDismissSheet).invoke()
    }

    @Test
    fun `test that clicking on new text file option invokes onNewTextFileClicked and onDismissSheet`() {
        initComposeTestRule()

        composeTestRule.onNodeWithTag(TEST_TAG_NEW_TEXT_FILE_ACTION)
            .performSemanticsAction(SemanticsActions.OnClick)

        verify(onNewTextFileClicked).invoke()
        verify(onDismissSheet).invoke()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    private fun initComposeTestRule() {
        composeTestRule.setContent {
            UploadOptionsBottomSheet(
                onUploadFilesClicked = onUploadFilesClicked,
                onUploadFolderClicked = onUploadFolderClicked,
                onScanDocumentClicked = onScanDocumentClicked,
                onCaptureClicked = onCaptureClicked,
                onNewFolderClicked = onNewFolderClicked,
                onNewTextFileClicked = onNewTextFileClicked,
                onDismissSheet = onDismissSheet,
            )
        }
    }

    internal fun SemanticsNodeInteractionsProvider.onNodeWithText(
        @StringRes id: Int,
    ) = onNodeWithText(fromId(id = id))

    internal fun fromId(@StringRes id: Int) =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id)
}