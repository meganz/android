package mega.privacy.android.data.repository

import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.gateway.api.StreamingGateway
import mega.privacy.android.domain.repository.StreamingServerRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class StreamingServerRepositoryImplTest {
    private lateinit var underTest: StreamingServerRepository

    private val streamingGateway = mock<StreamingGateway>()

    @Before
    fun setUp() {
        underTest = StreamingServerRepositoryImpl(
            ioDispatcher = UnconfinedTestDispatcher(),
            streamingGateway = streamingGateway,
        )
    }

    @Test
    fun `test that streaming server is started if port is 0`() = runTest{
        whenever(streamingGateway.getPort()).thenReturn(0)
        underTest.startServer()

        verify(streamingGateway).startServer()
    }

    @Test
    fun `test that server is not started if port is not 0`() = runTest{
        whenever(streamingGateway.getPort()).thenReturn(1)
        underTest.startServer()

        verify(streamingGateway, never()).startServer()
    }

}