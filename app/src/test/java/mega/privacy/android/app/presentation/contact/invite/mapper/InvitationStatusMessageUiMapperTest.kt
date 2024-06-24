package mega.privacy.android.app.presentation.contact.invite.mapper

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.app.R
import mega.privacy.android.app.main.model.InviteContactUiState.InvitationStatusMessageUiState.InvitationsSent
import mega.privacy.android.app.main.model.InviteContactUiState.InvitationStatusMessageUiState.NavigateUpWithResult
import mega.privacy.android.app.main.model.InviteContactUiState.MessageTypeUiState.Plural
import mega.privacy.android.app.main.model.InviteContactUiState.MessageTypeUiState.Singular
import mega.privacy.android.app.presentation.contact.invite.navigation.InviteContactScreenResult
import mega.privacy.android.domain.entity.contacts.InviteContactRequest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class InvitationStatusMessageUiMapperTest {

    private lateinit var underTest: InvitationStatusMessageUiMapper

    @BeforeEach
    fun setUp() {
        underTest = InvitationStatusMessageUiMapper()
    }

    @Test
    fun `test that a message with an email is returned when there is only one email invitation`() {
        val isFromAchievement = false
        val requests = listOf(InviteContactRequest.Sent)
        val emails = listOf("email@email.com")

        val actual = underTest(isFromAchievement, requests, emails)

        val expected = InvitationsSent(
            messages = listOf(
                Singular(
                    id = R.string.context_contact_request_sent,
                    argument = emails[0]
                )
            )
        )
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test that a message displaying the total number of successful invitations and the total number of failed invitations is returned when there are failed invitations`() {
        val isFromAchievement = false
        val requests = listOf(InviteContactRequest.Sent, InviteContactRequest.InvalidEmail)
        val emails = listOf("email@email.com", "email2@email.com")

        val actual = underTest(isFromAchievement, requests, emails)

        val expected = InvitationsSent(
            messages = listOf(
                Plural(
                    id = R.plurals.contact_snackbar_invite_contact_requests_sent,
                    quantity = 1,
                ),
                Plural(
                    id = R.plurals.contact_snackbar_invite_contact_requests_not_sent,
                    quantity = 1,
                )
            )
        )
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test that a message displaying the total number of successful invitations is returned after all invitations have been successfully sent`() {
        val isFromAchievement = false
        val requests = listOf(InviteContactRequest.Sent, InviteContactRequest.Sent)
        val emails = listOf("email@email.com", "email2@email.com")

        val actual = underTest(isFromAchievement, requests, emails)

        val expected = InvitationsSent(
            messages = listOf(
                Plural(
                    id = R.plurals.number_correctly_invite_contact_request,
                    quantity = emails.size,
                )
            )
        )
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test that the screen navigates back with a result when the invitations originate from the achievement screen`() {
        val isFromAchievement = true
        val requests = listOf(InviteContactRequest.Sent, InviteContactRequest.Sent)
        val emails = listOf("email@email.com", "email2@email.com")

        val actual = underTest(isFromAchievement, requests, emails)

        val expected = NavigateUpWithResult(
            result = InviteContactScreenResult(
                key = InviteContactScreenResult.KEY_SENT_NUMBER,
                totalInvitationsSent = 2
            )
        )
        assertThat(actual).isEqualTo(expected)
    }
}
