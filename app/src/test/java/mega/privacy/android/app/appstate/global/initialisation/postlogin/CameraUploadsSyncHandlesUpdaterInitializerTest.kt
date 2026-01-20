package mega.privacy.android.app.appstate.global.initialisation.postlogin

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.user.UserChanges
import mega.privacy.android.domain.usecase.MonitorUserUpdates
import mega.privacy.android.domain.usecase.camerauploads.EstablishCameraUploadsSyncHandlesUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CameraUploadsSyncHandlesUpdaterInitializerTest {
    private lateinit var underTest: CameraUploadsSyncHandlesUpdaterInitializer

    private val establishCameraUploadsSyncHandlesUseCase =
        mock<EstablishCameraUploadsSyncHandlesUseCase>()
    private val monitorUserUpdatesFakeFlow = MutableSharedFlow<UserChanges>()

    @BeforeAll
    fun setup() {
        val monitorUserUpdates = mock<MonitorUserUpdates> {
            on { invoke() }.thenReturn(monitorUserUpdatesFakeFlow)
        }
        underTest = CameraUploadsSyncHandlesUpdaterInitializer(
            monitorUserUpdates,
            establishCameraUploadsSyncHandlesUseCase,
        )
    }

    @BeforeEach
    fun cleanUp() {
        reset(establishCameraUploadsSyncHandlesUseCase)
    }

    @Test
    fun `test that camera uploads sync handles are established when camera uploads folder changes`() =
        runTest {
            val job = launch {
                underTest.invoke("session", false)
            }
            testScheduler.advanceUntilIdle()
            monitorUserUpdatesFakeFlow.emit(UserChanges.CameraUploadsFolder)

            verify(establishCameraUploadsSyncHandlesUseCase).invoke()
            job.cancel()
        }

    @Test
    fun `test that camera uploads sync handles are not established when other user changes occur`() =
        runTest {
            val job = launch {
                underTest.invoke("session", false)
            }
            testScheduler.advanceUntilIdle()
            monitorUserUpdatesFakeFlow.emit(UserChanges.Email)

            verifyNoInteractions(establishCameraUploadsSyncHandlesUseCase)
            job.cancel()
        }

    @Test
    fun `test that camera uploads sync handles are not established when avatar changes`() =
        runTest {
            val job = launch {
                underTest.invoke("session", false)
            }
            testScheduler.advanceUntilIdle()
            monitorUserUpdatesFakeFlow.emit(UserChanges.Avatar)

            verifyNoInteractions(establishCameraUploadsSyncHandlesUseCase)
            job.cancel()
        }
}
