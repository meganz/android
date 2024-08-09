package mega.privacy.android.shared.original.core.ui.controls.transfers

import mega.privacy.android.icon.pack.R as iconPackR
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import mega.privacy.android.core.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CompletedTransferItemTest {

    @get:Rule
    var composeRule = createComposeRule()

    private val name = "File name.pdf"
    private val downloadLocation = "63% of 1MB"
    private val uploadLocation = "Cloud Drive"
    private val error = "Failed"

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
            )
        )
        with(composeRule) {
            onNodeWithTag(TEST_TAG_COMPLETED_TRANSFER_ITEM).assertIsDisplayed()
            onNodeWithTag(TEST_TAG_COMPLETED_TRANSFER_IMAGE).assertIsDisplayed()
            onNodeWithTag(TEST_TAG_COMPLETED_TRANSFER_NAME).assertIsDisplayed()
            onNodeWithText(name).assertIsDisplayed()
            onNodeWithTag(TEST_TAG_COMPLETED_TRANSFER_SUCCESS_ICON).assertIsDisplayed()
            onNodeWithText(downloadLocation).assertIsDisplayed()
        }
    }

    @Test
    fun `test that failed download shows correctly`() {
        initComposeRuleContent(
            CompletedTransferUI(
                isDownload = true,
                fileTypeResId = iconPackR.drawable.ic_pdf_medium_solid,
                previewUri = null,
                fileName = name,
                location = null,
                error = error,
            )
        )
        with(composeRule) {
            onNodeWithTag(TEST_TAG_COMPLETED_TRANSFER_ITEM).assertIsDisplayed()
            onNodeWithTag(TEST_TAG_COMPLETED_TRANSFER_IMAGE).assertIsDisplayed()
            onNodeWithTag(TEST_TAG_COMPLETED_TRANSFER_NAME).assertIsDisplayed()
            onNodeWithText(name).assertIsDisplayed()
            onNodeWithTag(TEST_TAG_COMPLETED_TRANSFER_SUCCESS_ICON).assertDoesNotExist()
            onNodeWithText(error).assertIsDisplayed()
        }
    }

    @Test
    fun `test that cancelled download shows correctly`() {
        initComposeRuleContent(
            CompletedTransferUI(
                isDownload = true,
                fileTypeResId = iconPackR.drawable.ic_pdf_medium_solid,
                previewUri = null,
                fileName = name,
                location = null,
                error = null,
            )
        )
        with(composeRule) {
            onNodeWithTag(TEST_TAG_COMPLETED_TRANSFER_ITEM).assertIsDisplayed()
            onNodeWithTag(TEST_TAG_COMPLETED_TRANSFER_IMAGE).assertIsDisplayed()
            onNodeWithTag(TEST_TAG_COMPLETED_TRANSFER_NAME).assertIsDisplayed()
            onNodeWithText(name).assertIsDisplayed()
            onNodeWithTag(TEST_TAG_COMPLETED_TRANSFER_SUCCESS_ICON).assertDoesNotExist()
            onNodeWithText(InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.transfer_cancelled)).assertIsDisplayed()
        }
    }

    @Test
    fun `test that successful upload shows correctly`() {
        initComposeRuleContent(
            CompletedTransferUI(
                isDownload = false,
                fileTypeResId = iconPackR.drawable.ic_pdf_medium_solid,
                previewUri = null,
                fileName = name,
                location = uploadLocation,
                error = null,
            )
        )
        with(composeRule) {
            onNodeWithTag(TEST_TAG_COMPLETED_TRANSFER_ITEM).assertIsDisplayed()
            onNodeWithTag(TEST_TAG_COMPLETED_TRANSFER_IMAGE).assertIsDisplayed()
            onNodeWithTag(TEST_TAG_COMPLETED_TRANSFER_NAME).assertIsDisplayed()
            onNodeWithText(name).assertIsDisplayed()
            onNodeWithTag(TEST_TAG_COMPLETED_TRANSFER_SUCCESS_ICON).assertIsDisplayed()
            onNodeWithText(uploadLocation).assertIsDisplayed()
        }
    }

    @Test
    fun `test that failed upload shows correctly`() {
        initComposeRuleContent(
            CompletedTransferUI(
                isDownload = false,
                fileTypeResId = iconPackR.drawable.ic_pdf_medium_solid,
                previewUri = null,
                fileName = name,
                location = null,
                error = error,
            )
        )
        with(composeRule) {
            onNodeWithTag(TEST_TAG_COMPLETED_TRANSFER_ITEM).assertIsDisplayed()
            onNodeWithTag(TEST_TAG_COMPLETED_TRANSFER_IMAGE).assertIsDisplayed()
            onNodeWithTag(TEST_TAG_COMPLETED_TRANSFER_NAME).assertIsDisplayed()
            onNodeWithText(name).assertIsDisplayed()
            onNodeWithTag(TEST_TAG_COMPLETED_TRANSFER_SUCCESS_ICON).assertDoesNotExist()
            onNodeWithText(error).assertIsDisplayed()
        }
    }

    @Test
    fun `test that cancelled upload shows correctly`() {
        initComposeRuleContent(
            CompletedTransferUI(
                isDownload = false,
                fileTypeResId = iconPackR.drawable.ic_pdf_medium_solid,
                previewUri = null,
                fileName = name,
                location = null,
                error = null,
            )
        )
        with(composeRule) {
            onNodeWithTag(TEST_TAG_COMPLETED_TRANSFER_ITEM).assertIsDisplayed()
            onNodeWithTag(TEST_TAG_COMPLETED_TRANSFER_IMAGE).assertIsDisplayed()
            onNodeWithTag(TEST_TAG_COMPLETED_TRANSFER_NAME).assertIsDisplayed()
            onNodeWithText(name).assertIsDisplayed()
            onNodeWithTag(TEST_TAG_COMPLETED_TRANSFER_SUCCESS_ICON).assertDoesNotExist()
            onNodeWithText(InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.transfer_cancelled)).assertIsDisplayed()
        }
    }

    private fun initComposeRuleContent(completedTransferUI: CompletedTransferUI) =
        with(completedTransferUI) {
            composeRule.setContent {
                CompletedTransferItem(
                    isDownload,
                    fileTypeResId,
                    previewUri,
                    fileName,
                    location,
                    error,
                )
            }
        }
}