package mega.privacy.android.data.repository

import android.content.Context
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.gateway.CacheGateway
import mega.privacy.android.data.gateway.FileGateway
import mega.privacy.android.data.gateway.MegaLocalRoomGateway
import mega.privacy.android.data.gateway.MegaLocalStorageGateway
import mega.privacy.android.data.gateway.api.MegaApiFolderGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.gateway.api.MegaChatApiGateway
import mega.privacy.android.data.gateway.api.StreamingGateway
import mega.privacy.android.data.listener.OptionalMegaRequestListenerInterface
import mega.privacy.android.data.mapper.ChatFilesFolderUserAttributeMapper
import mega.privacy.android.data.mapper.FileTypeInfoMapper
import mega.privacy.android.data.mapper.MegaExceptionMapper
import mega.privacy.android.data.mapper.NodeUpdateMapper
import mega.privacy.android.data.mapper.OfflineInformationMapper
import mega.privacy.android.data.mapper.OfflineNodeInformationMapper
import mega.privacy.android.data.mapper.SortOrderIntMapper
import mega.privacy.android.data.mapper.node.FetchChildrenMapper
import mega.privacy.android.data.mapper.node.FileNodeMapper
import mega.privacy.android.data.mapper.node.FolderNodeMapper
import mega.privacy.android.data.mapper.node.MegaNodeMapper
import mega.privacy.android.data.mapper.node.NodeMapper
import mega.privacy.android.data.mapper.node.NodeShareKeyResultMapper
import mega.privacy.android.data.mapper.node.OfflineAvailabilityMapper
import mega.privacy.android.data.mapper.node.label.NodeLabelIntMapper
import mega.privacy.android.data.mapper.shares.AccessPermissionIntMapper
import mega.privacy.android.data.mapper.shares.AccessPermissionMapper
import mega.privacy.android.data.mapper.shares.ShareDataMapper
import mega.privacy.android.domain.entity.FolderTreeInfo
import mega.privacy.android.domain.entity.Offline
import mega.privacy.android.domain.entity.PdfFileTypeInfo
import mega.privacy.android.domain.entity.ShareData
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.publiclink.PublicLinkFolder
import mega.privacy.android.domain.entity.offline.OfflineFolderInfo
import mega.privacy.android.domain.entity.offline.OtherOfflineNodeInformation
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.exception.node.ForeignNodeException
import mega.privacy.android.domain.repository.NodeRepository
import nz.mega.sdk.MegaChatMessage
import nz.mega.sdk.MegaChatRoom
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaFolderInfo
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaNodeList
import nz.mega.sdk.MegaRequest
import nz.mega.sdk.MegaRequestListenerInterface
import nz.mega.sdk.MegaShare
import nz.mega.sdk.MegaShare.ACCESS_READ
import nz.mega.sdk.MegaUser
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import java.io.File
import java.util.stream.Stream

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NodeRepositoryImplTest {

    private lateinit var underTest: NodeRepository
    private val context: Context = mock()
    private val megaApiGateway: MegaApiGateway = mock()
    private val megaApiFolderGateway: MegaApiFolderGateway = mock()
    private val megaChatApiGateway: MegaChatApiGateway = mock()
    private val megaLocalStorageGateway: MegaLocalStorageGateway = mock()
    private val shareDataMapper: ShareDataMapper = mock()
    private val megaExceptionMapper: MegaExceptionMapper = mock()
    private val sortOrderIntMapper: SortOrderIntMapper = mock()
    private val cacheGateway: CacheGateway = mock()
    private val fileTypeInfoMapper: FileTypeInfoMapper = mock()
    private val offlineNodeInformationMapper: OfflineNodeInformationMapper = mock()
    private val offlineInformationMapper: OfflineInformationMapper = mock()
    private val fileGateway: FileGateway = mock()
    private val chatFilesFolderUserAttributeMapper: ChatFilesFolderUserAttributeMapper = mock()
    private val streamingGateway: StreamingGateway = mock()
    private val nodeUpdateMapper: NodeUpdateMapper = mock()
    private val folderNode: TypedFolderNode = mock()
    private val publicLinkFolder: PublicLinkFolder = mock()
    private val accessPermissionMapper: AccessPermissionMapper = mock()
    private val accessPermissionIntMapper: AccessPermissionIntMapper = AccessPermissionIntMapper()
    private val nodeShareKeyResultMapper = mock<NodeShareKeyResultMapper>()
    private val fetChildrenMapper = mock<FetchChildrenMapper>()
    private val megaLocalRoomGateway: MegaLocalRoomGateway = mock()
    private val offlineAvailabilityMapper: OfflineAvailabilityMapper = mock()
    private val megaNodeMapper = mock<MegaNodeMapper>()
    private val fileNodeMapper = FileNodeMapper(
        cacheGateway = cacheGateway,
        megaApiGateway = megaApiGateway,
        fileTypeInfoMapper = fileTypeInfoMapper,
        offlineAvailabilityMapper = offlineAvailabilityMapper,
    )

    private val nodeMapper: NodeMapper = NodeMapper(
        fileNodeMapper = fileNodeMapper,
        folderNodeMapper = FolderNodeMapper(
            megaApiGateway = megaApiGateway,
            megaApiFolderGateway = megaApiFolderGateway,
            fetChildrenMapper = fetChildrenMapper,
        )
    )

    val offline: Offline = mock()

    private val nodeLabelIntMapper = NodeLabelIntMapper()

    @BeforeAll
    fun setup() {
        underTest = NodeRepositoryImpl(
            context = context,
            megaApiGateway = megaApiGateway,
            megaApiFolderGateway = megaApiFolderGateway,
            megaChatApiGateway = megaChatApiGateway,
            ioDispatcher = UnconfinedTestDispatcher(),
            megaLocalStorageGateway = megaLocalStorageGateway,
            shareDataMapper = shareDataMapper,
            megaExceptionMapper = megaExceptionMapper,
            sortOrderIntMapper = sortOrderIntMapper,
            nodeMapper = nodeMapper,
            fileNodeMapper = fileNodeMapper,
            fileTypeInfoMapper = fileTypeInfoMapper,
            offlineNodeInformationMapper = offlineNodeInformationMapper,
            offlineInformationMapper = offlineInformationMapper,
            fileGateway = fileGateway,
            chatFilesFolderUserAttributeMapper = chatFilesFolderUserAttributeMapper,
            streamingGateway = streamingGateway,
            nodeUpdateMapper = nodeUpdateMapper,
            accessPermissionMapper = accessPermissionMapper,
            nodeShareKeyResultMapper = nodeShareKeyResultMapper,
            accessPermissionIntMapper = accessPermissionIntMapper,
            megaLocalRoomGateway = megaLocalRoomGateway,
            megaNodeMapper = megaNodeMapper,
            nodeLabelIntMapper = nodeLabelIntMapper
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            megaApiGateway,
            megaApiFolderGateway,
            megaChatApiGateway,
            megaLocalStorageGateway,
            shareDataMapper,
            megaExceptionMapper,
            sortOrderIntMapper,
            fileTypeInfoMapper,
            offlineNodeInformationMapper,
            offlineInformationMapper,
            fileGateway,
            chatFilesFolderUserAttributeMapper,
            streamingGateway,
            nodeUpdateMapper,
            accessPermissionMapper,
            nodeShareKeyResultMapper,
            accessPermissionMapper,
            megaLocalStorageGateway,
            megaNodeMapper,
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
            verifyNoInteractions(megaApiFolderGateway)
        }

    @Test
    fun `test getFolderVersionInfo is returning correct info from megaApiGateway`() =
        runTest {
            mockFolderInfoResponse()
            val result = underTest.getFolderTreeInfo(folderNode)
            assertThat(result).isEqualTo(folderInfo)
        }

    @Test
    fun `test getFolderVersionInfo queries megaApiFolderGateway when the node is a PublicLinkFolder`() =
        runTest {
            mockPublicLinkFolderInfoResponse()
            underTest.getFolderTreeInfo(publicLinkFolder)
            verify(megaApiFolderGateway).getFolderInfo(any(), any())
            verifyNoInteractions(megaApiGateway)
        }

    @Test
    fun `test getFolderVersionInfo is returning correct info from megaApiFolderGateway when the node is a PublicLinkFolder`() =
        runTest {
            mockPublicLinkFolderInfoResponse()
            val result = underTest.getFolderTreeInfo(publicLinkFolder)
            assertThat(result).isEqualTo(folderInfo)
        }

    @Test
    fun `test access is fetched from mega api gateway`() = runTest {
        val node = mock<MegaNode>()
        whenever(megaApiGateway.getMegaNodeByHandle(nodeId.longValue)).thenReturn(node)
        whenever(megaApiGateway.getAccess(node)).thenReturn(ACCESS_READ)
        whenever(accessPermissionMapper.invoke(ACCESS_READ)).thenReturn(AccessPermission.READ)
        underTest.getNodeAccessPermission(nodeId)
        verify(megaApiGateway).getAccess(node)
    }

    @Test
    fun `test when stopSharingNode is called then api gateway stopSharingNode is called with the proper node`() =
        runTest {
            val megaNode = mock<MegaNode>()
            whenever(megaApiGateway.getMegaNodeByHandle(nodeId.longValue)).thenReturn(megaNode)
            underTest.stopSharingNode(nodeId)
            verify(megaApiGateway).stopSharingNode(megaNode)
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
            verify(mapperResultBlock).invoke(AccessPermission.READ, email)
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
            verify(megaApiGateway).openShareDialog(eq(megaNode), any())
            assertThat(actual).isEqualTo(expected)
        }

    @Test
    fun `test when getInShare called with valid email node list is returned`() = runTest {
        val testEmail = "test@mega.nz"
        val user = mock<MegaUser> {
            on { email }.thenReturn(testEmail)
        }
        val megaNode = mockMegaNodeForConversion()
        whenever(offlineAvailabilityMapper(megaNode, offline)).thenReturn(true)
        val nodeList = listOf(megaNode)
        whenever(megaApiGateway.getContact(testEmail)).thenReturn(user)
        whenever(megaApiGateway.getInShares(user)).thenReturn(nodeList)
        val actual = underTest.getInShares(testEmail)
        assertThat(actual.size).isEqualTo(1)
        assertThat(actual[0].base64Id).isEqualTo("base64Handle")
    }

    @Test
    fun `test that when getMegaNodeByHandle returns null then getNodeOutgoingShares returns an empty list`() =
        runTest {
            whenever(megaApiGateway.getMegaNodeByHandle(nodeHandle)).thenReturn(null)
            val actual = underTest.getNodeOutgoingShares(nodeId)
            assertThat(actual).isEmpty()
        }

    @Test
    fun `test that when getMegaNodeByHandle returns a node and getOutShares returns an empty list then getNodeOutgoingShares returns an empty list`() =
        runTest {
            val node: MegaNode = mock()
            whenever(megaApiGateway.getMegaNodeByHandle(nodeHandle)).thenReturn(node)
            whenever(megaApiGateway.getOutShares(node)).thenReturn(emptyList())
            val actual = underTest.getNodeOutgoingShares(nodeId)
            assertThat(actual).isEmpty()
        }

    @Test
    fun `test that when getMegaNodeByHandle returns a node and getOutShares returns a list of shares then getNodeOutgoingShares returns the mapped list`() =
        runTest {
            val node: MegaNode = mock()
            val megaShare: MegaShare = mock()
            val share: ShareData = mock()
            whenever(megaApiGateway.getMegaNodeByHandle(nodeHandle)).thenReturn(node)
            whenever(megaApiGateway.getOutShares(node)).thenReturn(listOf(megaShare))
            whenever(shareDataMapper(megaShare)).thenReturn(share)
            val actual = underTest.getNodeOutgoingShares(nodeId)
            assertThat(actual).containsExactly(share)
        }

    @Test
    fun `test that getNodePathById returns an empty node path if getMegaNodeByHandle returns null`() =
        runTest {
            whenever(megaApiGateway.getMegaNodeByHandle(any())).thenReturn(null)
            val nodePath = underTest.getNodePathById(NodeId(123456))
            assertThat(nodePath).isEmpty()
            verify(megaApiGateway, times(0)).getNodePath(any())
        }

    @ParameterizedTest(name = "when getNodePath returns {0}, then getNodePathById returns {1}")
    @MethodSource("mockGetNodePathById")
    fun `test that getNodePathById returns the correct node path`(
        nodePath: String?,
        expectedNodePath: String,
    ) = runTest {
        val testNode = mock<MegaNode>()
        megaApiGateway.stub {
            onBlocking { getMegaNodeByHandle(any()) }.thenReturn(testNode)
            onBlocking { getNodePath(testNode) }.thenReturn(nodePath)
        }
        val actualNodePath = underTest.getNodePathById(NodeId(123456))
        assertThat(actualNodePath).isEqualTo(expectedNodePath)
    }

    private fun mockGetNodePathById() = Stream.of(
        Arguments.of(null, ""),
        Arguments.of("", ""),
        Arguments.of("test/path", "test/path")
    )

    @Test
    fun `test that getUnverifiedIncomingShares calls api gateway getUnverifiedIncomingShares with mapped sort order`() =
        runTest {
            val sortOrder = SortOrder.ORDER_NONE
            whenever(sortOrderIntMapper(any())).thenReturn(0)
            whenever(megaApiGateway.getUnverifiedIncomingShares(any())).thenReturn(listOf(mock()))

            underTest.getUnverifiedIncomingShares(sortOrder)

            verify(megaApiGateway).getUnverifiedIncomingShares(sortOrderIntMapper(sortOrder))
        }

    @Test
    fun `test that getUnverifiedIncomingShares returns mapped result from api gateway`() =
        runTest {
            whenever(sortOrderIntMapper(any())).thenReturn(0)
            val megaShare1 = mock<MegaShare>()
            val megaShare2 = mock<MegaShare>()
            val megaShares = listOf(megaShare1, megaShare2)
            whenever(megaApiGateway.getUnverifiedIncomingShares(any())).thenReturn(megaShares)
            val share1 = mock<ShareData>()
            val share2 = mock<ShareData>()
            whenever(shareDataMapper(megaShare1)).thenReturn(share1)
            whenever(shareDataMapper(megaShare2)).thenReturn(share2)

            val expected = listOf(share1, share2)
            val actual = underTest.getUnverifiedIncomingShares(any())

            assertThat(actual).isEqualTo(expected)
        }

    @Test
    fun `test that getUnverifiedOutgoingShares calls api gateway getOutgoingSharesNode`() =
        runTest {
            val sortOrder = SortOrder.ORDER_NONE
            whenever(sortOrderIntMapper(any())).thenReturn(0)
            whenever(megaApiGateway.getOutgoingSharesNode(any())).thenReturn(listOf(mock()))

            underTest.getUnverifiedOutgoingShares(sortOrder)

            verify(megaApiGateway).getOutgoingSharesNode(sortOrderIntMapper(sortOrder))
        }

    @Test
    fun `test that getUnverifiedOutgoingShares returns mapped result from api gateway with filtered result`() =
        runTest {
            whenever(sortOrderIntMapper(any())).thenReturn(0)
            val megaShare1 = mock<MegaShare> {
                on { isVerified }.thenReturn(true)
            }
            val megaShare2 = mock<MegaShare> {
                on { isVerified }.thenReturn(false)
            }
            val megaShares = listOf(megaShare1, megaShare2)
            whenever(megaApiGateway.getOutgoingSharesNode(any())).thenReturn(megaShares)
            val share1 = mock<ShareData>()
            val share2 = mock<ShareData>()
            whenever(shareDataMapper(megaShare1)).thenReturn(share1)
            whenever(shareDataMapper(megaShare2)).thenReturn(share2)

            val expected = listOf(share2)
            val actual = underTest.getUnverifiedOutgoingShares(any())

            assertThat(actual).isEqualTo(expected)
        }

    @Test
    fun `test that getVerifiedIncomingShares calls api gateway getVerifiedIncomingShares with mapped sort order`() =
        runTest {
            val sortOrder = SortOrder.ORDER_NONE
            whenever(sortOrderIntMapper(any())).thenReturn(0)
            whenever(megaApiGateway.getVerifiedIncomingShares(any())).thenReturn(listOf(mock()))

            underTest.getVerifiedIncomingShares(sortOrder)

            verify(megaApiGateway).getVerifiedIncomingShares(sortOrderIntMapper(sortOrder))
        }

    @Test
    fun `test that getVerifiedIncomingShares returns mapped result from api gateway`() =
        runTest {
            whenever(sortOrderIntMapper(any())).thenReturn(0)
            val megaShare1 = mock<MegaShare>()
            val megaShare2 = mock<MegaShare>()
            val megaShares = listOf(megaShare1, megaShare2)
            whenever(megaApiGateway.getVerifiedIncomingShares(any())).thenReturn(megaShares)
            val share1 = mock<ShareData>()
            val share2 = mock<ShareData>()
            whenever(shareDataMapper(megaShare1)).thenReturn(share1)
            whenever(shareDataMapper(megaShare2)).thenReturn(share2)

            val expected = listOf(share1, share2)
            val actual = underTest.getVerifiedIncomingShares(any())

            assertThat(actual).isEqualTo(expected)
        }

    @Test
    fun `test that saveOfflineNodeInformation calls api gateway saveOfflineNodeInformation with the mapped data and correct parent id`() =
        runTest {
            val parentOfflineInfoId = 2L
            val offlineNodeInformation = mock<OtherOfflineNodeInformation>()
            val mapped = mock<Offline>()

            whenever(
                offlineInformationMapper(
                    offlineNodeInformation,
                    parentOfflineInfoId.toInt()
                )
            ).thenReturn(mapped)

            underTest.saveOfflineNodeInformation(offlineNodeInformation, parentOfflineInfoId)

            verify(megaLocalRoomGateway).saveOfflineInformation(mapped)
        }


    @Test
    fun `test that throw IllegalArgumentException when call moveNode and can not find node by handle`() =
        runTest {
            val node = NodeId(1L)
            val destinationNode = NodeId(2L)
            whenever(megaApiGateway.getMegaNodeByHandle(any())).thenReturn(null)
            try {
                underTest.moveNode(node, destinationNode, null)
            } catch (e: Exception) {
                assertThat(e).isInstanceOf(IllegalArgumentException::class.java)
            }
        }

    @Test
    fun `test that move node success when call to SDK successfully`() = runTest {
        val node = NodeId(1L)
        val destinationNode = NodeId(2L)
        val megaNode = mock<MegaNode>()
        val destinationMegaNode = mock<MegaNode>()
        whenever(megaApiGateway.getMegaNodeByHandle(node.longValue)).thenReturn(megaNode)
        whenever(megaApiGateway.getMegaNodeByHandle(destinationNode.longValue)).thenReturn(
            destinationMegaNode
        )
        whenever(megaApiGateway.moveNode(any(), any(), anyOrNull(), any())).thenAnswer {
            ((it.arguments[3]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                api = mock(),
                request = mock {
                    on { nodeHandle }.thenReturn(node.longValue)
                },
                error = mock {
                    on { errorCode }.thenReturn(
                        MegaError.API_OK
                    )
                },
            )
        }

        assertThat(underTest.moveNode(node, destinationNode, null)).isEqualTo(node)
    }

    @Test
    fun `test that move node throw ForeignNodeException when call to SDK returns API_EOVERQUOTA`() =
        runTest {
            val node = NodeId(1L)
            val destinationNode = NodeId(2L)
            val megaNode = mock<MegaNode>()
            val destinationMegaNode = mock<MegaNode> {
                on { handle }.thenReturn(destinationNode.longValue)
            }
            whenever(megaApiGateway.getMegaNodeByHandle(node.longValue)).thenReturn(megaNode)
            whenever(megaApiGateway.getMegaNodeByHandle(destinationNode.longValue)).thenReturn(
                destinationMegaNode
            )
            whenever(megaApiGateway.moveNode(any(), any(), anyOrNull(), any())).thenAnswer {
                ((it.arguments[3]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                    api = mock(),
                    request = mock {
                        on { nodeHandle }.thenReturn(node.longValue)
                    },
                    error = mock {
                        on { errorCode }.thenReturn(
                            MegaError.API_EOVERQUOTA
                        )
                    },
                )
            }
            whenever(megaApiGateway.isForeignNode(destinationNode.longValue)).thenReturn(true)

            try {
                underTest.moveNode(node, destinationNode, null)
            } catch (e: Exception) {
                assertThat(e).isInstanceOf(ForeignNodeException::class.java)
            }
        }

    @Test
    fun `test that getNodesByHandle invokes correct function`() = runTest {
        val handle = 1L
        val megaNode = mockMegaNodeForConversion()
        whenever(offlineAvailabilityMapper(megaNode, offline)).thenReturn(false)
        whenever(megaApiGateway.getMegaNodeByHandle(handle)).thenReturn(megaNode)
        assertThat(underTest.getNodeByHandle(handle)?.base64Id).isEqualTo("base64Handle")
    }

    private suspend fun mockFolderInfoResponse() {
        val fileNode: MegaNode = mock()
        whenever(folderNode.id).thenReturn(nodeId)
        whenever(fileNode.isFolder).thenReturn(true)
        whenever(megaNodeMapper(any())).thenReturn(fileNode)
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

    private suspend fun mockPublicLinkFolderInfoResponse() {
        val fileNode: MegaNode = mock()
        whenever(publicLinkFolder.id).thenReturn(nodeId)
        whenever(fileNode.isFolder).thenReturn(true)
        whenever(megaNodeMapper(any())).thenReturn(fileNode)
        val response = mock<MegaRequest>()
        val megaFolderInfo = mock<MegaFolderInfo>()
        whenever(response.megaFolderInfo).thenReturn(megaFolderInfo)
        whenever(megaFolderInfo.numVersions).thenReturn(folderInfo.numberOfVersions)
        whenever(megaFolderInfo.versionsSize).thenReturn(folderInfo.sizeOfPreviousVersionsInBytes)
        whenever(megaFolderInfo.currentSize).thenReturn(folderInfo.totalCurrentSizeInBytes)
        whenever(megaFolderInfo.numFolders).thenReturn(folderInfo.numberOfFolders)
        whenever(megaFolderInfo.numFiles).thenReturn(folderInfo.numberOfFiles)
        whenever(megaApiFolderGateway.getFolderInfo(any(), any())).thenAnswer {
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
        whenever(cacheGateway.getThumbnailCacheFolder()).thenReturn(File("thumbnail_path"))
        whenever(megaApiGateway.hasVersion(megaNode)).thenReturn(true)
        whenever(megaApiGateway.getNumVersions(megaNode)).thenReturn(2)
        whenever(megaApiGateway.getNumChildFolders(megaNode)).thenReturn(2)
        whenever(megaApiGateway.getNumChildFiles(megaNode)).thenReturn(3)
        whenever(megaApiGateway.isPendingShare(megaNode)).thenReturn(true)
        whenever(megaApiGateway.isInRubbish(megaNode)).thenReturn(true)
        whenever(fileTypeInfoMapper.invoke(megaNode)).thenReturn(PdfFileTypeInfo)
        return megaNode
    }

    @Test
    fun `test that exportNode throws IllegalArgumentException when node is not found`() =
        runTest {
            val node = NodeId(1L)
            whenever(megaApiGateway.getMegaNodeByHandle(any())).thenReturn(null)
            assertThrows<IllegalArgumentException> {
                underTest.exportNode(node, null)
            }
        }

    @Test
    fun `test that exportNode throws IllegalArgumentException when node is taken down`() =
        runTest {
            val node = NodeId(1L)
            val megaNode = mock<MegaNode> {
                on { isTakenDown }.thenReturn(true)
            }
            whenever(megaApiGateway.getMegaNodeByHandle(any())).thenReturn(megaNode)
            assertThrows<IllegalArgumentException> {
                underTest.exportNode(node, null)
            }
        }


    @Test
    fun `test that exportNode returns publicLink as result when node is exported but not expired and expireTime matches`() =
        runTest {
            val node = NodeId(1L)
            val expireTime = 2L
            val expected = "public_link"
            val megaNode = mock<MegaNode> {
                on { isTakenDown }.thenReturn(false)
                on { isExported }.thenReturn(true)
                on { isExpired }.thenReturn(false)
                on { expirationTime }.thenReturn(expireTime)
                on { publicLink }.thenReturn(expected)
            }
            whenever(megaApiGateway.getMegaNodeByHandle(any())).thenReturn(megaNode)
            val actual = underTest.exportNode(node, expireTime)
            assertThat(actual).isEqualTo(expected)
        }


    @Test
    fun `test that exportNode is successful when SDK call is successful`() = runTest {

        val node = NodeId(1L)
        val expireTime = 2L
        val expected = "result_link"
        val megaNode = mock<MegaNode> {
            on { isTakenDown }.thenReturn(false)
        }
        whenever(megaApiGateway.getMegaNodeByHandle(node.longValue)).thenReturn(megaNode)

        whenever(megaApiGateway.exportNode(any(), any(), any())).thenAnswer {
            ((it.arguments[2]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                api = mock(),
                request = mock {
                    on { link }.thenReturn(expected)
                },
                error = mock {
                    on { errorCode }.thenReturn(
                        MegaError.API_OK
                    )
                },
            )
        }
        assertThat(underTest.exportNode(node, expireTime)).isEqualTo(expected)
    }

    @Test
    fun `test that exportNode throws MegaException when SDK returns error`() =
        runTest {
            val node = NodeId(1L)
            val expireTime = 2L
            val megaNode = mock<MegaNode> {
                on { isTakenDown }.thenReturn(false)
            }
            whenever(megaApiGateway.getMegaNodeByHandle(node.longValue)).thenReturn(megaNode)

            whenever(megaApiGateway.exportNode(any(), any(), any())).thenAnswer {
                ((it.arguments[2]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                    api = mock(),
                    request = mock(),
                    error = mock {
                        on { errorCode }.thenReturn(
                            MegaError.API_EINTERNAL
                        )
                    },
                )
            }
            assertThrows<MegaException> {
                underTest.exportNode(node, expireTime)
            }
        }

    @Test
    fun `test that checkIfNodeHasTheRequiredAccessLevelPermission when the method execute successfully`() =
        runTest {
            val nodeId = NodeId(1L)
            val megaNode = mock<MegaNode>()
            val permission = AccessPermission.FULL
            val error = mock<MegaError> {
                on { errorCode }.thenReturn(MegaError.API_OK)
            }
            whenever(megaApiGateway.getMegaNodeByHandle(nodeId.longValue)).thenReturn(megaNode)
            whenever(
                megaApiGateway.checkAccessErrorExtended(
                    node = megaNode,
                    level = accessPermissionIntMapper(accessPermission = permission)
                )
            ).thenReturn(error)
            val response =
                underTest.checkIfNodeHasTheRequiredAccessLevelPermission(nodeId, permission)
            assertThat(response).isTrue()
        }

    @Test
    fun `test that checkIfNodeHasTheRequiredAccessLevelPermission return false when getMegaNode returns null`() =
        runTest {
            val nodeId = NodeId(1L)
            val permission = AccessPermission.FULL
            whenever(megaApiGateway.getMegaNodeByHandle(nodeId.longValue)).thenReturn(null)
            whenever(megaApiFolderGateway.getMegaNodeByHandle(nodeId.longValue)).thenReturn(null)
            val response =
                underTest.checkIfNodeHasTheRequiredAccessLevelPermission(nodeId, permission)
            assertThat(response).isFalse()
        }

    @Test
    fun `test that exception is returned when moveNodeToRubbishBinByHandle is invoked and rubbish bin is not found`() =
        runTest {
            val nodeId = NodeId(1L)
            whenever(megaApiGateway.getRubbishBinNode()).thenReturn(null)
            whenever(megaApiGateway.getMegaNodeByHandle(nodeId.longValue)).thenReturn(mock())
            assertThrows<IllegalArgumentException> {
                underTest.moveNodeToRubbishBinByHandle(NodeId(1L))
            }
        }

    @Test
    fun `test that exception is returned when moveNodeToRubbishBinByHandle is invoked and node is not found`() =
        runTest {
            whenever(megaApiGateway.getRubbishBinNode()).thenReturn(null)
            assertThrows<IllegalArgumentException> {
                underTest.moveNodeToRubbishBinByHandle(NodeId(1L))
            }
        }

    @Test
    fun `test that moveNodeToRubbishBinByHandle success when sdk success`() = runTest {
        val node = NodeId(1L)
        val result = NodeId(2L)
        val megaNode = mock<MegaNode>()
        val rubbishBinNode = mock<MegaNode>()
        whenever(megaApiGateway.getMegaNodeByHandle(node.longValue)).thenReturn(megaNode)
        whenever(megaApiGateway.getRubbishBinNode()).thenReturn(rubbishBinNode)
        whenever(megaApiGateway.moveNode(any(), any(), anyOrNull(), any())).thenAnswer {
            ((it.arguments[3]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                api = mock(),
                request = mock {
                    on { nodeHandle }.thenReturn(result.longValue)
                },
                error = mock {
                    on { errorCode }.thenReturn(
                        MegaError.API_OK
                    )
                },
            )
        }
        assertDoesNotThrow {
            underTest.moveNodeToRubbishBinByHandle(node)
        }
    }

    @Test
    fun `test that getNodeFromChatMessage returns correct node from gateway`() = runTest {
        val chatId = 11L
        val messageId = 22L
        val megaNode = mockMegaNodeForConversion()
        val megaChatMessage = mock<MegaChatMessage>()
        val megaNodeList = mock<MegaNodeList>()
        whenever(megaNodeList.get(0)).thenReturn(megaNode)
        whenever(megaChatMessage.megaNodeList).thenReturn(megaNodeList)
        whenever(megaChatApiGateway.getMessage(chatId, messageId)).thenReturn(megaChatMessage)
        assertThat(
            underTest.getNodeFromChatMessage(chatId, messageId)?.id?.longValue
        ).isEqualTo(megaNode.handle)
    }

    @Test
    fun `test that chat node history is used as a fallback when node is not found`() = runTest {
        val chatId = 11L
        val messageId = 22L
        val megaNode = mockMegaNodeForConversion()
        val megaChatMessage = mock<MegaChatMessage>()
        val megaNodeList = mock<MegaNodeList>()
        whenever(megaNodeList.get(0)).thenReturn(megaNode)
        whenever(megaChatMessage.megaNodeList).thenReturn(megaNodeList)
        whenever(megaChatApiGateway.getMessage(chatId, messageId)).thenReturn(null)
        whenever(megaChatApiGateway.getMessageFromNodeHistory(chatId, messageId))
            .thenReturn(megaChatMessage)
        assertThat(
            underTest.getNodeFromChatMessage(chatId, messageId)?.id?.longValue
        ).isEqualTo(megaNode.handle)
    }

    @Test
    fun `test that chat node is authorized if is in chat preview`() = runTest {
        val chatId = 11L
        val messageId = 22L
        val authorizationToken = "authorizationToken"
        val megaNode = mockMegaNodeForConversion()
        val megaNode2 = mock<MegaNode>()
        val megaChatMessage = mock<MegaChatMessage>()
        val megaNodeList = mock<MegaNodeList>()
        val chat = mock<MegaChatRoom>()
        whenever(megaNodeList.get(0)).thenReturn(megaNode2)
        whenever(megaChatMessage.megaNodeList).thenReturn(megaNodeList)
        whenever(megaChatApiGateway.getMessage(chatId, messageId)).thenReturn(megaChatMessage)
        whenever(megaChatApiGateway.getChatRoom(chatId)).thenReturn(chat)
        whenever(chat.isPreview).thenReturn(true)
        whenever(chat.authorizationToken).thenReturn(authorizationToken)
        whenever(megaApiGateway.authorizeChatNode(megaNode2, chat.authorizationToken))
            .thenReturn(megaNode)
        assertThat(
            underTest.getNodeFromChatMessage(chatId, messageId)?.id?.longValue
        ).isEqualTo(megaNode.handle)
        verify(megaApiGateway).authorizeChatNode(megaNode2, chat.authorizationToken)
    }

    @Test
    fun `test that getOfflineFolderInfo should return correct OfflineFolderInfo`() = runTest {
        val parentId = 1
        val offlineNodes = listOf(
            Offline(
                1,
                "56",
                "path1",
                "name1",
                parentId,
                Offline.FOLDER,
                Offline.OTHER,
                "handleIncoming1",
                12345
            ),
            Offline(
                2,
                "67",
                "path2",
                "name2",
                parentId,
                Offline.FILE,
                Offline.INCOMING,
                "handleIncoming2",
                67890
            ),
        )
        whenever(megaLocalRoomGateway.getOfflineInfoByParentId(parentId)).thenReturn(offlineNodes)

        val expected = OfflineFolderInfo(1, 1)
        val actual = underTest.getOfflineFolderInfo(parentId)
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test that getNodeFromSerializedData returns mapped node from gateway with correct serialized data`() =
        runTest {
            val serializedData = "serialized Data"
            val megaNode = mockMegaNodeForConversion()
            whenever(megaNode.serialize()).thenReturn(serializedData)
            whenever(megaApiGateway.unSerializeNode(serializedData)).thenReturn(megaNode)
            whenever(megaApiGateway.getNumVersions(megaNode)).thenReturn(1)
            val actual = underTest.getNodeFromSerializedData(serializedData)
            assertThat(actual?.serializedData).isEqualTo(serializedData)
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
