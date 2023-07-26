package mega.privacy.android.data.repository

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.data.gateway.CacheGateway
import mega.privacy.android.data.gateway.MegaLocalStorageGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.listener.OptionalMegaRequestListenerInterface
import mega.privacy.android.data.mapper.node.NodeMapper
import mega.privacy.android.data.model.node.DefaultFileNode
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.exception.PublicNodeException
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaRequest
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class FileLinkRepositoryImplTest {
    private lateinit var underTest: FileLinkRepositoryImpl
    private val testCoroutineDispatcher = StandardTestDispatcher()
    private val megaApiGateway: MegaApiGateway = mock()
    private val cacheGateway: CacheGateway = mock()
    private val megaLocalStorageGateway: MegaLocalStorageGateway = mock()
    private val nodeMapper: NodeMapper = mock()

    @Before
    fun setup() {
        Dispatchers.setMain(testCoroutineDispatcher)
        underTest =
            FileLinkRepositoryImpl(
                megaApiGateway = megaApiGateway,
                nodeMapper = nodeMapper,
                cacheGateway = cacheGateway,
                megaLocalStorageGateway = megaLocalStorageGateway,
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
    fun `test that on getting valid public node handle it is saved in database`() = runTest {
        val url = "https://mega.co.nz/abc"
        val nodeHandle = 1234L
        val publicNode = mock<MegaNode> {
            on { handle }.thenReturn(nodeHandle)
        }
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

        underTest.getPublicNode(url)
        verify(megaLocalStorageGateway).setLastPublicHandle(nodeHandle)
        verify(megaLocalStorageGateway).setLastPublicHandleTimeStamp()
    }

    @Test
    fun `test that on getting valid public node correct preview path is returned`() = runTest {
        val url = "https://mega.co.nz/abc"
        val expectedPath = "data/cache/xyz.jpg"
        val cacheFile = mock<File> {
            on { exists() }.thenReturn(true)
            on { absolutePath }.thenReturn(expectedPath)
        }
        val publicNode = mock<MegaNode> {
            on { hasPreview() }.thenReturn(true)
        }
        val untypedPublicNode = mock<DefaultFileNode>()
        val expectedNode = mock<DefaultFileNode> {
            on { previewPath }.thenReturn(expectedPath)
        }
        val megaRequest = mock<MegaRequest> {
            on { publicMegaNode }.thenReturn(publicNode)
        }
        val megaError = mock<MegaError> { on { errorCode }.thenReturn(MegaError.API_OK) }

        whenever(nodeMapper(publicNode)).thenReturn(untypedPublicNode)
        whenever(untypedPublicNode.copy(previewPath = expectedPath)).thenReturn(expectedNode)
        whenever(megaApiGateway.getPublicNode(any(), any())).thenAnswer {
            ((it.arguments[1]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                mock(),
                megaRequest,
                megaError
            )
        }
        whenever(cacheGateway.getCacheFile(any(), any())).thenReturn(cacheFile)

        val node = underTest.getPublicNode(url)
        assertThat(node).isInstanceOf(DefaultFileNode::class.java)
        assertThat((node as DefaultFileNode).previewPath).isEqualTo(expectedPath)
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

    @Test
    fun `test that encryptLinkWithPassword returns correctly when call encryptLinkWithPassword successfully`() =
        runTest {
            val url = "https://mega.co.nz/abc"
            val encryptedLink = "https://mega.co.nz/abc/encrypted"
            val megaError = mock<MegaError> { on { errorCode }.thenReturn(MegaError.API_OK) }
            val request = mock<MegaRequest> { on { text }.thenReturn(encryptedLink) }

            whenever(megaApiGateway.encryptLinkWithPassword(any(), any(), any())).thenAnswer {
                ((it.arguments[2]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                    mock(),
                    request,
                    megaError
                )
            }

            assertThat(underTest.encryptLinkWithPassword(url, "password"))
                .isEqualTo(encryptedLink)
        }

    @Test
    fun `test that encryptLinkWithPassword throw exception when call encryptLinkWithPassword returns failed`() =
        runTest {
            val megaError =
                mock<MegaError> { on { errorCode }.thenReturn(MegaError.API_EARGS) }

            whenever(megaApiGateway.encryptLinkWithPassword(any(), any(), any())).thenAnswer {
                ((it.arguments[2]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                    mock(),
                    mock(),
                    megaError
                )
            }

            assertThrows<MegaException> {
                underTest.encryptLinkWithPassword(
                    "https://mega.co.nz/abc",
                    "password"
                )
            }
        }
}