package test.mega.privacy.android.app.exportRK

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
import mega.privacy.android.app.exportRK.ExportRecoveryKeyViewModel
import mega.privacy.android.app.exportRK.model.RecoveryKeyUIState
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
    fun `test that ui state is emitting the correct state and recovery key when copy`() = runTest {
        whenever(getExportMasterKey()).thenReturn(fakeRecoveryKey)

        underTest.onCopyRecoveryKey()

        underTest.uiState.test {
            val state = awaitItem()

            assertThat(state)
                .isInstanceOf(RecoveryKeyUIState.CopyRecoveryKey::class.java)
            assertThat((state as RecoveryKeyUIState.CopyRecoveryKey).key).isEqualTo(fakeRecoveryKey)
        }
    }

    @Test
    fun `test that ui state is emitting the correct state and recovery key when export`() =
        runTest {
            whenever(getExportMasterKey()).thenReturn(fakeRecoveryKey)

            underTest.onExportRecoveryKey()

            underTest.uiState.test {
                val state = awaitItem()

                assertThat(state)
                    .isInstanceOf(RecoveryKeyUIState.ExportRecoveryKey::class.java)
                assertThat((state as RecoveryKeyUIState.ExportRecoveryKey).key).isEqualTo(
                    fakeRecoveryKey
                )
            }
        }

    @Test
    fun `test that ui state is emitting the correct state when print`() = runTest {
        whenever(getExportMasterKey()).thenReturn(fakeRecoveryKey)

        underTest.onPrintRecoveryKey()

        underTest.uiState.test {
            val state = awaitItem()

            assertThat(state)
                .isInstanceOf(RecoveryKeyUIState.PrintRecoveryKey::class.java)
        }
    }

    @Test
    fun `test that export master key should not trigger when recovery key is empty, and vice versa`() {
        fun verify(key: String?, expectedInvocation: Int) = runTest {
            whenever(getExportMasterKey()).thenReturn(key)

            underTest.onCopyRecoveryKey()

            advanceUntilIdle()

            verify(setMasterKeyExported, times(expectedInvocation)).invoke()
        }

        verify(key = null, expectedInvocation = 0)
        verify(key = fakeRecoveryKey, expectedInvocation = 1)
    }
}