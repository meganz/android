package mega.privacy.android.feature.transfers.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CompletedTransferBottomSheetHeaderTest {

    @get:Rule
    var composeRule = createComposeRule()

    @Test
    fun `test that header shows correctly`() {
        val completedTransferHeaderUI = CompletedTransferHeaderUI.default

        initComposeRuleContent(completedTransferHeaderUI)

        with(composeRule) {
            onNodeWithTag(TEST_TAG_COMPLETED_TRANSFER_SHEET_HEADER).assertIsDisplayed()
            onNodeWithTag(TEST_TAG_COMPLETED_TRANSFER_IMAGE).assertIsDisplayed()
            onNodeWithTag(TEST_TAG_COMPLETED_TRANSFER_NAME).assertIsDisplayed()
            onNodeWithText(completedTransferHeaderUI.fileName).assertIsDisplayed()
            onNodeWithText(completedTransferHeaderUI.size, substring = true).assertIsDisplayed()
        }
    }

    private fun initComposeRuleContent(completedTransferHeaderUI: CompletedTransferHeaderUI) =
        with(completedTransferHeaderUI) {
            composeRule.setContent {
                CompletedTransferBottomSheetHeader(
                    fileName = fileName,
                    size = size,
                    date = date,
                    fileTypeResId = fileTypeResId,
                    previewUri = null,
                )
            }
        }
}