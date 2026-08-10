package mega.privacy.android.core.sharedcomponents.dialog

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.sharedcomponents.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SecurityUpgradeDialogViewTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val testTitleText = "Account security upgrade"
    private val testContentText = "We are upgrading the security of your account."
    private val testOkButtonText = "OK"

    private fun initComposeRule(
        titleText: String = testTitleText,
        contentText: String = testContentText,
        okButtonText: String = testOkButtonText,
    ) {
        composeTestRule.setContent {
            AndroidTheme(isDark = isSystemInDarkTheme()) {
                SecurityUpgradeDialogView(
                    titleText = titleText,
                    contentText = contentText,
                    okButtonText = okButtonText,
                    imageVector = ImageVector.vectorResource(id = R.drawable.ic_security_upgrade),
                    onOkClick = {},
                    onCloseClick = {},
                )
            }
        }
    }

    @Test
    fun `test that image view is displayed`() {
        initComposeRule()
        composeTestRule.onNodeWithTag("HeaderImage").assertIsDisplayed()
    }

    @Test
    fun `test that title text is displayed`() {
        initComposeRule()
        composeTestRule.onNodeWithText(testTitleText).assertIsDisplayed()
    }

    @Test
    fun `test that content text is displayed`() {
        initComposeRule()
        composeTestRule.onNodeWithText(testContentText).assertIsDisplayed()
    }

    @Test
    fun `test that ok button text is displayed`() {
        initComposeRule()
        composeTestRule.onNodeWithText(testOkButtonText).assertIsDisplayed()
    }

    @Test
    fun `test that all elements are displayed together`() {
        initComposeRule()
        composeTestRule.run {
            onNodeWithTag("HeaderImage").assertIsDisplayed()
            onNodeWithText(testTitleText).assertIsDisplayed()
            onNodeWithText(testContentText).assertIsDisplayed()
            onNodeWithText(testOkButtonText).assertIsDisplayed()
        }
    }

    @Test
    fun `test that dialog displays custom title text`() {
        val customTitle = "Custom Title"
        initComposeRule(titleText = customTitle)
        composeTestRule.onNodeWithText(customTitle).assertIsDisplayed()
    }

    @Test
    fun `test that dialog displays custom content text`() {
        val customContent = "Custom content message"
        initComposeRule(contentText = customContent)
        composeTestRule.onNodeWithText(customContent).assertIsDisplayed()
    }

    @Test
    fun `test that dialog displays custom button text`() {
        val customButtonText = "Confirm"
        initComposeRule(okButtonText = customButtonText)
        composeTestRule.onNodeWithText(customButtonText).assertIsDisplayed()
    }
}
