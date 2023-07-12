package mega.privacy.android.data.repository

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.listener.OptionalMegaRequestListenerInterface
import mega.privacy.android.data.mapper.node.NodeMapper
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.exception.PublicNodeException
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaRequest
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class FileLinkRepositoryImplTest {
    private lateinit var underTest: FileLinkRepositoryImpl
    private val testCoroutineDispatcher = StandardTestDispatcher()
    private val megaApiGateway: MegaApiGateway = mock()
    private val nodeMapper: NodeMapper = mock()

    @Before
    fun setup() {
        Dispatchers.setMain(testCoroutineDispatcher)
        underTest =
            FileLinkRepositoryImpl(
                megaApiGateway = megaApiGateway,
                nodeMapper = nodeMapper,
                ioDispatcher = UnconfinedTestDispatcher()
            )
    }

    @Test
    fun `test that on getting API_OK valid file node is returned`() = runTest {
        val url = "https://mega.co.nz/abc"
        val publicNode = mock<MegaNode>()
        val untypedPublicNode = mock<FileNode>()
        val megaRequest = mock<MegaRequest> {
            on { publicMegaNode }.thenReturn(publicNode)
        }
        val megaError = mock<MegaError> { on { errorCode }.thenReturn(MegaError.API_OK) }

        whenever(nodeMapper(publicNode)).thenReturn(untypedPublicNode)
        whenever(megaApiGateway.getPublicNode(any(), any())).thenAnswer {
            ((it.arguments[1]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                mock(),
                megaRequest,
                megaError
            )
        }

        assertThat(underTest.getPublicNode(url)).isEqualTo(untypedPublicNode)
    }

    @Test
    fun `test that on getting API_EBLOCKED error code LinkRemoved exception is thrown`() = runTest {
        val url = "https://mega.co.nz/abc"
        val megaError = mock<MegaError> { on { errorCode }.thenReturn(MegaError.API_EBLOCKED) }

        whenever(megaApiGateway.getPublicNode(any(), any())).thenAnswer {
            ((it.arguments[1]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                mock(),
                mock(),
                megaError
            )
        }

        assertThrows<PublicNodeException.LinkRemoved> { underTest.getPublicNode(url) }
    }

    @Test
    fun `test that on getting API_ETOOMANY error code AccountTerminated exception is thrown`() =
        runTest {
            val url = "https://mega.co.nz/abc"
            val megaError = mock<MegaError> { on { errorCode }.thenReturn(MegaError.API_ETOOMANY) }

            whenever(megaApiGateway.getPublicNode(any(), any())).thenAnswer {
                ((it.arguments[1]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                    mock(),
                    mock(),
                    megaError
                )
            }

            assertThrows<PublicNodeException.AccountTerminated> { underTest.getPublicNode(url) }
        }

    @Test
    fun `test that on getting API_EINCOMPLETE error code DecryptionKeyRequired exception is thrown`() =
        runTest {
            val url = "https://mega.co.nz/abc"
            val megaError =
                mock<MegaError> { on { errorCode }.thenReturn(MegaError.API_EINCOMPLETE) }

            whenever(megaApiGateway.getPublicNode(any(), any())).thenAnswer {
                ((it.arguments[1]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                    mock(),
                    mock(),
                    megaError
                )
            }

            assertThrows<PublicNodeException.DecryptionKeyRequired> { underTest.getPublicNode(url) }
        }
}