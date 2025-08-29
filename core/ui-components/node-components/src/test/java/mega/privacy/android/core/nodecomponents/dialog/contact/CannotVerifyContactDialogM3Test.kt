package mega.privacy.android.core.nodecomponents.dialog.contact

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyChild
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.printToString
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import mega.privacy.android.core.nodecomponents.dialog.contact.CannotVerifyContactDialogM3
import mega.privacy.android.core.nodecomponents.dialog.contact.CANNOT_VERIFY_DIALOG_TAG
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import mega.privacy.android.shared.resources.R as sharedResR

@RunWith(AndroidJUnit4::class)
class CannotVerifyContactDialogM3Test {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `test that dialog displays email in description`() {
        val testEmail = "test@example.com"
        val expectedText = InstrumentationRegistry
            .getInstrumentation()
            .targetContext
            .getString(
                sharedResR.string.shared_items_contact_not_in_contact_list_dialog_content,
                testEmail
            )
        val onDismiss = mock<() -> Unit>()

        composeTestRule.setContent {
            CannotVerifyContactDialogM3(
                email = testEmail,
                onDismiss = onDismiss
            )
        }

        composeTestRule
            .onNodeWithTag(CANNOT_VERIFY_DIALOG_TAG)
            .assertIsDisplayed()

        composeTestRule.onNodeWithText(expectedText).assertIsDisplayed()
    }
}
