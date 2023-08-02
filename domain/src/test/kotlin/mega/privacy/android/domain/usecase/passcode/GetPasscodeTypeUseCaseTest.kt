package mega.privacy.android.domain.usecase.passcode

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.passcode.PasscodeType
import mega.privacy.android.domain.exception.security.NoPasscodeTypeSetException
import mega.privacy.android.domain.repository.security.PasscodeRepository
import mega.privacy.android.domain.testutils.hotFlow
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.kotlin.stub

@OptIn(ExperimentalCoroutinesApi::class)
class GetPasscodeTypeUseCaseTest {
    private lateinit var underTest: GetPasscodeTypeUseCase

    private val passcodeRepository = mock<PasscodeRepository?>()

    @BeforeEach
    internal fun setUp() {
        underTest = GetPasscodeTypeUseCase(passcodeRepository = passcodeRepository)
    }

    @Test
    internal fun `test that value is returned if found`() = runTest {
        val expected = PasscodeType.Password

        passcodeRepository.stub {
            on { monitorPasscodeType() }.thenReturn(hotFlow(expected))
        }

        assertThat(underTest()).isEqualTo(expected)
    }

    @Test
    internal fun `test that exception is thrown if type is null`() = runTest {
        passcodeRepository.stub {
            on { monitorPasscodeType() }.thenReturn(hotFlow(null))
        }

        assertThrows<NoPasscodeTypeSetException> { underTest() }
    }
}