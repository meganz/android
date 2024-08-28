package mega.privacy.android.app.presentation.settings.exportrecoverykey

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.StateEventWithContentConsumed
import de.palm.composestateevents.StateEventWithContentTriggered
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.presentation.settings.exportrecoverykey.ExportRecoveryKeyViewModel
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.usecase.GetExportMasterKeyUseCase
import mega.privacy.android.domain.usecase.SetMasterKeyExportedUseCase
import mega.privacy.android.domain.usecase.account.GetPrintRecoveryKeyFileUseCase
import org.junit.Assert.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import mega.privacy.android.app.InstantExecutorExtension
import java.io.File

@ExtendWith(InstantExecutorExtension::class)
@OptIn(ExperimentalCoroutinesApi::class)
class ExportRecoveryKeyViewModelTest {

    lateinit var underTest: ExportRecoveryKeyViewModel
    private val getExportMasterKeyUseCase = mock<GetExportMasterKeyUseCase>()
    private val setMasterKeyExportedUseCase = mock<SetMasterKeyExportedUseCase>()
    private val getPrintRecoveryKeyFileUseCase = mock<GetPrintRecoveryKeyFileUseCase>()
    private val fakeRecoveryKey = "JALSJLKNDnsnda12738"

    private fun constructViewModel() = ExportRecoveryKeyViewModel(
        getExportMasterKeyUseCase,
        setMasterKeyExportedUseCase,
        getPrintRecoveryKeyFileUseCase,
        dispatcher
    )

    @BeforeEach
    fun setUp() {
        underTest = constructViewModel()
    }

    @Test
    fun `test that setMasterKeyExported should NOT trigger when recovery key is empty, and vice versa`() {
        fun verify(key: String?, expectedInvocation: Int) = runTest {
            whenever(getExportMasterKeyUseCase()).thenReturn(key)

            underTest.getRecoveryKey()

            advanceUntilIdle()

            verify(setMasterKeyExportedUseCase, times(expectedInvocation)).invoke()
        }

        verify(key = null, expectedInvocation = 0)
        verify(key = fakeRecoveryKey, expectedInvocation = 1)
    }

    @Test
    fun `test that action group should be vertical when setActionGroupVertical is triggered`() =
        runTest {
            underTest.setActionGroupVertical()

            advanceUntilIdle()

            underTest.uiState.test {
                assertThat(awaitItem().isActionGroupVertical).isTrue()
            }
        }

    @Test
    fun `test that message should be updated when showSnackBar`() =
        runTest {
            val fakeMessage = "asdjaskdjasalskdj"

            underTest.showSnackBar(fakeMessage)

            advanceUntilIdle()

            underTest.uiState.test {
                assertThat(awaitItem().message).isEqualTo(fakeMessage)
            }
        }

    @Test
    fun `test that message should be resetted to null when snackbar is shown`() =
        runTest {
            underTest.setSnackBarShown()

            advanceUntilIdle()

            underTest.uiState.test {
                assertThat(awaitItem().message).isEqualTo(null)
            }
        }

    @Test
    fun `test that printRecoveryKey event is triggered when printRecoveryKey is invoked`() =
        runTest {
            val file = mock<File>()
            whenever(getPrintRecoveryKeyFileUseCase()).thenReturn(file)
            underTest.printRecoveryKey()
            underTest.uiState.test {
                val result = awaitItem()
                assertThat(result.printRecoveryKey).isInstanceOf(StateEventWithContentTriggered::class.java)
            }
        }

    @Test
    fun `test that printRecoveryKey event is consumed when resetPrintRecoveryKey is invoked`() =
        runTest {
            underTest.resetPrintRecoveryKey()
            underTest.uiState.test {
                val result = awaitItem()
                assertThat(result.printRecoveryKey).isInstanceOf(StateEventWithContentConsumed::class.java)
            }
        }

    companion object {
        private val dispatcher = UnconfinedTestDispatcher()

        @JvmField
        @RegisterExtension
        val extension = CoroutineMainDispatcherExtension(dispatcher)
    }
}