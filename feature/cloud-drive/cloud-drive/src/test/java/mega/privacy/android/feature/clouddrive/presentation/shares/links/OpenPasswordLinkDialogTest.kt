package mega.privacy.android.feature.clouddrive.presentation.shares.links

import android.os.Build
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import mega.privacy.android.shared.resources.R as sharedR
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class OpenPasswordLinkDialogTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun `test that dialog is displayed with correct test tag`() {
        initComposeRuleContent()

        composeRule.onNodeWithTag(OPEN_PASSWORD_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that dialog displays correct title`() {
        initComposeRuleContent()

        val title = context.getString(sharedR.string.password_dialog_hint)
        composeRule.onNodeWithText(title).assertIsDisplayed()
    }

    @Test
    fun `test that dialog displays correct positive button text`() {
        initComposeRuleContent()

        val positiveButtonText = context.getString(sharedR.string.general_decrypt)
        composeRule.onNodeWithText(positiveButtonText).assertIsDisplayed()
    }

    @Test
    fun `test that dialog displays correct negative button text`() {
        initComposeRuleContent()

        val negativeButtonText = context.getString(sharedR.string.general_dialog_cancel_button)
        composeRule.onNodeWithText(negativeButtonText).assertIsDisplayed()
    }

    @Test
    fun `test that error text is displayed when errorText is provided`() {
        val errorText = "Invalid password"
        initComposeRuleContent(errorText = errorText)

        composeRule.onNodeWithText(errorText).assertIsDisplayed()
    }

    @Test
    fun `test that onConfirm is called when positive button is clicked`() {
        val onConfirm = mock<() -> Unit>()
        initComposeRuleContent(onConfirm = onConfirm)

        val positiveButtonText = context.getString(sharedR.string.general_decrypt)
        composeRule.onNodeWithText(positiveButtonText).performClick()

        verify(onConfirm).invoke()
    }

    @Test
    fun `test that onDismiss is called when negative button is clicked`() {
        val onDismiss = mock<() -> Unit>()
        initComposeRuleContent(onDismiss = onDismiss)

        val negativeButtonText = context.getString(sharedR.string.general_dialog_cancel_button)
        composeRule.onNodeWithText(negativeButtonText).performClick()

        verify(onDismiss).invoke()
    }

    @Test
    fun `test that onConfirm is not called when negative button is clicked`() {
        val onConfirm = mock<() -> Unit>()
        val onDismiss = mock<() -> Unit>()
        initComposeRuleContent(onConfirm = onConfirm, onDismiss = onDismiss)

        val negativeButtonText = context.getString(sharedR.string.general_dialog_cancel_button)
        composeRule.onNodeWithText(negativeButtonText).performClick()

        verify(onDismiss).invoke()
        verifyNoInteractions(onConfirm)
    }

    @Test
    fun `test that password value is shown`() {
        val password = "testPassword123"
        initComposeRuleContent(password = password)

        composeRule.onNodeWithText(password).assertIsDisplayed()
    }

    private fun initComposeRuleContent(
        password: String = "",
        errorText: String? = null,
        onValueChanged: (String) -> Unit = {},
        onConfirm: () -> Unit = {},
        onDismiss: () -> Unit = {},
    ) {
        composeRule.setContent {
            OpenPasswordLinkDialog(
                password = password,
                errorText = errorText,
                onValueChanged = onValueChanged,
                onConfirm = onConfirm,
                onDismiss = onDismiss,
            )
        }
    }
}

