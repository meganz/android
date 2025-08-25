package mega.privacy.android.app.presentation.login.confirmemail.mapper

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.app.presentation.login.confirmemail.model.ResendSignUpLinkError
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.exception.account.CreateAccountException
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ResendSignUpLinkErrorMapperTest {

    private lateinit var underTest: ResendSignUpLinkErrorMapper

    @BeforeEach
    fun setup() {
        underTest = ResendSignUpLinkErrorMapper()
    }

    @ParameterizedTest
    @MethodSource("provideException")
    fun `test that exception is successfully mapped to ResendSignUpLink`(exception: Throwable) {
        val actual = underTest(exception = exception)

        val expected = when (exception) {
            is CreateAccountException.AccountAlreadyExists -> ResendSignUpLinkError.AccountExists
            is CreateAccountException.TooManyAttemptsException -> ResendSignUpLinkError.TooManyAttempts
            else -> ResendSignUpLinkError.Unknown
        }
        assertThat(actual).isEqualTo(expected)
    }

    private fun provideException() = Stream.of(
        Arguments.of(CreateAccountException.AccountAlreadyExists),
        Arguments.of(CreateAccountException.TooManyAttemptsException),
        Arguments.of(CreateAccountException.Unknown(MegaException(1, null))),
        Arguments.of(RuntimeException()),
    )
}
