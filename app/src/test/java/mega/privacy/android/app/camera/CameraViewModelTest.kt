package mega.privacy.android.app.camera

import android.app.Application
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.camera.state.CameraState
import mega.privacy.android.domain.usecase.file.CreateNewImageUriUseCase
import mega.privacy.android.domain.usecase.file.CreateNewVideoUriUseCase
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class CameraViewModelTest {
    private lateinit var underTest: CameraViewModel
    private val createNewImageUriUseCase: CreateNewImageUriUseCase = mock()
    private val createNewVideoUriUseCase: CreateNewVideoUriUseCase = mock()
    private val application: Application = mock()
    private val testDispatcher: CoroutineDispatcher = UnconfinedTestDispatcher()

    @BeforeAll
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        initTestClass()
    }

    @AfterAll
    fun tearDown() {
        Dispatchers.resetMain()
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
        underTest.captureVideo(cameraState)

        verify(createNewVideoUriUseCase).invoke(any())
    }

    @Test
    fun `test that stopping recording invokes stopRecording`() = runTest {
        val cameraState = mock<CameraState> {
            on { isRecording }.thenReturn(true)
        }
        underTest.captureVideo(cameraState)

        verify(cameraState).stopRecording()
    }

    @Test
    fun `test that starting recording invokes createNewVideoUriUseCase`() = runTest {
        val cameraState = mock<CameraState> {
            on { isRecording }.thenReturn(false)
        }
        underTest.captureVideo(cameraState)

        verify(createNewVideoUriUseCase).invoke(any())
    }

    private fun initTestClass() {
        underTest = CameraViewModel(
            createNewImageUriUseCase,
            createNewVideoUriUseCase,
            application,
            testDispatcher
        )
    }
}