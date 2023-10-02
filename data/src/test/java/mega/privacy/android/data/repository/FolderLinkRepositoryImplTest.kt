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
import mega.privacy.android.data.mapper.FolderInfoMapper
import mega.privacy.android.data.mapper.FolderLoginStatusMapper
import mega.privacy.android.data.mapper.node.NodeMapper
import mega.privacy.android.domain.entity.FolderInfo
import mega.privacy.android.domain.entity.folderlink.FolderLoginStatus
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.exception.SynchronisationException
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaFolderInfo
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaRequest
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
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
        whenever(nodeMapper(any(), any(), any())).thenReturn(untypedNode)

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
        whenever(nodeMapper(any(), any(), any())).thenReturn(untypedNode)

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
}