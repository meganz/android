package mega.privacy.android.app.presentation.documentscanner

import android.net.Uri
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.presentation.documentscanner.groups.SAVE_SCANNED_DOCUMENTS_DESTINATION_GROUP_CHIP_CHAT
import mega.privacy.android.app.presentation.documentscanner.groups.SAVE_SCANNED_DOCUMENTS_DESTINATION_GROUP_CHIP_CLOUD_DRIVE
import mega.privacy.android.app.presentation.documentscanner.groups.SAVE_SCANNED_DOCUMENTS_DESTINATION_GROUP_HEADER
import mega.privacy.android.app.presentation.documentscanner.groups.SAVE_SCANNED_DOCUMENTS_FILENAME_GROUP_EDIT_FILENAME_IMAGE
import mega.privacy.android.app.presentation.documentscanner.groups.SAVE_SCANNED_DOCUMENTS_FILENAME_GROUP_FILENAME_TEXT_FIELD
import mega.privacy.android.app.presentation.documentscanner.groups.SAVE_SCANNED_DOCUMENTS_FILENAME_GROUP_FILE_TYPE_IMAGE
import mega.privacy.android.app.presentation.documentscanner.groups.SAVE_SCANNED_DOCUMENTS_FILENAME_GROUP_HEADER
import mega.privacy.android.app.presentation.documentscanner.groups.SAVE_SCANNED_DOCUMENTS_FILE_TYPE_GROUP_CHIP_JPG
import mega.privacy.android.app.presentation.documentscanner.groups.SAVE_SCANNED_DOCUMENTS_FILE_TYPE_GROUP_CHIP_PDF
import mega.privacy.android.app.presentation.documentscanner.groups.SAVE_SCANNED_DOCUMENTS_FILE_TYPE_GROUP_HEADER
import mega.privacy.android.app.presentation.documentscanner.model.SaveScannedDocumentsUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

/**
 * Test class for [SaveScannedDocumentsView]
 */
@RunWith(AndroidJUnit4::class)
internal class SaveScannedDocumentsViewTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `test that all static ui components are displayed`() {
        composeTestRule.setContent {
            SaveScannedDocumentsView(
                uiState = SaveScannedDocumentsUiState(
                    filename = "Scanned_test_document"
                ),
                onFilenameChanged = {},
                onFilenameConfirmed = {},
                onSaveButtonClicked = {},
                onScanFileTypeSelected = {},
                onScanDestinationSelected = {},
                onSnackbarMessageConsumed = {},
                onUploadScansStarted = {},
                onUploadScansEventConsumed = {},
            )
        }

        composeTestRule.onNodeWithTag(SAVE_SCANNED_DOCUMENTS_TOOLBAR).assertIsDisplayed()
        composeTestRule.onNodeWithTag(SAVE_SCANNED_DOCUMENTS_FILENAME_GROUP_HEADER)
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag(SAVE_SCANNED_DOCUMENTS_FILENAME_GROUP_FILE_TYPE_IMAGE)
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag(SAVE_SCANNED_DOCUMENTS_FILENAME_GROUP_FILENAME_TEXT_FIELD)
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag(SAVE_SCANNED_DOCUMENTS_FILENAME_GROUP_EDIT_FILENAME_IMAGE)
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag(SAVE_SCANNED_DOCUMENTS_FILE_NAME_DIVIDER).assertIsDisplayed()
        composeTestRule.onNodeWithTag(SAVE_SCANNED_DOCUMENTS_DESTINATION_GROUP_HEADER)
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag(SAVE_SCANNED_DOCUMENTS_DESTINATION_GROUP_CHIP_CLOUD_DRIVE)
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag(SAVE_SCANNED_DOCUMENTS_DESTINATION_GROUP_CHIP_CHAT)
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag(SAVE_SCANNED_DOCUMENTS_DESTINATION_DIVIDER)
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag(SAVE_SCANNED_DOCUMENTS_SAVE_BUTTON).assertIsDisplayed()
    }

    @Test
    fun `test that the save button is clicked`() = runTest {
        val onSaveDestinationClicked = mock<() -> Unit>()
        composeTestRule.setContent {
            SaveScannedDocumentsView(
                uiState = SaveScannedDocumentsUiState(
                    filename = "Scanned_test_document"
                ),
                onFilenameChanged = {},
                onFilenameConfirmed = {},
                onSaveButtonClicked = onSaveDestinationClicked,
                onScanFileTypeSelected = {},
                onScanDestinationSelected = {},
                onSnackbarMessageConsumed = {},
                onUploadScansStarted = {},
                onUploadScansEventConsumed = {},
            )
        }

        composeTestRule.onNodeWithTag(SAVE_SCANNED_DOCUMENTS_SAVE_BUTTON).performClick()

        verify(onSaveDestinationClicked).invoke()
    }

    @Test
    fun `test that the file type selection is not shown when there is more than one scan`() =
        runTest {
            composeTestRule.setContent {
                SaveScannedDocumentsView(
                    uiState = SaveScannedDocumentsUiState(
                        filename = "Scanned_test_document",
                        soloImageUri = null,
                    ),
                    onFilenameChanged = {},
                    onFilenameConfirmed = {},
                    onSaveButtonClicked = {},
                    onScanFileTypeSelected = {},
                    onScanDestinationSelected = {},
                    onSnackbarMessageConsumed = {},
                    onUploadScansStarted = {},
                    onUploadScansEventConsumed = {},
                )
            }

            composeTestRule.onNodeWithTag(SAVE_SCANNED_DOCUMENTS_FILE_TYPE_GROUP_HEADER)
                .assertDoesNotExist()
            composeTestRule.onNodeWithTag(SAVE_SCANNED_DOCUMENTS_FILE_TYPE_GROUP_CHIP_PDF)
                .assertDoesNotExist()
            composeTestRule.onNodeWithTag(SAVE_SCANNED_DOCUMENTS_FILE_TYPE_GROUP_CHIP_JPG)
                .assertDoesNotExist()
        }

    @Test
    fun `test that the file type selection is shown when there is only one scan`() = runTest {
        val soloImageUri = mock<Uri> {
            on { toString() } doReturn "/data/user/0/app_location/cache/test_solo_scan.jpg"
        }
        composeTestRule.setContent {
            SaveScannedDocumentsView(
                uiState = SaveScannedDocumentsUiState(
                    filename = "Scanned_test_document",
                    soloImageUri = soloImageUri,
                ),
                onFilenameChanged = {},
                onFilenameConfirmed = {},
                onSaveButtonClicked = {},
                onScanFileTypeSelected = {},
                onScanDestinationSelected = {},
                onSnackbarMessageConsumed = {},
                onUploadScansStarted = {},
                onUploadScansEventConsumed = {},
            )
        }

        composeTestRule.onNodeWithTag(SAVE_SCANNED_DOCUMENTS_FILE_TYPE_GROUP_HEADER)
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag(SAVE_SCANNED_DOCUMENTS_FILE_TYPE_GROUP_CHIP_PDF)
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag(SAVE_SCANNED_DOCUMENTS_FILE_TYPE_GROUP_CHIP_JPG)
            .assertIsDisplayed()
    }
}