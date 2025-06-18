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

@RunWith(AndroidJUnit4::class)
class FailedTransferBottomSheetHeaderTest {

    @get:Rule
    var composeRule = createComposeRule()

    private val fileName = "File name.pdf"
    private val fileTypeRes = iconPackR.drawable.ic_pdf_medium_solid

    @Test
    fun `test that header shows correctly`() {
        val failedTransferHeaderUI = FailedTransferHeaderUI(
            fileName = fileName,
            info = "failed",
            fileTypeResId = fileTypeRes,
            previewUri = null,
            isError = false,
        )

        initComposeRuleContent(failedTransferHeaderUI)

        with(composeRule) {
            onNodeWithTag(TEST_TAG_COMPLETED_TRANSFER_SHEET_HEADER).assertIsDisplayed()
            onNodeWithTag(TEST_TAG_COMPLETED_TRANSFER_IMAGE).assertIsDisplayed()
            onNodeWithTag(TEST_TAG_COMPLETED_TRANSFER_NAME).assertIsDisplayed()
            onNodeWithText(failedTransferHeaderUI.fileName).assertIsDisplayed()
            onNodeWithText(failedTransferHeaderUI.info).assertIsDisplayed()
        }
    }

    private fun initComposeRuleContent(failedTransferHeaderUI: FailedTransferHeaderUI) =
        with(failedTransferHeaderUI) {
            composeRule.setContent {
                FailedTransferBottomSheetHeader(
                    fileName = fileName,
                    info = info,
                    fileTypeResId = fileTypeResId,
                    previewUri = previewUri,
                    isError = isError,
                )
            }
        }
}