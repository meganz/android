package mega.privacy.android.app.presentation.documentscanner.groups

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.presentation.documentscanner.model.ScanFileType
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

/**
 * Test class for [SaveScannedDocumentsFileTypeGroup]
 */
@RunWith(AndroidJUnit4::class)
internal class SaveScannedDocumentsFileTypeGroupTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `test that the ui components are displayed`() {
        composeTestRule.setContent {
            SaveScannedDocumentsFileTypeGroup(
                selectedScanFileType = ScanFileType.Pdf,
                onScanFileTypeSelected = {},
            )
        }

        composeTestRule.onNodeWithTag(SAVE_SCANNED_DOCUMENTS_FILE_TYPE_GROUP_HEADER)
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag(SAVE_SCANNED_DOCUMENTS_FILE_TYPE_GROUP_CHIP_PDF)
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag(SAVE_SCANNED_DOCUMENTS_FILE_TYPE_GROUP_CHIP_JPG)
            .assertIsDisplayed()
    }

    @Test
    fun `test that the PDF chip is selected`() {
        val onScanFileTypeSelected = mock<(ScanFileType) -> Unit>()
        composeTestRule.setContent {
            SaveScannedDocumentsFileTypeGroup(
                selectedScanFileType = ScanFileType.Pdf,
                onScanFileTypeSelected = onScanFileTypeSelected,
            )
        }

        composeTestRule.onNodeWithTag(SAVE_SCANNED_DOCUMENTS_FILE_TYPE_GROUP_CHIP_PDF)
            .performClick()

        verify(onScanFileTypeSelected).invoke(ScanFileType.Pdf)
    }

    @Test
    fun `test that the JPG chip is selected`() {
        val onScanFileTypeSelected = mock<(ScanFileType) -> Unit>()
        composeTestRule.setContent {
            SaveScannedDocumentsFileTypeGroup(
                selectedScanFileType = ScanFileType.Jpg,
                onScanFileTypeSelected = onScanFileTypeSelected,
            )
        }

        composeTestRule.onNodeWithTag(SAVE_SCANNED_DOCUMENTS_FILE_TYPE_GROUP_CHIP_JPG)
            .performClick()

        verify(onScanFileTypeSelected).invoke(ScanFileType.Jpg)
    }
}