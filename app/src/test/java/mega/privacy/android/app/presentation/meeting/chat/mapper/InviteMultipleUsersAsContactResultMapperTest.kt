package mega.privacy.android.app.presentation.meeting.chat.mapper

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.app.presentation.meeting.chat.model.messages.InviteMultipleUsersAsContactResult
import mega.privacy.android.domain.entity.contacts.InviteContactRequest
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class InviteMultipleUsersAsContactResultMapperTest {
    lateinit var underTest: InviteMultipleUsersAsContactResultMapper

    @BeforeAll
    fun setup() {
        underTest = InviteMultipleUsersAsContactResultMapper()
    }

    @ParameterizedTest(name = "{1} is mapped correctly")
    @MethodSource("provideParameters")
    fun `test that `(
        resultList: List<InviteContactRequest>,
        expected: InviteMultipleUsersAsContactResult,
    ) {
        assertThat(underTest(resultList)).isEqualTo(expected)
    }

    private fun provideParameters(): Stream<Arguments?>? {
        return Stream.of(
            Arguments.of(
                listOf(
                    InviteContactRequest.Sent,
                    InviteContactRequest.Sent
                ), InviteMultipleUsersAsContactResult.AllSent(2)
            ),
            Arguments.of(
                listOf(
                    InviteContactRequest.AlreadySent,
                    InviteContactRequest.AlreadyContact,
                    InviteContactRequest.Sent
                ), InviteMultipleUsersAsContactResult.SomeAlreadyRequestedSomeSent(1, 1)
            ),
            Arguments.of(
                listOf(
                    InviteContactRequest.InvalidEmail,
                    InviteContactRequest.Sent
                ), InviteMultipleUsersAsContactResult.SomeFailedSomeSent(1, 1)
            ),
        )
    }
}
