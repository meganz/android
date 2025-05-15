package mega.privacy.android.feature.transfers.components

import android.net.Uri
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
    fun `test that type icon is shows when there's no uri`() {
        initComposeRuleContent()
        with(composeRule) {
            onNodeWithTag(TEST_TAG_FILE_TYPE_ICON).assertIsDisplayed()
        }
    }

    @Test
    fun `test that thumbnail is shown when there's an uri`() {
        initComposeRuleContent(Uri.parse("foo"))
        with(composeRule) {
            onNodeWithTag(TEST_TAG_FILE_THUMBNAIL).assertIsDisplayed()
        }
    }

    private fun initComposeRuleContent(previewUri: Uri? = null) {
        composeRule.setContent {
            TransferImage(
                fileTypeResId = R.drawable.ic_pdf_medium_solid,
                previewUri = previewUri,
            )
        }
    }
}