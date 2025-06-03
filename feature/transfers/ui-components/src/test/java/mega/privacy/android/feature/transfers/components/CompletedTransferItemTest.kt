package mega.privacy.android.feature.transfers.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.icon.pack.R as iconPackR
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock

@RunWith(AndroidJUnit4::class)
class CompletedTransferItemTest {

    @get:Rule
    var composeRule = createComposeRule()

    private val name = "File name.pdf"
    private val downloadLocation = "63% of 1MB"
    private val uploadLocation = "Cloud Drive"
    private val uploadCompletedTransfer = CompletedTransferUI(
        isDownload = false,
        fileTypeResId = iconPackR.drawable.ic_pdf_medium_solid,
        previewUri = null,
        fileName = name,
        location = uploadLocation,
        error = null,
        sizeString = "10 MB",
        date = "10 Aug 2024 19:09",
    )

    @Test
    fun `test that successful download shows correctly`() {
        initComposeRuleContent(
            CompletedTransferUI(
                isDownload = true,
                fileTypeResId = iconPackR.drawable.ic_pdf_medium_solid,
                previewUri = null,
                fileName = name,
                location = downloadLocation,
                error = null,
                sizeString = "10 MB",
                date = "10 Aug 2024 19:09",
            )
        )
        with(composeRule) {
            onNodeWithTag(TEST_TAG_COMPLETED_TRANSFER_ITEM).assertIsDisplayed()
            onNodeWithTag(TEST_TAG_COMPLETED_TRANSFER_IMAGE).assertIsDisplayed()
            onNodeWithTag(TEST_TAG_COMPLETED_TRANSFER_NAME).assertIsDisplayed()
            onNodeWithText(name).assertIsDisplayed()
            onNodeWithText(downloadLocation).assertIsDisplayed()
        }
    }

    @Test
    fun `test that successful upload shows correctly`() {
        initComposeRuleContent(
            uploadCompletedTransfer
        )
        with(composeRule) {
            onNodeWithTag(TEST_TAG_COMPLETED_TRANSFER_ITEM).assertIsDisplayed()
            onNodeWithTag(TEST_TAG_COMPLETED_TRANSFER_IMAGE).assertIsDisplayed()
            onNodeWithTag(TEST_TAG_COMPLETED_TRANSFER_NAME).assertIsDisplayed()
            onNodeWithText(name).assertIsDisplayed()
            onNodeWithText(uploadLocation).assertIsDisplayed()
        }
    }

    @Test
    fun `test that selected active transfer shows correctly`() {
        initComposeRuleContent(
            uploadCompletedTransfer.copy(
                isSelected = true,
            )
        )
        with(composeRule) {
            onNodeWithTag(TEST_TAG_TRANSFER_SELECTED).assertIsDisplayed()
            onNodeWithTag(TEST_TAG_COMPLETED_TRANSFER_IMAGE).assertDoesNotExist()
        }
    }

    private fun initComposeRuleContent(completedTransferUI: CompletedTransferUI) =
        with(completedTransferUI) {
            composeRule.setContent {
                CompletedTransferItem(
                    isDownload = isDownload,
                    fileTypeResId = fileTypeResId,
                    previewUri = previewUri,
                    fileName = fileName,
                    location = location ?: "",
                    sizeString = sizeString,
                    date = date,
                    isSelected = isSelected,
                    onMoreClicked = mock(),
                )
            }
        }
}