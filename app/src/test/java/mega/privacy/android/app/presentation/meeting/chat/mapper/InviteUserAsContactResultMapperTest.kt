package mega.privacy.android.app.presentation.meeting.chat.mapper

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.app.presentation.meeting.chat.model.InviteUserAsContactResult
import mega.privacy.android.domain.entity.contacts.InviteContactRequest
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class InviteUserAsContactResultMapperTest {

    lateinit var underTest: InviteUserAsContactResultMapper

    @BeforeAll
    fun setup() {
        underTest = InviteUserAsContactResultMapper()
    }

    private val fakeEmail = "fakemail@b.c"

    @ParameterizedTest(name = "{0} is mapped to {1} correctly")
    @MethodSource("provideParameters")
    fun `test that `(
        inviteContactRequest: InviteContactRequest,
        expectedResult: InviteUserAsContactResult,
    ) {
        assertThat(
            underTest(
                inviteContactRequest = inviteContactRequest,
                email = fakeEmail
            )
        ).isEqualTo(expectedResult)
    }

    private fun provideParameters(): Stream<Arguments?>? {
        return Stream.of(
            Arguments.of(InviteContactRequest.Sent, InviteUserAsContactResult.ContactInviteSent),
            Arguments.of(
                InviteContactRequest.AlreadySent,
                InviteUserAsContactResult.ContactAlreadyInvitedError(email = fakeEmail)
            ),
            Arguments.of(
                InviteContactRequest.AlreadyContact,
                InviteUserAsContactResult.ContactAlreadyInvitedError(email = fakeEmail)
            ),
            Arguments.of(
                InviteContactRequest.InvalidEmail,
                InviteUserAsContactResult.OwnEmailAsContactError
            ),
            Arguments.of(
                InviteContactRequest.InvalidStatus,
                InviteUserAsContactResult.GeneralError
            ),
            Arguments.of(InviteContactRequest.Deleted, InviteUserAsContactResult.GeneralError),
            Arguments.of(InviteContactRequest.Resent, InviteUserAsContactResult.GeneralError),
        )
    }
}

