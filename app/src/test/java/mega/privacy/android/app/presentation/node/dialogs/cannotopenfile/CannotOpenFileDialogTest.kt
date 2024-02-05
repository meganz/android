package mega.privacy.android.app.presentation.node.dialogs.cannotopenfile

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.R
import org.junit.Rule
import org.junit.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CannotOpenFileDialogTest {
    @get:Rule
    var composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `test that correct dialog is shown`() {
        initComposeRuleContent()
        with(composeRule) {
            onNodeWithTag(TEST_TAG_CANNOT_OPEN_FILE_DIALOG_TAG).assertIsDisplayed()
            onNodeWithText(activity.getString(R.string.dialog_cannot_open_file_title))
                .assertIsDisplayed()
            onNodeWithText(activity.getString(R.string.dialog_cannot_open_file_text))
                .assertIsDisplayed()
            onNodeWithText(activity.getString(R.string.context_download)).assertIsDisplayed()
            onNodeWithText(activity.getString(R.string.general_cancel)).assertIsDisplayed()
        }
    }

    fun initComposeRuleContent() {
        composeRule.setContent {
            CannotOpenFileDialog(
                onDismiss = {},
                onDownload = {},
            )
        }

    }
}
