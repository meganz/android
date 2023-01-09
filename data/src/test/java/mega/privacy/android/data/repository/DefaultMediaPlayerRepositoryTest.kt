package mega.privacy.android.data.repository

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.constant.CacheFolderConstant
import mega.privacy.android.data.database.DatabaseHandler
import mega.privacy.android.data.gateway.CacheFolderGateway
import mega.privacy.android.data.gateway.FileGateway
import mega.privacy.android.data.gateway.api.MegaApiFolderGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.mapper.FileTypeInfoMapper
import mega.privacy.android.data.mapper.SortOrderIntMapper
import mega.privacy.android.data.mapper.toNode
import mega.privacy.android.domain.entity.FileTypeInfo
import mega.privacy.android.domain.entity.FolderType
import mega.privacy.android.domain.entity.StaticImageFileTypeInfo
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.repository.MediaPlayerRepository
import mega.privacy.android.domain.usecase.DefaultAddNodeType
import mega.privacy.android.domain.usecase.GetFolderType
import nz.mega.sdk.MegaNode
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultMediaPlayerRepositoryTest {
    private lateinit var underTest: MediaPlayerRepository

    private val megaApi = mock<MegaApiGateway>()
    private val megaApiFolder = mock<MegaApiFolderGateway>()
    private val dbHandler = mock<DatabaseHandler>()
    private val cacheFolder = mock<CacheFolderGateway>()
    private val fileTypeInfoMapper = mock<FileTypeInfoMapper>()
    private val fileGateway = mock<FileGateway>()
    private val getFolderType = mock<GetFolderType>()
    private val sortOrderIntMapper = mock<SortOrderIntMapper>()

    private val expectedHandle = 100L
    private val expectedParentHandle = 999L
    private val expectedHasVersion = true
    private val expectedGetNumChildFolders = 12
    private val expectedGetNumChildFiles = 34
    private val expectedName = "name"
    private val expectedLabel = 6
    private val expectedNodeId = NodeId(expectedHandle)
    private val expectedParentNodeId = NodeId(expectedParentHandle)
    private val expectedBase64Id = "base 64 id"
    private val expectedIsFavourite = true
    private val expectedIsExported = true
    private val expectedIsTakenDown = true
    private val expectedInRubbishBin = true
    private val expectedIncomingShare = true
    private val expectedInShared = false
    private val expectedIsPendingShare = false
    private val expectedDevice = "device id"
    private val expectedSize = 1000L
    private val expectedModificationTime = 2000L
    private val expectedThumbnailPath: String? = null
    private val expectedFingerprint = "fingerprint"
    private val expectedType = StaticImageFileTypeInfo(type = "", extension = "image")
    private val expectedFileMegaNode = createMegaNode(false)
    private val expectedFolderMegaNode = createMegaNode(true)

    @Before
    fun setUp() {
        underTest = DefaultMediaPlayerRepository(
            megaApi = megaApi,
            megaApiFolder = megaApiFolder,
            dbHandler = dbHandler,
            nodeMapper = ::toNode,
            cacheFolder = cacheFolder,
            fileTypeInfoMapper = fileTypeInfoMapper,
            addNodeType = DefaultAddNodeType(getFolderType),
            fileGateway = fileGateway,
            sortOrderIntMapper = sortOrderIntMapper,
            ioDispatcher = UnconfinedTestDispatcher()
        )
    }

    @Test
    fun `test that get typed file node by handle when mega node is file`() = runTest {
        whenever(megaApi.getMegaNodeByHandle(expectedHandle)).thenReturn(expectedFileMegaNode)
        initTestConditions(expectedFileMegaNode, expectedType)

        val expectedTypedNode = createTypedFileNode()

        val actualTypedFile = underTest.getTypedNodeByHandle(expectedHandle)

        assertThat(actualTypedFile).isNotNull()
        assertThat(actualTypedFile).isInstanceOf(TypedFileNode::class.java)
        actualTypedFile?.let { nonNullActualTypedFile ->
            with(nonNullActualTypedFile) {
                this as TypedFileNode
                assertThat(id).isEqualTo(expectedTypedNode.id)
                assertThat(parentId).isEqualTo(expectedTypedNode.parentId)
                assertThat(name).isEqualTo(expectedTypedNode.name)
                assertThat(base64Id).isEqualTo(expectedTypedNode.base64Id)
                assertThat(label).isEqualTo(expectedTypedNode.label)
                assertThat(hasVersion).isEqualTo(expectedTypedNode.hasVersion)
                assertThat(isFavourite).isEqualTo(expectedTypedNode.isFavourite)
                assertThat(isExported).isEqualTo(expectedTypedNode.isExported)
                assertThat(isTakenDown).isEqualTo(expectedTypedNode.isTakenDown)
                assertThat(isIncomingShare).isEqualTo(expectedTypedNode.isIncomingShare)
                assertThat(size).isEqualTo(expectedTypedNode.size)
                assertThat(modificationTime).isEqualTo(expectedTypedNode.modificationTime)
                assertThat(type).isEqualTo(expectedTypedNode.type)
                assertThat(thumbnailPath).isEqualTo(expectedTypedNode.thumbnailPath)
                assertThat(fingerprint).isEqualTo(expectedTypedNode.fingerprint)
            }
        }
    }

    @Test
    fun `test that get typed file node by handle when mega node is folder`() = runTest {
        whenever(megaApi.getMegaNodeByHandle(expectedHandle)).thenReturn(expectedFolderMegaNode)
        initTestConditions(expectedFolderMegaNode, expectedType)

        val expectedTypedNode = createTypedFolderNode()
        val actualTypedFolder = underTest.getTypedNodeByHandle(expectedHandle)

        assertThat(actualTypedFolder).isNotNull()
        assertThat(actualTypedFolder).isInstanceOf(TypedFolderNode::class.java)
        actualTypedFolder?.let { nonNullActualTypedFolder ->
            with(nonNullActualTypedFolder) {
                this as TypedFolderNode
                assertThat(id).isEqualTo(expectedTypedNode.id)
                assertThat(parentId).isEqualTo(expectedTypedNode.parentId)
                assertThat(name).isEqualTo(expectedTypedNode.name)
                assertThat(base64Id).isEqualTo(expectedTypedNode.base64Id)
                assertThat(label).isEqualTo(expectedTypedNode.label)
                assertThat(hasVersion).isEqualTo(expectedTypedNode.hasVersion)
                assertThat(isFavourite).isEqualTo(expectedTypedNode.isFavourite)
                assertThat(isExported).isEqualTo(expectedTypedNode.isExported)
                assertThat(isTakenDown).isEqualTo(expectedTypedNode.isTakenDown)
                assertThat(isIncomingShare).isEqualTo(expectedTypedNode.isIncomingShare)
                assertThat(isInRubbishBin).isEqualTo(expectedTypedNode.isInRubbishBin)
                assertThat(isShared).isEqualTo(expectedTypedNode.isShared)
                assertThat(isPendingShare).isEqualTo(expectedTypedNode.isPendingShare)
                assertThat(device).isEqualTo(expectedTypedNode.device)
                assertThat(childFolderCount).isEqualTo(expectedTypedNode.childFolderCount)
                assertThat(childFileCount).isEqualTo(expectedTypedNode.childFileCount)
            }
        }
    }

    @Test
    fun `test that get local link for folder link using MegaApi`() = runTest {
        val node = mock<MegaNode>()
        val expectedLocalLink = "local link"
        whenever(megaApiFolder.getMegaNodeByHandle(expectedHandle)).thenReturn(node)
        whenever(megaApiFolder.authorizeNode(node)).thenReturn(node)
        whenever(megaApi.httpServerGetLocalLink(any())).thenReturn(expectedLocalLink)

        val actual = underTest.getLocalLinkForFolderLinkFromMegaApi(expectedHandle)

        assertThat(actual).isEqualTo(expectedLocalLink)
    }

    @Test
    fun `test that get local link for folder link using MegaApiFolder`() = runTest {
        val node = mock<MegaNode>()
        val expectedLocalLink = "local link"
        whenever(megaApiFolder.getMegaNodeByHandle(expectedHandle)).thenReturn(node)
        whenever(megaApiFolder.authorizeNode(node)).thenReturn(node)
        whenever(megaApiFolder.httpServerGetLocalLink(any())).thenReturn(expectedLocalLink)

        val actual = underTest.getLocalLinkForFolderLinkFromMegaApiFolder(expectedHandle)

        assertThat(actual).isEqualTo(expectedLocalLink)
    }

    @Test
    fun `test that get local link from mega api`() = runTest {
        val expectedLocalLink = "local link"
        whenever(megaApi.getMegaNodeByHandle(expectedHandle)).thenReturn(mock())
        whenever(megaApi.httpServerGetLocalLink(any())).thenReturn(expectedLocalLink)

        val actual = underTest.getLocalLinkFromMegaApi(expectedHandle)

        assertThat(actual).isEqualTo(expectedLocalLink)
    }

    private fun createMegaNode(isFolder: Boolean) = mock<MegaNode> {
        on { handle }.thenReturn(expectedHandle)
        on { parentHandle }.thenReturn(expectedParentHandle)
        on { this.isFolder }.thenReturn(isFolder)
        on { this.isFile }.thenReturn(!isFolder)
        on { name }.thenReturn(expectedName)
        on { label }.thenReturn(expectedLabel)
        on { base64Handle }.thenReturn(expectedBase64Id)
        on { isFavourite }.thenReturn(expectedIsFavourite)
        on { isExported }.thenReturn(expectedIsExported)
        on { isTakenDown }.thenReturn(expectedIsTakenDown)
        on { isInShare }.thenReturn(expectedIncomingShare)
        on { isOutShare }.thenReturn(expectedInShared)
        on { deviceId }.thenReturn(expectedDevice)
        on { size }.thenReturn(expectedSize)
        on { modificationTime }.thenReturn(expectedModificationTime)
        on { fingerprint }.thenReturn(expectedFingerprint)
    }

    private fun createTypedFolderNode() = mock<TypedFolderNode> {
        on { id }.thenReturn(expectedNodeId)
        on { parentId }.thenReturn(expectedParentNodeId)
        on { name }.thenReturn(expectedName)
        on { base64Id }.thenReturn(expectedBase64Id)
        on { label }.thenReturn(expectedLabel)
        on { hasVersion }.thenReturn(expectedHasVersion)
        on { childFileCount }.thenReturn(expectedGetNumChildFiles)
        on { childFolderCount }.thenReturn(expectedGetNumChildFolders)
        on { isFavourite }.thenReturn(expectedIsFavourite)
        on { isExported }.thenReturn(expectedIsExported)
        on { isTakenDown }.thenReturn(expectedIsTakenDown)
        on { isInRubbishBin }.thenReturn(expectedInRubbishBin)
        on { isIncomingShare }.thenReturn(expectedIncomingShare)
        on { isShared }.thenReturn(expectedInShared)
        on { isPendingShare }.thenReturn(expectedIsPendingShare)
        on { device }.thenReturn(expectedDevice)
    }

    private fun createTypedFileNode() = mock<TypedFileNode> {
        on { id }.thenReturn(expectedNodeId)
        on { parentId }.thenReturn(expectedParentNodeId)
        on { name }.thenReturn(expectedName)
        on { base64Id }.thenReturn(expectedBase64Id)
        on { label }.thenReturn(expectedLabel)
        on { hasVersion }.thenReturn(expectedHasVersion)
        on { isFavourite }.thenReturn(expectedIsFavourite)
        on { isExported }.thenReturn(expectedIsExported)
        on { isTakenDown }.thenReturn(expectedIsTakenDown)
        on { isIncomingShare }.thenReturn(expectedIncomingShare)
        on { size }.thenReturn(expectedSize)
        on { modificationTime }.thenReturn(expectedModificationTime)
        on { fingerprint }.thenReturn(expectedFingerprint)
        on { thumbnailPath }.thenReturn(expectedThumbnailPath)
        on { type }.thenReturn(expectedType)
    }

    private suspend fun initTestConditions(megaNode: MegaNode, typeInfo: FileTypeInfo) {
        whenever(megaApi.hasVersion(megaNode)).thenReturn(expectedHasVersion)
        whenever(megaApi.getNumChildFolders(megaNode)).thenReturn(expectedGetNumChildFolders)
        whenever(megaApi.getNumChildFiles(megaNode)).thenReturn(expectedGetNumChildFiles)
        whenever(megaApi.isInRubbish(megaNode)).thenReturn(expectedInRubbishBin)
        whenever(megaApi.isPendingShare(megaNode)).thenReturn(expectedIsPendingShare)
        whenever(fileTypeInfoMapper(megaNode)).thenReturn(expectedType)
        whenever(cacheFolder.getCacheFolder(CacheFolderConstant.THUMBNAIL_FOLDER)).thenReturn(null)
        whenever(getFolderType(any())).thenReturn(FolderType.Default)
        whenever(fileTypeInfoMapper(any())).thenReturn(typeInfo)
    }
}