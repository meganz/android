package mega.privacy.android.app.appstate.global.initialisation.postlogin

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.MyAccountUpdate
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.usecase.account.MonitorMyAccountUpdateUseCase
import mega.privacy.android.domain.usecase.workers.StartCameraUploadUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StartCameraUploadsAfterStorageStateEventInitializerTest {

    private lateinit var underTest: StartCameraUploadsAfterStorageStateEventInitializer
    private val startCameraUploads: StartCameraUploadUseCase = mock()
    private val monitorMyAccountUpdateFakeFlow = MutableSharedFlow<MyAccountUpdate>()

    @BeforeAll
    fun setUp() {
        val monitorMyAccountUpdateUseCase: MonitorMyAccountUpdateUseCase = mock {
            whenever(it()).thenReturn(monitorMyAccountUpdateFakeFlow)
        }
        underTest = StartCameraUploadsAfterStorageStateEventInitializer(
            monitorMyAccountUpdateUseCase = monitorMyAccountUpdateUseCase,
            startCameraUploads = startCameraUploads
        )
    }

    @BeforeEach
    fun cleanUp() {
        reset(startCameraUploads)
    }

    @ParameterizedTest
    @EnumSource(StorageState::class, names = ["Green", "Orange"])
    fun `test that startCameraUploads is called when storage state is the desired`(
        storageState: StorageState,
    ) = runTest {
        val job = launch {
            underTest("test-session", false)
        }
        advanceUntilIdle()

        monitorMyAccountUpdateFakeFlow.emit(
            MyAccountUpdate(
                action = MyAccountUpdate.Action.STORAGE_STATE_CHANGED,
                storageState = storageState
            )
        )
        advanceUntilIdle()

        verify(startCameraUploads).invoke()
        job.cancel()
    }

    @ParameterizedTest
    @EnumSource(StorageState::class, names = ["Unknown", "Red", "Change", "PayWall"])
    fun `test that startCameraUploads is not called when storage state is not the desired`(
        storageState: StorageState,
    ) = runTest {
        val job = launch {
            underTest("test-session", false)
        }
        advanceUntilIdle()

        monitorMyAccountUpdateFakeFlow.emit(
            MyAccountUpdate(
                action = MyAccountUpdate.Action.STORAGE_STATE_CHANGED,
                storageState = storageState
            )
        )
        advanceUntilIdle()

        verifyNoInteractions(startCameraUploads)
        job.cancel()
    }

    @Test
    fun `test that exception is handled gracefully when startCameraUploads throws`() = runTest {
        whenever(startCameraUploads()).thenThrow(RuntimeException("Test error"))

        val job = launch {
            underTest("test-session", false)
        }
        advanceUntilIdle()

        monitorMyAccountUpdateFakeFlow.emit(
            MyAccountUpdate(
                action = MyAccountUpdate.Action.STORAGE_STATE_CHANGED,
                storageState = StorageState.Green
            )
        )
        advanceUntilIdle()

        // Should not throw exception, but startCameraUploads should be called
        verify(startCameraUploads).invoke()
        job.cancel()
    }
}
