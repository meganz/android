package mega.privacy.android.app.presentation.contact.invite.mapper

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.app.R
import mega.privacy.android.app.main.model.InviteContactUiState.InvitationStatusMessageUiState.InvitationsSent
import mega.privacy.android.app.main.model.InviteContactUiState.InvitationStatusMessageUiState.NavigateUpWithResult
import mega.privacy.android.app.main.model.InviteContactUiState.MessageTypeUiState.Plural
import mega.privacy.android.app.main.model.InviteContactUiState.MessageTypeUiState.Singular
import mega.privacy.android.app.presentation.contact.invite.navigation.InviteContactScreenResult
import mega.privacy.android.domain.entity.contacts.InviteContactRequest
import mega.privacy.android.shared.resources.R as sharedR
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
    fun `test that the correct InvitationsSent is returned when there is only one email invitation`() {
        val requests = listOf(InviteContactRequest.Sent)
        val emails = listOf("email@email.com")
        val expected = InvitationsSent(
            messages = listOf(
                Singular(id = sharedR.string.contacts_invites_sent)
            ),
            actionId = R.string.tab_sent_requests,
        )

        assertThat(underTest(isFromAchievement = false, requests = requests, emails = emails))
            .isEqualTo(expected)
    }

    @Test
    fun `test that the correct InvitationsSent is returned when there are more than one email invitation`() {
        val requests = listOf(InviteContactRequest.Sent, InviteContactRequest.Sent)
        val emails = listOf("email@email.com", "email2@email.com")
        val expected = InvitationsSent(
            messages = listOf(
                Singular(id = sharedR.string.contacts_invites_sent)
            ),
            actionId = R.string.tab_sent_requests,
        )

        assertThat(underTest(isFromAchievement = false, requests = requests, emails = emails))
            .isEqualTo(expected)
    }

    @Test
    fun `test that the correct InvitationsSent is returned when there is only one email invitation and the user already sent you an invitation`() {
        val requests = listOf(InviteContactRequest.AlreadyReceived)
        val emails = listOf("email@email.com")
        val expected = InvitationsSent(
            messages = listOf(
                Singular(id = sharedR.string.contacts_invite_already_received)
            ),
            actionId = R.string.tab_received_requests,
        )

        assertThat(underTest(isFromAchievement = false, requests = requests, emails = emails))
            .isEqualTo(expected)
    }

    @Test
    fun `test that the correct InvitationsSent is returned when there are more than one email invitation and all the users already sent you an invitation`() {
        val requests =
            listOf(InviteContactRequest.AlreadyReceived, InviteContactRequest.AlreadyReceived)
        val emails = listOf("email@email.com", "email2@email.com")
        val expected = InvitationsSent(
            messages = listOf(
                Singular(id = sharedR.string.contacts_invites_already_received)
            ),
            actionId = R.string.tab_received_requests,
        )

        assertThat(underTest(isFromAchievement = false, requests = requests, emails = emails))
            .isEqualTo(expected)
    }

    @Test
    fun `test that the correct InvitationsSent is returned when there are more than one email invitation and some of the users already sent you an invitation`() {
        val requests =
            listOf(InviteContactRequest.Sent, InviteContactRequest.AlreadyReceived)
        val emails = listOf("email@email.com", "email2@email.com")
        val expected = InvitationsSent(
            messages = listOf(
                Plural(
                    id = sharedR.plurals.contacts_invites_sent_but_others_already_received,
                    quantity = 1,
                )
            ),
            actionId = R.string.tab_received_requests,
        )

        assertThat(underTest(isFromAchievement = false, requests = requests, emails = emails))
            .isEqualTo(expected)
    }

    @Test
    fun `test that a message displaying the total number of successful invitations and the total number of failed invitations is returned when there are failed invitations`() {
        val requests = listOf(InviteContactRequest.Sent, InviteContactRequest.InvalidEmail)
        val emails = listOf("email@email.com", "email2@email.com")
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
            ),
            actionId = R.string.tab_sent_requests,
        )

        assertThat(underTest(isFromAchievement = false, requests = requests, emails = emails))
            .isEqualTo(expected)
    }

    @Test
    fun `test that the screen navigates back with a result when the invitations originate from the achievement screen`() {
        val requests = listOf(InviteContactRequest.Sent, InviteContactRequest.Sent)
        val emails = listOf("email@email.com", "email2@email.com")
        val expected = NavigateUpWithResult(
            result = InviteContactScreenResult(
                key = InviteContactScreenResult.KEY_SENT_NUMBER,
                totalInvitationsSent = 2
            )
        )

        assertThat(underTest(isFromAchievement = true, requests = requests, emails = emails))
            .isEqualTo(expected)
    }
}
