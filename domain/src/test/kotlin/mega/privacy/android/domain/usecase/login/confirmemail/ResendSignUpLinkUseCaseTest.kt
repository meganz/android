package mega.privacy.android.domain.usecase.login.confirmemail

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.security.LoginRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ResendSignUpLinkUseCaseTest {

    private lateinit var underTest: ResendSignUpLinkUseCase

    private val loginRepository: LoginRepository = mock()

    @BeforeEach
    fun setUp() {
        underTest = ResendSignUpLinkUseCase(loginRepository)
    }

    @AfterEach
    fun tearDown() {
        reset(loginRepository)
    }

    @Test
    fun `test that the sign-up link is re-sent with the correct email and full name`() = runTest {
        val email = "test@test.com"
        val fullName = "fullName"
        whenever(loginRepository.resendSignupLink(email, fullName)).thenReturn(email)

        val actual = underTest(email, fullName)

        assertThat(actual).isEqualTo(email)
        verify(loginRepository).resendSignupLink(email, fullName)
    }
}
