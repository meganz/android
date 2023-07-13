package mega.privacy.android.data.mapper

import com.google.common.truth.Truth
import mega.privacy.android.domain.entity.contacts.InviteContactRequest
import mega.privacy.android.domain.exception.MegaException
import nz.mega.sdk.MegaError
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class InviteContactUseCaseRequestMapperTest {
    private val underTest: InviteContactRequestMapper = InviteContactRequestMapper()

    @ParameterizedTest(name = "test {0} is mapped correctly")
    @MethodSource("provideParameters")
    fun `test mapped correctly`(expected: InviteContactRequest, raw: MegaError) {
        val actual = underTest(raw, "", emptyList())
        Truth.assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test mapper throw MegaException`() {
        val error = mock<MegaError> { on { errorCode }.thenReturn(MegaError.API_EACCESS) }
        assertThrows<MegaException> {
            underTest(error, "", emptyList())
        }
    }

    private fun provideParameters(): Stream<Arguments> = Stream.of(
        Arguments.of(
            InviteContactRequest.Sent,
            mock<MegaError> { on { errorCode }.thenReturn(MegaError.API_OK) }),
        Arguments.of(
            InviteContactRequest.AlreadyContact,
            mock<MegaError> { on { errorCode }.thenReturn(MegaError.API_EEXIST) }),
        Arguments.of(
            InviteContactRequest.InvalidEmail,
            mock<MegaError> { on { errorCode }.thenReturn(MegaError.API_EARGS) }),
    )
}