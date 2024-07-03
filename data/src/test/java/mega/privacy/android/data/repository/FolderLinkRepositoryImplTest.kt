package mega.privacy.android.data.repository

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.data.gateway.MegaLocalStorageGateway
import mega.privacy.android.data.gateway.api.MegaApiFolderGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.listener.OptionalMegaRequestListenerInterface
import mega.privacy.android.data.mapper.FileTypeInfoMapper
import mega.privacy.android.data.mapper.FolderInfoMapper
import mega.privacy.android.data.mapper.FolderLoginStatusMapper
import mega.privacy.android.data.mapper.node.ImageNodeMapper
import mega.privacy.android.data.mapper.node.NodeMapper
import mega.privacy.android.data.mapper.search.MegaSearchFilterMapper
import mega.privacy.android.domain.entity.FolderInfo
import mega.privacy.android.domain.entity.RawFileTypeInfo
import mega.privacy.android.domain.entity.folderlink.FolderLoginStatus
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedImageNode
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.exception.SynchronisationException
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaCancelToken
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaFolderInfo
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaRequest
import nz.mega.sdk.MegaSearchFilter
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.contracts.ExperimentalContracts

@OptIn(ExperimentalCoroutinesApi::class)
@ExperimentalContracts
class FolderLinkRepositoryImplTest {

    private lateinit var underTest: FolderLinkRepositoryImpl
    private val testCoroutineDispatcher = StandardTestDispatcher()
    private val megaApiFolderGateway = mock<MegaApiFolderGateway>()
    private val megaApiGateway: MegaApiGateway = mock()
    private val folderLoginStatusMapper = mock<FolderLoginStatusMapper>()
    private val megaLocalStorageGateway = mock<MegaLocalStorageGateway>()
    private val untypedNode = mock<FolderNode>()
    private val nodeMapper: NodeMapper = mock()
    private val folderInfoMapper: FolderInfoMapper = mock()
    private val fileTypeInfoMapper: FileTypeInfoMapper = mock()
    private val imageNodeMapper: ImageNodeMapper = mock()
    private val megaSearchFilterMapper = mock<MegaSearchFilterMapper>()
    private val cancelTokenProvider = mock<CancelTokenProvider>()

