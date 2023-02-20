package test.mega.privacy.android.app.presentation.fingerprintauth

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidTest
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.fingerprintauth.SecurityUpgradeDialogView
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import test.mega.privacy.android.app.onNodeWithText

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class SecurityUpgradeDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun initComposeRule() {
        composeTestRule.setContent {
            SecurityUpgradeDialogView(folderNames = listOf("folder name 1 ",
                "folder name 2 ",
                "folder name 3 "),
                onOkClick = { },
                onCancelClick = {})
        }
    }

    @Test
    fun test_that_imageview_resource_is_as_expected() {
        initComposeRule()
        composeTestRule.run {
            onNodeWithTag("HeaderImage").assertIsDisplayed()
            onNodeWithText(R.string.shared_items_security_upgrade_dialog_title).assertIsDisplayed()
            onNodeWithText(R.string.shared_items_security_upgrade_dialog_content).assertIsDisplayed()
            onNodeWithTag("SharedNodeInfo").assertIsDisplayed()
            onNodeWithText(R.string.general_ok).assertIsDisplayed()
            onNodeWithText(R.string.button_cancel).assertIsDisplayed()
        }
    }
}