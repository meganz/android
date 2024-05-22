package mega.privacy.android.domain.usecase.login

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.security.LoginRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SetLogoutInProgressFlagUseCaseTest {

    private lateinit var underTest: SetLogoutInProgressFlagUseCase

    private val loginRepository: LoginRepository = mock()

    @BeforeEach
    fun setUp() {
        underTest = SetLogoutInProgressFlagUseCase(
            loginRepository = loginRepository
        )
    }

    @AfterEach
    fun tearDown() {
        reset(loginRepository)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that the logout flag is set correctly`(isLoggingOut: Boolean) = runTest {
        underTest(isLoggingOut = isLoggingOut)

        verify(loginRepository).setLogoutInProgressFlag(isLoggingOut)
    }
}
