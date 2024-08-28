package mega.privacy.android.domain.usecase.passcode

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.passcode.PasscodeTimeout
import mega.privacy.android.domain.repository.security.PasscodeRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class SetPasscodeTimeoutUseCaseTest {
    private lateinit var underTest: SetPasscodeTimeoutUseCase

    private val passcodeRepository = mock<PasscodeRepository>()

    @BeforeEach
    internal fun setUp() {
        underTest = SetPasscodeTimeoutUseCase(passcodeRepository = passcodeRepository)
    }

    @Test
    fun `test that repository is called`() = runTest {
        val expectedTimeout = PasscodeTimeout.Immediate
        underTest(expectedTimeout)

        verify(passcodeRepository).setPasscodeTimeOut(expectedTimeout)
    }
}