package mega.privacy.android.data.mapper

import com.google.common.truth.Truth
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.contacts.InviteContactRequest
import mega.privacy.android.domain.exception.MegaException
import nz.mega.sdk.MegaContactRequest
import nz.mega.sdk.MegaError
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class InviteContactRequestMapperTest {
    private val underTest: InviteContactRequestMapper = InviteContactRequestMapper()

    private val getOutgoingContactRequests = mock<() -> List<MegaContactRequest>>()
    private val getIncomingContactRequests = mock<() -> List<MegaContactRequest>>()
    private val email = "email@domain.com"
    private val outgoingContactRequest = mock<MegaContactRequest> {
        on { targetEmail } doReturn email
    }
    private val incomingContactRequest = mock<MegaContactRequest> {
        on { sourceEmail } doReturn email
    }

    @ParameterizedTest(name = "test {0} is mapped correctly")
    @MethodSource("provideParameters")
    fun `test mapped correctly`(
        expected: InviteContactRequest,
        raw: MegaError,
        outgoingContactRequests: List<MegaContactRequest>,
        incomingContactRequests: List<MegaContactRequest>,
    ) = runTest {
        whenever(getOutgoingContactRequests()).thenReturn(outgoingContactRequests)
        whenever(getIncomingContactRequests()).thenReturn(incomingContactRequests)

        val actual = underTest(
            error = raw,
            email = email,
            getOutgoingContactRequests = getOutgoingContactRequests,
            getIncomingContactRequests = getIncomingContactRequests
        )

        Truth.assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test mapper throw MegaException`() = runTest {
        val error = mock<MegaError> { on { errorCode }.thenReturn(MegaError.API_EACCESS) }

        assertThrows<MegaException> {
            underTest(
                error, "",
                getOutgoingContactRequests = getOutgoingContactRequests,
                getIncomingContactRequests = getIncomingContactRequests
            )
        }
    }

    private fun provideParameters(): Stream<Arguments> = Stream.of(
        Arguments.of(
            InviteContactRequest.Sent,
            mock<MegaError> { on { errorCode }.thenReturn(MegaError.API_OK) },
            emptyList<MegaContactRequest>(),
            emptyList<MegaContactRequest>(),
        ),
        Arguments.of(
            InviteContactRequest.AlreadySent,
            mock<MegaError> { on { errorCode }.thenReturn(MegaError.API_EEXIST) },
            listOf(outgoingContactRequest),
            emptyList<MegaContactRequest>(),
        ),
        Arguments.of(
            InviteContactRequest.AlreadyReceived,
            mock<MegaError> { on { errorCode }.thenReturn(MegaError.API_EEXIST) },
            emptyList<MegaContactRequest>(),
            listOf(incomingContactRequest),
        ),
        Arguments.of(
            InviteContactRequest.AlreadyContact,
            mock<MegaError> { on { errorCode }.thenReturn(MegaError.API_EEXIST) },
            emptyList<MegaContactRequest>(),
            emptyList<MegaContactRequest>(),
        ),
        Arguments.of(
            InviteContactRequest.InvalidEmail,
            mock<MegaError> { on { errorCode }.thenReturn(MegaError.API_EARGS) },
            emptyList<MegaContactRequest>(),
            emptyList<MegaContactRequest>(),
        ),
    )
}