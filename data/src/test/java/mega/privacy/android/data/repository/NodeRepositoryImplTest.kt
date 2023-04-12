package mega.privacy.android.data.repository

import android.content.Context
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.gateway.CacheFolderGateway
import mega.privacy.android.data.gateway.FileGateway
import mega.privacy.android.data.gateway.MegaLocalStorageGateway
import mega.privacy.android.data.gateway.api.MegaApiFolderGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.gateway.api.MegaChatApiGateway
import mega.privacy.android.data.gateway.api.StreamingGateway
import mega.privacy.android.data.listener.OptionalMegaRequestListenerInterface
import mega.privacy.android.data.mapper.ChatFilesFolderUserAttributeMapper
import mega.privacy.android.data.mapper.FileTypeInfoMapper
import mega.privacy.android.data.mapper.MegaExceptionMapper
import mega.privacy.android.data.mapper.MegaShareMapper
import mega.privacy.android.data.mapper.NodeMapper
import mega.privacy.android.data.mapper.NodeUpdateMapper
import mega.privacy.android.data.mapper.OfflineNodeInformationMapper
import mega.privacy.android.data.mapper.SortOrderIntMapper
import mega.privacy.android.data.mapper.node.NodeShareKeyResultMapper
import mega.privacy.android.data.mapper.shares.AccessPermissionMapper
import mega.privacy.android.domain.entity.FolderTreeInfo
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.repository.NodeRepository
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaFolderInfo
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaRequest
import nz.mega.sdk.MegaRequestListenerInterface
import nz.mega.sdk.MegaShare.ACCESS_READ
import nz.mega.sdk.MegaUser
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class NodeRepositoryImplTest {

    private lateinit var underTest: NodeRepository
    private val context: Context = mock()
    private val megaApiGateway: MegaApiGateway = mock()
    private val megaApiFolderGateway: MegaApiFolderGateway = mock()
    private val megaChatApiGateway: MegaChatApiGateway = mock()
    private val megaLocalStorageGateway: MegaLocalStorageGateway = mock()
    private val megaShareMapper: MegaShareMapper = mock()
    private val megaExceptionMapper: MegaExceptionMapper = mock()
    private val sortOrderIntMapper: SortOrderIntMapper = mock()
    private val cacheFolderGateway: CacheFolderGateway = mock()
    private val nodeMapper: NodeMapper = NodeMapper()
    private val fileTypeInfoMapper: FileTypeInfoMapper = mock()
    private val offlineNodeInformationMapper: OfflineNodeInformationMapper = mock()
    private val fileGateway: FileGateway = mock()
    private val chatFilesFolderUserAttributeMapper: ChatFilesFolderUserAttributeMapper = mock()
    private val streamingGateway: StreamingGateway = mock()
    private val nodeUpdateMapper: NodeUpdateMapper = mock()
    private val folderNode: TypedFolderNode = mock()
    private val accessPermissionMapper: AccessPermissionMapper = mock()
    private val nodeShareKeyResultMapper = mock<NodeShareKeyResultMapper>()

    @Before
    fun setup() {
        underTest = NodeRepositoryImpl(
            context = context,
            megaApiGateway = megaApiGateway,
            megaApiFolderGateway = megaApiFolderGateway,
            megaChatApiGateway = megaChatApiGateway,
            ioDispatcher = UnconfinedTestDispatcher(),
            megaLocalStorageGateway = megaLocalStorageGateway,
            megaShareMapper = megaShareMapper,
            megaExceptionMapper = megaExceptionMapper,
            sortOrderIntMapper = sortOrderIntMapper,
            cacheFolderGateway = cacheFolderGateway,
            nodeMapper = nodeMapper,
            fileTypeInfoMapper = fileTypeInfoMapper,
            offlineNodeInformationMapper = offlineNodeInformationMapper,
            fileGateway = fileGateway,
            chatFilesFolderUserAttributeMapper = chatFilesFolderUserAttributeMapper,
            streamingGateway = streamingGateway,
            nodeUpdateMapper = nodeUpdateMapper,
            accessPermissionMapper = accessPermissionMapper,
            nodeShareKeyResultMapper = nodeShareKeyResultMapper
        )
    }

    @Test
    fun `test that base64ToHandle returns properly`() =
        runTest {
            val base64 = "a base 64 value"
            val expectedHandle = 1234L
            whenever(megaApiGateway.base64ToHandle(base64)).thenReturn(expectedHandle)
            assertThat(underTest.convertBase64ToHandle(base64)).isEqualTo(expectedHandle)
        }

    @Test
    fun `test getFolderVersionInfo queries megaApiGateway`() =
        runTest {
            mockFolderInfoResponse()
            underTest.getFolderTreeInfo(folderNode)
            verify(megaApiGateway).getFolderInfo(any(), any())
        }

    @Test
    fun `test getFolderVersionInfo is returning correct info from megaApiGateway`() =
        runTest {
            mockFolderInfoResponse()
            val result = underTest.getFolderTreeInfo(folderNode)
            assertThat(result).isEqualTo(folderInfo)
        }

    @Test
    fun `test access is fetched from mega api gateway`() = runTest {
        val node = mock<MegaNode>()
        whenever(megaApiGateway.getMegaNodeByHandle(nodeId.longValue)).thenReturn(node)
        whenever(megaApiGateway.getAccess(node)).thenReturn(ACCESS_READ)
        whenever(accessPermissionMapper.invoke(ACCESS_READ)).thenReturn(AccessPermission.READ)
        underTest.getNodeAccessPermission(nodeId)
        verify(megaApiGateway, times(1)).getAccess(node)
    }

    @Test
    fun `test when stopSharingNode is called then api gateway stopSharingNode is called with the proper node`() =
        runTest {
            val megaNode = mock<MegaNode>()
            whenever(megaApiGateway.getMegaNodeByHandle(nodeId.longValue)).thenReturn(megaNode)
            underTest.stopSharingNode(nodeId)
            verify(megaApiGateway, times(1)).stopSharingNode(megaNode)
        }

    @Test
    fun `test when setShareAccess is called then nodeShareKeyResultMapper is called with the meganode returned by megaApiGateway`() =
        runTest {
            val megaNode = mock<MegaNode>()
            val email = "example@example.com"
            val mapperResultBlock = mock<((AccessPermission, String) -> Unit)>()
            whenever(nodeShareKeyResultMapper.invoke(megaNode)).thenReturn(mapperResultBlock)
            whenever(megaApiGateway.getMegaNodeByHandle(nodeId.longValue)).thenReturn(megaNode)

            underTest.setShareAccess(nodeId, AccessPermission.READ, email)
            verify(mapperResultBlock, times(1)).invoke(AccessPermission.READ, email)
        }

    @Test
    fun `test when createShareKey is called then api gateway openShareDialog is called and the result of the mapper returned`() =
        runTest {
            val megaNode = mock<MegaNode>()
            val expected = mock<(suspend (AccessPermission, String) -> Unit)>()
            whenever(folderNode.id).thenReturn(nodeId)
            whenever(nodeShareKeyResultMapper.invoke(megaNode)).thenReturn(expected)
            whenever(megaApiGateway.getMegaNodeByHandle(nodeId.longValue)).thenReturn(megaNode)
            whenever(megaApiGateway.openShareDialog(any(), any())).thenAnswer {
                ((it.arguments[1]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                    api = mock(),
                    request = mock(),
                    error = mock {
                        on { errorCode }.thenReturn(
                            MegaError.API_OK
                        )
                    },
                )
            }

            val actual = underTest.createShareKey(folderNode)
            verify(megaApiGateway, times(1)).openShareDialog(eq(megaNode), any())
            assertThat(actual).isEqualTo(expected)
        }

    @Test
    fun `test when getInShare called with valid email node list is returned`() = runTest {
        val testEmail = "test@mega.nz"
        val user = mock<MegaUser> {
            on { email }.thenReturn(testEmail)
        }
        val megaNode = mockMegaNodeForConversion()
        val nodeList = listOf(megaNode)
        whenever(megaApiGateway.getContact(testEmail)).thenReturn(user)
        whenever(megaApiGateway.getInShares(user)).thenReturn(nodeList)
        val actual = underTest.getInShares(testEmail)
        assertThat(actual.size).isEqualTo(1)
        assertThat(actual[0].base64Id).isEqualTo("base64Handle")
    }

    private suspend fun mockFolderInfoResponse() {
        val fileNode: MegaNode = mock()
        whenever(folderNode.id).thenReturn(nodeId)
        whenever(fileNode.isFolder).thenReturn(true)
        whenever(megaApiGateway.getMegaNodeByHandle(nodeHandle)).thenReturn(fileNode)
        val response = mock<MegaRequest>()
        val megaFolderInfo = mock<MegaFolderInfo>()
        whenever(response.megaFolderInfo).thenReturn(megaFolderInfo)
        whenever(megaFolderInfo.numVersions).thenReturn(folderInfo.numberOfVersions)
        whenever(megaFolderInfo.versionsSize).thenReturn(folderInfo.sizeOfPreviousVersionsInBytes)
        whenever(megaFolderInfo.currentSize).thenReturn(folderInfo.totalCurrentSizeInBytes)
        whenever(megaFolderInfo.numFolders).thenReturn(folderInfo.numberOfFolders)
        whenever(megaFolderInfo.numFiles).thenReturn(folderInfo.numberOfFiles)
        whenever(megaApiGateway.getFolderInfo(any(), any())).thenAnswer {
            (it.arguments[1] as MegaRequestListenerInterface).onRequestFinish(
                mock(), response, mock()
            )
        }
    }

    private suspend fun mockMegaNodeForConversion(): MegaNode {
        val megaNode = mock<MegaNode> {
            on { handle }.thenReturn(987L)
            on { name }.thenReturn("name")
            on { size }.thenReturn(8)
            on { label }.thenReturn(10)
            on { parentHandle }.thenReturn(123456)
            on { base64Handle }.thenReturn("base64Handle")
            on { creationTime }.thenReturn(123456789)
            on { modificationTime }.thenReturn(1234567890)
            on { isFavourite }.thenReturn(true)
            on { isExported }.thenReturn(true)
            on { publicLink }.thenReturn("public_link")
            on { publicLinkCreationTime }.thenReturn(1234567)
            on { isTakenDown }.thenReturn(true)
            on { isInShare }.thenReturn(true)
            on { fingerprint }.thenReturn("finger_print")
            on { isNodeKeyDecrypted }.thenReturn(true)
            on { hasPreview() }.thenReturn(true)
        }
        whenever(cacheFolderGateway.getThumbnailCacheFolder()).thenReturn(File("thumbnail_path"))
        whenever(megaApiGateway.hasVersion(megaNode)).thenReturn(true)
        whenever(megaApiGateway.getNumChildFolders(megaNode)).thenReturn(2)
        whenever(megaApiGateway.getNumChildFiles(megaNode)).thenReturn(3)
        whenever(megaApiGateway.isPendingShare(megaNode)).thenReturn(true)
        whenever(megaApiGateway.isInRubbish(megaNode)).thenReturn(true)
        return megaNode
    }

    companion object {
        private const val nodeHandle = 1L
        private val nodeId = NodeId(nodeHandle)
        private val folderInfo = FolderTreeInfo(
            numberOfVersions = 2,
            sizeOfPreviousVersionsInBytes = 1000L,
            numberOfFiles = 4,
            numberOfFolders = 2,
            totalCurrentSizeInBytes = 2000L,
        )
    }
}
