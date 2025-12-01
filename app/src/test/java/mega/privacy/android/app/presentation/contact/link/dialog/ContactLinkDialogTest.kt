package mega.privacy.android.app.presentation.contact.link.dialog

import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.isNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import mega.privacy.android.app.R
import mega.privacy.android.app.onNodeWithText
import mega.privacy.android.domain.entity.contacts.ContactLinkQueryResult
import mega.privacy.android.shared.resources.R as sharedR
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions

@RunWith(AndroidJUnit4::class)
class ContactLinkDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private val inviteContact = mock<() -> Unit>()
    private val viewContact = mock<() -> Unit>()
    private val onDismiss = mock<() -> Unit>()
    private val email = "email@mega.io"
    private val fullName = "Full name"

    private fun initComposeTestRule(
        contactLinkQueryResult: ContactLinkQueryResult,
    ) {
        composeTestRule.setContent {
            ContactLinkDialog(
                uiState = ContactLinkDialogUiState(contactLinkQueryResult = contactLinkQueryResult),
                inviteContact = inviteContact,
                viewContact = viewContact,
                onDismiss = onDismiss,
                navigateToContactRequests = mock(),
            )
        }
    }

    private fun getContactLinkQueryResult(
        isContact: Boolean = false,
    ) = ContactLinkQueryResult(
        isContact = isContact,
        email = email,
        fullName = fullName,
    )

    @Test
    fun `test that dialog shows correctly if the user is a contact`() {
        initComposeTestRule(getContactLinkQueryResult(isContact = true))

        with(composeTestRule) {
            onNodeWithText(context.getString(sharedR.string.contact_found_dialog_title)).isDisplayed()
            onNodeWithText(
                context.getString(
                    R.string.context_contact_already_exists,
                    fullName
                )
            ).isDisplayed()
            onNodeWithText(fullName).isDisplayed()
            onNodeWithText(email).isDisplayed()
            onNodeWithText(R.string.contact_view).isDisplayed()
            onNodeWithText(sharedR.string.general_dialog_cancel_button).isDisplayed()
        }
    }

    @Test
    fun `test that dialog shows correctly if the user is NOT a contact`() {
        initComposeTestRule(getContactLinkQueryResult(isContact = false))

        with(composeTestRule) {
            onNodeWithText(context.getString(sharedR.string.general_invite_contact)).isDisplayed()
            onNodeWithText(
                context.getString(
                    R.string.context_contact_already_exists,
                    fullName
                )
            ).isNotDisplayed()
            onNodeWithText(fullName).isDisplayed()
            onNodeWithText(email).isDisplayed()
            onNodeWithText(sharedR.string.invite_contacts_action_label).isDisplayed()
            onNodeWithText(sharedR.string.general_dialog_cancel_button).isDisplayed()
        }
    }

    @Test
    fun `test that positive button behaves correctly if the user is a contact`() {
        initComposeTestRule(getContactLinkQueryResult(isContact = true))

        with(composeTestRule) {
            onNodeWithText(R.string.contact_view).apply {
                isDisplayed()
                performClick()
            }

            verify(onDismiss).invoke()
            verify(viewContact).invoke()
            verifyNoInteractions(inviteContact)
        }
    }

    @Test
    fun `test that positive button behaves correctly if the user is NOT a contact`() {
        initComposeTestRule(getContactLinkQueryResult(isContact = false))

        with(composeTestRule) {
            onNodeWithText(sharedR.string.invite_contacts_action_label).apply {
                isDisplayed()
                performClick()
            }

            verify(inviteContact).invoke()
            verifyNoInteractions(onDismiss)
            verifyNoInteractions(viewContact)
        }
    }

    @Test
    fun `test that negative button behaves correctly if the user is a contact`() {
        initComposeTestRule(getContactLinkQueryResult(isContact = true))

        with(composeTestRule) {
            onNodeWithText(sharedR.string.general_dialog_cancel_button).apply {
                isDisplayed()
                performClick()
            }

            verify(onDismiss).invoke()
            verifyNoInteractions(viewContact)
            verifyNoInteractions(inviteContact)
        }
    }

    @Test
    fun `test that negative button behaves correctly if the user is NOT a contact`() {
        initComposeTestRule(getContactLinkQueryResult(isContact = false))

        with(composeTestRule) {
            onNodeWithText(sharedR.string.general_dialog_cancel_button).apply {
                isDisplayed()
                performClick()
            }

            verify(onDismiss).invoke()
            verifyNoInteractions(viewContact)
            verifyNoInteractions(inviteContact)
        }
    }
}