    @Before
    fun setup() {
        Dispatchers.setMain(testCoroutineDispatcher)
        underTest =
            FolderLinkRepositoryImpl(
                megaApiFolderGateway = megaApiFolderGateway,
                megaApiGateway = megaApiGateway,
                folderLoginStatusMapper = folderLoginStatusMapper,
                megaLocalStorageGateway = megaLocalStorageGateway,
                nodeMapper = nodeMapper,
                folderInfoMapper = folderInfoMapper,
                fileTypeInfoMapper = fileTypeInfoMapper,
                imageNodeMapper = imageNodeMapper,
                megaSearchFilterMapper = megaSearchFilterMapper,
                cancelTokenProvider = cancelTokenProvider,
                ioDispatcher = UnconfinedTestDispatcher()
            )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test that status is returned correctly`() = runTest {
        val folderLink = "test"
        val megaError = mock<MegaError> { on { errorCode }.thenReturn(MegaError.API_OK) }
        val expectedResult = FolderLoginStatus.SUCCESS

        whenever(folderLoginStatusMapper(megaError)).thenReturn(expectedResult)

        whenever(megaApiFolderGateway.loginToFolder(any(), any())).thenAnswer {
            ((it.arguments[1]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                mock(),
                mock(),
                megaError
            )
        }

        assertThat(underTest.loginToFolder(folderLink)).isEqualTo(expectedResult)
    }

    @Test
    fun `test that on getting null megaNode SynchronisationException is thrown`() = runTest {
        val nodeId = NodeId(123)
        whenever(megaApiFolderGateway.getMegaNodeByHandle(nodeId.longValue)).thenReturn(null)

        Assert.assertThrows(SynchronisationException::class.java) {
            runBlocking { underTest.getParentNode(nodeId) }
        }
    }

    @Test
    fun `test that on getting null parentNode SynchronisationException is thrown`() = runTest {
        val nodeId = NodeId(123)
        val megaNode = mock<MegaNode>()
        whenever(megaApiFolderGateway.getMegaNodeByHandle(nodeId.longValue)).thenReturn(megaNode)
        whenever(megaApiFolderGateway.getParentNode(megaNode)).thenReturn(null)

        Assert.assertThrows(SynchronisationException::class.java) {
            runBlocking { underTest.getParentNode(nodeId) }
        }
    }

    @Test
    fun `test that valid untyped node is returned`() = runTest {
        val nodeId = NodeId(123)
        val megaNode = mock<MegaNode>()
        val parentNode = mock<MegaNode>()
        whenever(megaApiFolderGateway.getMegaNodeByHandle(nodeId.longValue)).thenReturn(megaNode)
        whenever(megaApiFolderGateway.getParentNode(megaNode)).thenReturn(parentNode)
        whenever(nodeMapper(any(), any(), any(), anyOrNull())).thenReturn(untypedNode)

        assertThat(underTest.getParentNode(nodeId)).isEqualTo(untypedNode)
    }

    @Test
    fun `test that on getting null folderLinkNode SynchronisationException is thrown`() = runTest {
        val base64Handle = "123"
        val handle = 1234L
        whenever(megaApiGateway.base64ToHandle(base64Handle)).thenReturn(handle)
        whenever(megaApiFolderGateway.getMegaNodeByHandle(handle)).thenReturn(null)

        Assert.assertThrows(SynchronisationException::class.java) {
            runBlocking { underTest.getFolderLinkNode(base64Handle) }
        }
    }

    @Test
    fun `test that valid folderLinkNode is returned`() = runTest {
        val base64Handle = "123"
        val handle = 1234L
        val megaNode = mock<MegaNode>()
        whenever(megaApiGateway.base64ToHandle(base64Handle)).thenReturn(handle)
        whenever(megaApiFolderGateway.getMegaNodeByHandle(handle)).thenReturn(megaNode)
        whenever(nodeMapper(any(), any(), any(), anyOrNull())).thenReturn(untypedNode)

        assertThat(underTest.getFolderLinkNode(base64Handle)).isEqualTo(untypedNode)
    }

    @Test
    fun `test that valid folder info of public link is returned`() = runTest {
        val expectedResult = FolderInfo(
            currentSize = 1000L,
            numVersions = 1,
            numFiles = 2,
            numFolders = 3,
            versionsSize = 4,
            folderName = "folder_name",
        )

        val mockMegaFolderInfo = mock<MegaFolderInfo> {
            on { numFolders }.thenReturn(expectedResult.numFolders)
            on { numVersions }.thenReturn(expectedResult.numVersions)
            on { currentSize }.thenReturn(expectedResult.currentSize)
            on { numFiles }.thenReturn(expectedResult.numFiles)
            on { versionsSize }.thenReturn(expectedResult.versionsSize)
        }

        val mockMegaError = mock<MegaError> {
            on { errorCode }.thenReturn(MegaError.API_OK)
        }

        val mockMegaRequest = mock<MegaRequest> {
            on { megaFolderInfo }.thenReturn(mockMegaFolderInfo)
            on { text }.thenReturn(expectedResult.folderName)
        }

        whenever(megaApiFolderGateway.getPublicLinkInformation(any(), any())).thenAnswer {
            ((it.arguments[1]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                mock(),
                mockMegaRequest,
                mockMegaError
            )
        }

        whenever(folderInfoMapper(any(), any())).thenReturn(expectedResult)

        val folderLink = "folder_link"
        assertThat(underTest.getPublicLinkInformation(folderLink)).isEqualTo(expectedResult)
    }

    @Test
    fun `test that exception is thrown when failure to get folder info of public link`() = runTest {
        val mockMegaError = mock<MegaError> {
            on { errorCode }.thenReturn(MegaError.API_ENOENT)
        }

        whenever(megaApiFolderGateway.getPublicLinkInformation(any(), any())).thenAnswer {
            ((it.arguments[1]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                mock(),
                mock(),
                mockMegaError
            )
        }

        assertThrows<MegaException> {
            val folderLink = "folder_link"
            underTest.getPublicLinkInformation(folderLink)
        }
    }

    @Test
    fun `test that correct node from gateway is returned when getChildNode is invoked`() = runTest {
        val megaNode = mock<MegaNode>()
        val id = 1L
        whenever(megaApiFolderGateway.getMegaNodeByHandle(id)).thenReturn(megaNode)
        whenever(megaApiFolderGateway.authorizeNode(megaNode)).thenReturn(megaNode)
        whenever(nodeMapper(any(), any(), any(), anyOrNull())).thenReturn(untypedNode)
        val actual = underTest.getChildNode(NodeId(id))
        assertThat(actual).isEqualTo(untypedNode)
    }

    @Test
    fun `test that node is authorized when getChildNode is invoked`() = runTest {
        val megaNode = mock<MegaNode>()
        val id = 1L
        whenever(megaApiFolderGateway.getMegaNodeByHandle(id)).thenReturn(megaNode)
        whenever(megaApiFolderGateway.authorizeNode(megaNode)).thenReturn(megaNode)
        whenever(nodeMapper(any(), any(), any(), anyOrNull())).thenReturn(untypedNode)
        underTest.getChildNode(NodeId(id))
        verify(megaApiFolderGateway).authorizeNode(megaNode)
    }

    @Test
    fun `test that node is mapped with correct parameters when getChildNode is invoked`() =
        runTest {
            val megaNode = mock<MegaNode>()
            val id = 1L
            whenever(megaApiFolderGateway.getMegaNodeByHandle(id)).thenReturn(megaNode)
            whenever(megaApiFolderGateway.authorizeNode(megaNode)).thenReturn(megaNode)
            whenever(nodeMapper(any(), any(), any(), anyOrNull())).thenReturn(untypedNode)
            underTest.getChildNode(NodeId(id))
            verify(nodeMapper).invoke(
                megaNode,
                fromFolderLink = true,
                requireSerializedData = false,
                offline = null
            )
        }

    @Test
    fun `test that untyped nodes from gateway are returned when getNodeChildren is invoked`() =
        runTest {
            val megaNode = mock<MegaNode> {
                on { handle }.thenReturn(1L)
            }
            val expectedOrder = MegaApiJava.ORDER_NONE
            val token = mock<MegaCancelToken>()
            val filter = mock<MegaSearchFilter>()
            whenever(cancelTokenProvider.getOrCreateCancelToken()).thenReturn(token)
            whenever(megaSearchFilterMapper(NodeId(megaNode.handle))).thenReturn(filter)
            val child = mock<MegaNode>()
            megaApiFolderGateway.stub {
                onBlocking { getChildren(filter, expectedOrder, token) }.thenReturn(listOf(child))
            }
            whenever(nodeMapper(child, fromFolderLink = true)).thenReturn(untypedNode)
            val id = 1L
            val order = 0
            whenever(megaApiFolderGateway.authorizeNode(megaNode)).thenReturn(megaNode)
            val actual = underTest.getNodeChildren(id, order)
            assertThat(actual).containsExactly(untypedNode)
        }

    @Test
    fun `test that image nodes are returned when getFolderLinkImageNodes is invoked`() =
        runTest {
            val megaNode = mock<MegaNode> {
                on { handle }.thenReturn(1L)
            }
            val expectedOrder = MegaApiJava.ORDER_NONE
            val token = mock<MegaCancelToken>()
            val filter = mock<MegaSearchFilter>()
            val imageNode = mock<TypedImageNode>()
            whenever(cancelTokenProvider.getOrCreateCancelToken()).thenReturn(token)
            whenever(megaSearchFilterMapper(NodeId(megaNode.handle))).thenReturn(filter)
            val child = mock<MegaNode>() {
                on { name }.thenReturn("name")
                on { duration }.thenReturn(0)
            }
            megaApiFolderGateway.stub {
                onBlocking { getChildren(filter, expectedOrder, token) }.thenReturn(listOf(child))
            }
            whenever(
                fileTypeInfoMapper(
                    child.name,
                    child.duration
                )
            ).thenReturn(mock<RawFileTypeInfo>())
            whenever(nodeMapper(child, fromFolderLink = true)).thenReturn(untypedNode)
            whenever(imageNodeMapper(any(), any(), any(), anyOrNull())).thenReturn(imageNode)
            val id = 1L
            val order = 0
            whenever(megaApiFolderGateway.authorizeNode(megaNode)).thenReturn(megaNode)
            val actual = underTest.getFolderLinkImageNodes(id, order)
            assertThat(actual).containsExactly(imageNode)
        }
}