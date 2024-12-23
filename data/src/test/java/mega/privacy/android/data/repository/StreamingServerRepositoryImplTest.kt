package mega.privacy.android.data.repository

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.gateway.api.StreamingGateway
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.StreamingServerRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class StreamingServerRepositoryImplTest {
    private lateinit var underTest: StreamingServerRepository

    private val streamingGateway = mock<StreamingGateway>()
    private val megaApiGateway = mock<MegaApiGateway>()

    @Before
    fun setUp() {
        underTest = StreamingServerRepositoryImpl(
            ioDispatcher = UnconfinedTestDispatcher(),
            streamingGateway = streamingGateway,
            megaApiGateway = megaApiGateway
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

    @Test
    fun `test that local file url string is returned if node exists`() = runTest {
        whenever(megaApiGateway.getMegaNodeByHandle(any())).thenReturn(mock())
        val expected = "expectedUrl"
        whenever(streamingGateway.getLocalLink(any())).thenReturn(expected)

        val actual = underTest.getFileStreamingUri(mock { on { id }.thenReturn(NodeId(1L)) })

        assertThat(actual).isEqualTo(expected)
    }

}