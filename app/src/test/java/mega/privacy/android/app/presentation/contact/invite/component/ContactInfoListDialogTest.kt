package mega.privacy.android.app.presentation.contact.invite.component

import android.telephony.PhoneNumberUtils
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.app.main.InvitationContactInfo
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ContactInfoListDialogTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `test that the correct list of selected contact info is returned when the confirm button is clicked`() {
        val contactName = "contactName"
        val firstPhoneNumber = "123123123"
        val secondPhoneNumber = "321321321321"
        val thirdPhoneNumber = "412312321"
        val firstEmail = "email@email.email"
        val secondEmail = "test@test.test"
        val thirdEmail = "test@test.com"
        val filteredContactInfos = listOf(
            firstPhoneNumber,
            secondPhoneNumber,
            thirdPhoneNumber,
            firstEmail,
            secondEmail,
            thirdEmail
        )
        val contactInfo = InvitationContactInfo(
            name = contactName,
            filteredContactInfos = filteredContactInfos
        )
        val currentSelectedContactInfo = listOf(
            InvitationContactInfo(
                name = contactName,
                filteredContactInfos = filteredContactInfos,
                displayInfo = secondPhoneNumber
            ),
            InvitationContactInfo(
                name = contactName,
                filteredContactInfos = filteredContactInfos,
                displayInfo = thirdEmail
            )
        )
        with(composeRule) {
            var selectedContactInfo: List<InvitationContactInfo> = emptyList()
            setDialog(
                contactInfo = contactInfo,
                currentSelectedContactInfo = currentSelectedContactInfo,
                onConfirm = { selectedContactInfo = it }
            )

            onNodeWithTag(CONTACT_LIST_TAG)
                .performScrollToNode(hasText(firstPhoneNumber))
                .performClick()
            onNodeWithTag(CONTACT_LIST_TAG)
                .performScrollToNode(hasText(thirdEmail))
                .performClick()
            onNodeWithTag(OK_BUTTON_TAG).performClick()

            val expected = listOf(
                InvitationContactInfo(
                    name = contactName,
                    filteredContactInfos = filteredContactInfos,
                    displayInfo = secondPhoneNumber
                ),
                InvitationContactInfo(
                    name = contactName,
                    filteredContactInfos = filteredContactInfos,
                    displayInfo = firstPhoneNumber
                )
            )
            assertThat(selectedContactInfo).isEqualTo(expected)
        }
    }

    @Test
    fun `test that the contact format is displayed correctly`() {
        val contactName = "contactName"
        val phoneNumber = "(121)-234-567"
        val email = "email@email.email"
        val contactInfo = InvitationContactInfo(
            name = contactName,
            filteredContactInfos = listOf(phoneNumber, email)
        )
        with(composeRule) {
            setDialog(contactInfo = contactInfo)

            onNodeWithText(PhoneNumberUtils.stripSeparators(phoneNumber)).assertIsDisplayed()
            onNodeWithText(email).assertIsDisplayed()
        }
    }

    @Test
    fun `test that the cancel callback is called when the cancel button is clicked`() {
        with(composeRule) {
            var isCanceled = false
            setDialog(onCancel = { isCanceled = true })

            onNodeWithTag(CANCEL_BUTTON_TAG).performClick()

            assertThat(isCanceled).isTrue()
        }
    }

    private fun ComposeContentTestRule.setDialog(
        contactInfo: InvitationContactInfo = InvitationContactInfo(),
        currentSelectedContactInfo: List<InvitationContactInfo> = emptyList(),
        onConfirm: (selectedContactInfo: List<InvitationContactInfo>) -> Unit = {},
        onCancel: () -> Unit = {},
        onDismiss: () -> Unit = {},
    ) {
        setContent {
            ContactInfoListDialog(
                contactInfo = contactInfo,
                currentSelectedContactInfo = currentSelectedContactInfo,
                onConfirm = onConfirm,
                onCancel = onCancel,
                onDismiss = onDismiss
            )
        }
    }
}
