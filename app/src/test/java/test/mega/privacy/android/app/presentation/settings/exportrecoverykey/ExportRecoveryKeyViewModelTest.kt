package test.mega.privacy.android.app.presentation.settings.exportrecoverykey

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.presentation.settings.exportrecoverykey.ExportRecoveryKeyViewModel
import mega.privacy.android.domain.usecase.GetExportMasterKey
import mega.privacy.android.domain.usecase.SetMasterKeyExported
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class ExportRecoveryKeyViewModelTest {
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    lateinit var underTest: ExportRecoveryKeyViewModel
    private val getExportMasterKey = mock<GetExportMasterKey>()
    private val setMasterKeyExported = mock<SetMasterKeyExported>()
    private val fakeRecoveryKey = "JALSJLKNDnsnda12738"

    private fun constructViewModel() = ExportRecoveryKeyViewModel(
        getExportMasterKey,
        setMasterKeyExported
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
        underTest = constructViewModel()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test that setMasterKeyExported should NOT trigger when recovery key is empty, and vice versa`() {
        fun verify(key: String?, expectedInvocation: Int) = runTest {
            whenever(getExportMasterKey()).thenReturn(key)

            underTest.getRecoveryKey()

            advanceUntilIdle()

            verify(setMasterKeyExported, times(expectedInvocation)).invoke()
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
                assertThat(awaitItem().snackBarMessage).isEqualTo(fakeMessage)
            }
        }

    @Test
    fun `test that message should be resetted to null when snackbar is shown`() =
        runTest {
            underTest.setSnackBarShown()

            advanceUntilIdle()

            underTest.uiState.test {
                assertThat(awaitItem().snackBarMessage).isEqualTo(null)
            }
        }
}