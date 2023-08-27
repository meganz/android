package mega.privacy.android.app.usecase.chat

import kotlinx.coroutines.runBlocking
import mega.privacy.android.app.utils.wrapper.CameraEnumeratorWrapper
import mega.privacy.android.domain.repository.CallRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.webrtc.CameraEnumerator

class SetChatVideoInDeviceUseCaseTest {

    private lateinit var underTest: SetChatVideoInDeviceUseCase
    private val callRepository: CallRepository = mock()
    private val cameraEnumerator: CameraEnumerator = mock()
    private val cameraEnumeratorWrapper: CameraEnumeratorWrapper = mock()

    @Before
    fun setUp() {
        underTest = SetChatVideoInDeviceUseCase(callRepository, cameraEnumeratorWrapper)
        whenever(cameraEnumeratorWrapper()).thenReturn(cameraEnumerator)
    }

    @Test
    fun `test that method sets the front camera when isFrontCamera is true`(): Unit = runBlocking {
        whenever(cameraEnumerator.deviceNames).thenReturn(arrayOf("front"))
        whenever(cameraEnumerator.isFrontFacing("front")).thenReturn(true)
        underTest.invoke(true)
        verify(callRepository).setChatVideoInDevice("front")
    }

    @Test
    fun `test that method sets the back camera when isFrontCamera is false`(): Unit = runBlocking {
        whenever(cameraEnumerator.deviceNames).thenReturn(arrayOf("back"))
        whenever(cameraEnumerator.isBackFacing("back")).thenReturn(true)
        underTest.invoke(false)
        verify(callRepository).setChatVideoInDevice("back")
    }

    @Test(expected = RuntimeException::class)
    fun `test that method throws an exception when no camera is found`() = runBlocking {
        whenever(cameraEnumerator.deviceNames).thenReturn(emptyArray())
        underTest.invoke(true)
    }
}
