package mega.privacy.android.domain.usecase

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.security.PasscodeRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub

@OptIn(ExperimentalCoroutinesApi::class)
class MonitorPasscodeLockPreferenceUseCaseTest {
    private lateinit var underTest: MonitorPasscodeLockPreferenceUseCase

    private val passcodeRepository = mock<PasscodeRepository>()

    @BeforeEach
    internal fun setUp() {
        underTest = MonitorPasscodeLockPreferenceUseCase(passcodeRepository = passcodeRepository)
    }

    @Test
    internal fun `test that values are returned from repository`() = runTest {
        val expected = true
        passcodeRepository.stub {
            on { monitorIsPasscodeEnabled() }.thenReturn(
                flow {
                    emit(expected)
                    awaitCancellation()
                }
            )

        }
        underTest().test {
            assertThat(awaitItem()).isEqualTo(expected)
        }
    }

    @Test
    internal fun `test that a default of false is returned if the value is null`() = runTest {
        val expected = false
        passcodeRepository.stub {
            on { monitorIsPasscodeEnabled() }.thenReturn(
                flow {
                    emit(null)
                    awaitCancellation()
                }
            )
        }

        underTest().test {
            assertThat(awaitItem()).isEqualTo(expected)
        }
    }
}