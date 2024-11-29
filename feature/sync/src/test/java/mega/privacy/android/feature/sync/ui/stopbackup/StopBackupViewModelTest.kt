package mega.privacy.android.feature.sync.ui.stopbackup

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.feature.sync.domain.entity.RemoteFolder
import mega.privacy.android.feature.sync.domain.usecase.sync.option.MonitorSelectedMegaFolderUseCase
import mega.privacy.android.feature.sync.ui.stopbackup.model.StopBackupState
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.reset
import org.mockito.kotlin.whenever

/**
 * Test Class for [StopBackupViewModel]
 */
@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExperimentalCoroutinesApi
internal class StopBackupViewModelTest {

    private lateinit var underTest: StopBackupViewModel

    private val monitorSelectedMegaFolderUseCase: MonitorSelectedMegaFolderUseCase = mock()

    @AfterEach
    fun resetMocks() {
        reset(
            monitorSelectedMegaFolderUseCase,
        )
    }

    @Test
    fun `test that the initial state is returned`() = runTest {
        whenever(monitorSelectedMegaFolderUseCase()).thenReturn(flow {
            emit(value = null)
            awaitCancellation()
        })
        initViewModel()
        underTest.state.test {
            val initialState = awaitItem()
            assertThat(initialState.selectedMegaFolder).isNull()
        }
    }

    @Test
    fun `test that the selected MEGA folder is reset`() = runTest {
        whenever(monitorSelectedMegaFolderUseCase()).thenReturn(flow {
            emit(value = null)
            awaitCancellation()
        })
        initViewModel()
        underTest.resetSelectedMegaFolder()
        underTest.state.test {
            val state = awaitItem()
            assertThat(state.selectedMegaFolder).isNull()
        }
    }

    @Test
    fun `test that when mega folder is selected state is also updated`() = runTest {
        val remoteFolder = RemoteFolder(123L, "Selected Folder")
        whenever(monitorSelectedMegaFolderUseCase()).thenReturn(flow {
            emit(remoteFolder)
            awaitCancellation()
        })
        initViewModel()
        val expectedState = StopBackupState(
            selectedMegaFolder = remoteFolder,
        )

        assertThat(expectedState).isEqualTo(underTest.state.value)
    }

    private fun initViewModel() {
        underTest = StopBackupViewModel(
            monitorSelectedMegaFolderUseCase = monitorSelectedMegaFolderUseCase,
        )
    }
}