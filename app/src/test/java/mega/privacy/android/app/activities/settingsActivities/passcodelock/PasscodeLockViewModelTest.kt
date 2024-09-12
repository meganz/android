package mega.privacy.android.app.activities.settingsActivities.passcodelock

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.usecase.offline.HasOfflineFilesUseCase
import mega.privacy.android.domain.usecase.transfers.OngoingTransfersExistUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever


@ExtendWith(CoroutineMainDispatcherExtension::class)
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PasscodeLockViewModelTest {
    private lateinit var underTest: PasscodeLockViewModel

    private val hasOfflineFilesUseCase = mock<HasOfflineFilesUseCase>()
    private val ongoingTransfersExistUseCase = mock<OngoingTransfersExistUseCase>()


    @BeforeAll
    fun setUp() {
        underTest = PasscodeLockViewModel(hasOfflineFilesUseCase, ongoingTransfersExistUseCase)
    }

    @Test
    internal fun `test that logoutEvent is set correctly when checkLogoutConfirmation is called`() =
        runTest {
            whenever(hasOfflineFilesUseCase()).thenReturn(true)
            whenever(ongoingTransfersExistUseCase()).thenReturn(true)

            underTest.checkLogoutConfirmation()

            underTest.uiState.test {
                assertThat(awaitItem().logoutEvent).isEqualTo(true to true)
            }
        }

    @Test
    internal fun `test logoutEvent is cleared when onLogoutEventConsumed is called`() =
        runTest {
            underTest.onLogoutEventConsumed()

            underTest.uiState.test {
                assertThat(awaitItem().logoutEvent).isNull()
            }
        }

    @BeforeEach
    fun resetMocks() {
        reset(hasOfflineFilesUseCase, ongoingTransfersExistUseCase)
    }
}
