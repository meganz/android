package mega.privacy.android.feature.signin.external.data.repository.security

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.login.GoogleSignInResult
import mega.privacy.android.domain.exception.login.GoogleSignInException
import mega.privacy.android.feature.signin.external.data.mapper.login.GoogleIdTokenMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultGoogleSignInRepositoryTest {

    private val googleIdTokenMapper = mock<GoogleIdTokenMapper>()

    private lateinit var underTest: DefaultGoogleSignInRepository

    @BeforeEach
    fun setUp() {
        reset(googleIdTokenMapper)
        underTest = DefaultGoogleSignInRepository(
            googleIdTokenMapper = googleIdTokenMapper,
        )
    }

    @Test
    fun `test that signIn returns GoogleSignInResult when mapper succeeds`() = runTest {
        val idToken = "header.payload.sig"
        val expected = GoogleSignInResult(
            email = "user@gmail.com",
            sub = "12345",
            firstName = "John",
            lastName = "Doe",
        )
        whenever(googleIdTokenMapper(idToken)).thenReturn(expected)

        val result = underTest.signIn(idToken)

        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `test that signIn throws GoogleSignInException Unknown when mapper throws`() = runTest {
        val idToken = "header.payload.sig"
        whenever(googleIdTokenMapper(idToken)).thenThrow(IllegalArgumentException("Missing required field: email"))

        assertThrows<GoogleSignInException.Unknown> { underTest.signIn(idToken) }
    }
}
