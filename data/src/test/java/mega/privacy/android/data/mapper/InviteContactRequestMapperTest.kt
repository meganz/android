package mega.privacy.android.data.mapper

import com.google.common.truth.Truth
import mega.privacy.android.domain.entity.contacts.InviteContactRequest
import nz.mega.sdk.MegaError
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class InviteContactRequestMapperTest {
    private val underTest: InviteContactRequestMapper = InviteContactRequestMapper()

    @ParameterizedTest(name = "test {0} is mapped correctly")
    @MethodSource("provideParameters")
    fun `test mapped correctly`(expected: InviteContactRequest, raw: MegaError) {
        val actual = underTest(raw)
        Truth.assertThat(actual).isEqualTo(expected)
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
        Arguments.of(
            InviteContactRequest.InvalidStatus,
            mock<MegaError> { on { errorCode }.thenReturn(MegaError.API_EACCESS) })
    )
}