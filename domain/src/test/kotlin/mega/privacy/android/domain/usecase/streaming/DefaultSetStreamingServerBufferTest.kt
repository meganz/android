package mega.privacy.android.domain.usecase.streaming

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.EnvironmentRepository
import mega.privacy.android.domain.repository.StreamingServerRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever


class DefaultSetStreamingServerBufferTest {
    private lateinit var underTest: SetStreamingServerBuffer

    private val environmentRepository = mock<EnvironmentRepository>()
    private val streamingServerRepository = mock<StreamingServerRepository>()

    @Before
    fun setUp() {
        underTest = DefaultSetStreamingServerBuffer(environmentRepository = environmentRepository,
            streamingServerRepository = streamingServerRepository)
    }

    @Test
    fun `test that if no memory data is returned, the buffer is set to 16 megabyte`() = runTest {
        whenever(environmentRepository.getDeviceMemorySizeInBytes()).thenReturn(null)
        underTest()
        verify(streamingServerRepository).setMaxBufferSize(DefaultSetStreamingServerBuffer.BUFFER_SIZE_16MB)
    }

    @Test
    fun `test that if device memory is over the threshold, the buffer size is set to 32 megabyte`() =
        runTest {
            whenever(environmentRepository.getDeviceMemorySizeInBytes()).thenReturn(
                DefaultSetStreamingServerBuffer.LARGE_BUFFER_THRESHOLD + 1)
            underTest()
            verify(streamingServerRepository).setMaxBufferSize(DefaultSetStreamingServerBuffer.BUFFER_SIZE_32MB)
        }

    @Test
    fun `test that if device memory is under threshold, the buffer is set to 16 megabyte`() =
        runTest {
            whenever(environmentRepository.getDeviceMemorySizeInBytes()).thenReturn(
                DefaultSetStreamingServerBuffer.LARGE_BUFFER_THRESHOLD - 1)
            underTest()
            verify(streamingServerRepository).setMaxBufferSize(DefaultSetStreamingServerBuffer.BUFFER_SIZE_16MB)
        }

}