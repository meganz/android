package mega.privacy.android.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.yield
import mega.privacy.android.data.gateway.MediaRecorderGateway
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MediaRecorderRepositoryImplTest {
    private lateinit var underTest: MediaRecorderRepositoryImpl

    private val mediaRecorderGateway = mock<MediaRecorderGateway>()

    @BeforeAll
    fun setup() {
        Dispatchers.setMain(StandardTestDispatcher())
        underTest = MediaRecorderRepositoryImpl(
            UnconfinedTestDispatcher(),
            mediaRecorderGateway,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(mediaRecorderGateway)
        whenever(mediaRecorderGateway.getCurrentMaxAmplitude()) doReturn 1
    }

    @Test
    fun `test that mediaRecorderGateway startRecording is invoked when the collecting on the returned flow begins`() =
        runTest {
            val destination = mock<File>()
            underTest.recordAudio(destination).first()
            verify(mediaRecorderGateway).startRecording(destination)
        }

    @Test
    fun `test that mediaRecorderGateway stopRecording is invoked when the collecting on the returned flow is cancelled`() =
        runTest {
            val destination = mock<File>()
            val job = launch { underTest.recordAudio(destination).collect() }
            yield() //wait for collection to start
            job.cancel()
            verify(mediaRecorderGateway).stopRecording()
        }
}