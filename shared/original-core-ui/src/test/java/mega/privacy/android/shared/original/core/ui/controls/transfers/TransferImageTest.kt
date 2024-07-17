package mega.privacy.android.shared.original.core.ui.controls.transfers

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.icon.pack.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TransferImageTest {

    @get:Rule
    var composeRule = createComposeRule()

    @Test
    fun `test that download transfer image shows correctly`() {
        initComposeRuleContent(true)
        with(composeRule) {
            onNodeWithTag(TEST_TAG_FILE_TYPE_ICON).assertIsDisplayed()
            onNodeWithTag(TEST_TAG_UPLOAD_LEADING_INDICATOR).assertDoesNotExist()
            onNodeWithTag(TEST_TAG_DOWNLOAD_LEADING_INDICATOR).assertIsDisplayed()
        }
    }

    @Test
    fun `test that upload transfer image shows correctly`() {
        initComposeRuleContent(false)
        with(composeRule) {
            onNodeWithTag(TEST_TAG_FILE_TYPE_ICON).assertIsDisplayed()
            onNodeWithTag(TEST_TAG_UPLOAD_LEADING_INDICATOR).assertIsDisplayed()
            onNodeWithTag(TEST_TAG_DOWNLOAD_LEADING_INDICATOR).assertDoesNotExist()
        }
    }

    private fun initComposeRuleContent(isDownload: Boolean) {
        composeRule.setContent {
            TransferImage(
                isDownload = isDownload,
                fileTypeResId = R.drawable.ic_pdf_medium_solid,
                previewUri = null,
            )
        }
    }
}