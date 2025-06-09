package mega.privacy.android.feature.transfers.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.shared.resources.R as sharedR
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FailedTransferItemTest {

    @get:Rule
    var composeRule = createComposeRule()

    private val name = "File name.pdf"
    private val error = "Failed"

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
                sizeString = "10 MB",
                date = "10 Aug 2024 19:09",
            )
        )
        with(composeRule) {
            onNodeWithTag(TEST_TAG_COMPLETED_TRANSFER_ITEM).assertIsDisplayed()
            onNodeWithTag(TEST_TAG_COMPLETED_TRANSFER_IMAGE).assertIsDisplayed()
            onNodeWithTag(TEST_TAG_COMPLETED_TRANSFER_NAME).assertIsDisplayed()
            onNodeWithText(name).assertIsDisplayed()
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
                sizeString = "10 MB",
                date = "10 Aug 2024 19:09",
            )
        )
        with(composeRule) {
            onNodeWithTag(TEST_TAG_COMPLETED_TRANSFER_ITEM).assertIsDisplayed()
            onNodeWithTag(TEST_TAG_COMPLETED_TRANSFER_IMAGE).assertIsDisplayed()
            onNodeWithTag(TEST_TAG_COMPLETED_TRANSFER_NAME).assertIsDisplayed()
            onNodeWithText(name).assertIsDisplayed()
            onNodeWithText(
                InstrumentationRegistry.getInstrumentation().targetContext.getString(sharedR.string.transfers_section_cancelled)
            ).assertIsDisplayed()
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
                sizeString = "10 MB",
                date = "10 Aug 2024 19:09",
            )
        )
        with(composeRule) {
            onNodeWithTag(TEST_TAG_COMPLETED_TRANSFER_ITEM).assertIsDisplayed()
            onNodeWithTag(TEST_TAG_COMPLETED_TRANSFER_IMAGE).assertIsDisplayed()
            onNodeWithTag(TEST_TAG_COMPLETED_TRANSFER_NAME).assertIsDisplayed()
            onNodeWithText(name).assertIsDisplayed()
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
                sizeString = "10 MB",
                date = "10 Aug 2024 19:09",
            )
        )
        with(composeRule) {
            onNodeWithTag(TEST_TAG_COMPLETED_TRANSFER_ITEM).assertIsDisplayed()
            onNodeWithTag(TEST_TAG_COMPLETED_TRANSFER_IMAGE).assertIsDisplayed()
            onNodeWithTag(TEST_TAG_COMPLETED_TRANSFER_NAME).assertIsDisplayed()
            onNodeWithText(name).assertIsDisplayed()
            onNodeWithText(
                InstrumentationRegistry.getInstrumentation().targetContext.getString(sharedR.string.transfers_section_cancelled)
            ).assertIsDisplayed()
        }
    }

    @Test
    fun `test that selected active transfer shows correctly`() {
        initComposeRuleContent(
            CompletedTransferUI(
                isDownload = false,
                fileTypeResId = iconPackR.drawable.ic_pdf_medium_solid,
                previewUri = null,
                fileName = name,
                location = null,
                error = null,
                sizeString = "10 MB",
                date = "10 Aug 2024 19:09",
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
                FailedTransferItem(
                    isDownload = isDownload,
                    fileTypeResId = fileTypeResId,
                    previewUri = previewUri,
                    fileName = fileName,
                    error = error,
                    isSelected = isSelected,
                )
            }
        }
}