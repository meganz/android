package mega.privacy.android.app.camera

import android.app.Application
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.camera.state.CameraState
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.usecase.file.CreateNewImageUriUseCase
import mega.privacy.android.domain.usecase.file.CreateNewVideoUriUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class CameraViewModelTest {
    private lateinit var underTest: CameraViewModel
    private val createNewImageUriUseCase: CreateNewImageUriUseCase = mock()
    private val createNewVideoUriUseCase: CreateNewVideoUriUseCase = mock()
    private val application: Application = mock()

    @BeforeAll
    fun setup() {
        initTestClass()
    }

    @BeforeEach
    fun resetMocks() {
        reset(createNewImageUriUseCase, createNewVideoUriUseCase, application)
    }

    @Test
    fun `test that taking picture invokes createNewImageUri`() = runTest {
        underTest.takePicture(mock())

        verify(createNewImageUriUseCase).invoke(any())
    }

    @Test
    fun `test that capturing video invokes createNewVideoUri`() = runTest {
        val cameraState = mock<CameraState> {
            on { isRecording }.thenReturn(false)
        }
        underTest.captureVideo(cameraState, false)

        verify(createNewVideoUriUseCase).invoke(any())
    }

    @Test
    fun `test that stopping recording invokes stopRecording`() = runTest {
        val cameraState = mock<CameraState> {
            on { isRecording }.thenReturn(true)
        }
        underTest.captureVideo(cameraState, false)

        verify(cameraState).stopRecording()
    }

    @Test
    fun `test that starting recording invokes createNewVideoUriUseCase`() = runTest {
        val cameraState = mock<CameraState> {
            on { isRecording }.thenReturn(false)
        }
        underTest.captureVideo(cameraState, false)

        verify(createNewVideoUriUseCase).invoke(any())
    }

    private fun initTestClass() {
        underTest = CameraViewModel(
            createNewImageUriUseCase,
            createNewVideoUriUseCase,
            application,
            extension.testDispatcher
        )
    }

    companion object {
        @JvmField
        @RegisterExtension
        val extension = CoroutineMainDispatcherExtension()
    }
}