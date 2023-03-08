package mega.privacy.android.data.repository

import com.google.common.truth.Truth.assertThat
import com.google.gson.Gson
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.constant.CacheFolderConstant
import mega.privacy.android.data.database.DatabaseHandler
import mega.privacy.android.data.gateway.CacheFolderGateway
import mega.privacy.android.data.gateway.FileGateway
import mega.privacy.android.data.gateway.api.MegaApiFolderGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.gateway.preferences.AppPreferencesGateway
import mega.privacy.android.data.mapper.FileTypeInfoMapper
import mega.privacy.android.data.mapper.SortOrderIntMapper
import mega.privacy.android.data.mapper.mediaplayer.SubtitleFileInfoMapper
import mega.privacy.android.data.mapper.toNode
import mega.privacy.android.data.model.node.DefaultFileNode
import mega.privacy.android.data.model.node.DefaultFolderNode
import mega.privacy.android.domain.entity.FileTypeInfo
import mega.privacy.android.domain.entity.FolderType
import mega.privacy.android.domain.entity.StaticImageFileTypeInfo
import mega.privacy.android.domain.entity.mediaplayer.PlaybackInformation
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.MediaPlayerRepository
import mega.privacy.android.domain.usecase.GetFolderType
import nz.mega.sdk.MegaNode
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
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
    private val appPreferencesGateway = mock<AppPreferencesGateway>()
    private val subtitleFileInfoMapper = mock<SubtitleFileInfoMapper>()

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
    private val expectedType = StaticImageFileTypeInfo(mimeType = "", extension = "image")
    private val expectedFileMegaNode = createMegaNode(false)
    private val expectedFolderMegaNode = createMegaNode(true)
    private val isNodeKetDecrypted = false
    private val expectedMediaId: Long = 1234567
    private val expectedTotalDuration: Long = 200000
    private val expectedCurrentPosition: Long = 16000
    private val expectedHasPreview = false

    @Before
    fun setUp() {
        underTest = DefaultMediaPlayerRepository(
            megaApi = megaApi,
            megaApiFolder = megaApiFolder,
            dbHandler = dbHandler,
            nodeMapper = ::toNode,
            cacheFolder = cacheFolder,
            fileTypeInfoMapper = fileTypeInfoMapper,
            fileGateway = fileGateway,
            sortOrderIntMapper = sortOrderIntMapper,
            appPreferencesGateway = appPreferencesGateway,
            ioDispatcher = UnconfinedTestDispatcher(),
            subtitleFileInfoMapper = subtitleFileInfoMapper,
        )
    }

    @Test
    fun `test that get typed file node by handle when mega node is file`() = runTest {
        whenever(megaApi.getMegaNodeByHandle(expectedHandle)).thenReturn(expectedFileMegaNode)
        initTestConditions(expectedFileMegaNode, expectedType)

        val expectedTypedNode = createTypedFileNode()

        val actualTypedFile = underTest.getUnTypedNodeByHandle(expectedHandle)

        assertThat(actualTypedFile).isNotNull()
        assertThat(actualTypedFile).isInstanceOf(DefaultFileNode::class.java)
        actualTypedFile?.let { nonNullActualTypedFile ->
            with(nonNullActualTypedFile) {
                this as DefaultFileNode
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
        val actualTypedFolder = underTest.getUnTypedNodeByHandle(expectedHandle)

        assertThat(actualTypedFolder).isNotNull()
        assertThat(actualTypedFolder).isInstanceOf(DefaultFolderNode::class.java)
        actualTypedFolder?.let { nonNullActualTypedFolder ->
            with(nonNullActualTypedFolder) {
                this as DefaultFolderNode
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

    @Test
    fun `test that updatePlayback information that there is no local data`() = runTest {
        val expectedPlaybackInfo = createPlaybackInformation()

        underTest.updatePlaybackInformation(expectedPlaybackInfo)
        whenever(appPreferencesGateway.monitorString(anyOrNull(),
            anyOrNull())).thenReturn(flowOf("{}"))
        val actual = underTest.monitorPlaybackTimes().firstOrNull()

        assertThat(actual?.get(expectedMediaId)?.mediaId).isEqualTo(expectedMediaId)
        assertThat(actual?.get(expectedMediaId)?.totalDuration).isEqualTo(expectedTotalDuration)
        assertThat(actual?.get(expectedMediaId)?.currentPosition).isEqualTo(expectedCurrentPosition)
    }

    @Test
    fun `test that monitorPlaybackTimes`() = runTest {
        val expectedPlaybackInfo = createPlaybackInformation()

        whenever(appPreferencesGateway.monitorString(anyOrNull(),
            anyOrNull())).thenReturn(flowOf(Gson().toJson(mapOf(Pair(expectedMediaId,
            expectedPlaybackInfo)))))
        val actual = underTest.monitorPlaybackTimes().firstOrNull()

        assertThat(actual?.get(expectedMediaId)?.mediaId).isEqualTo(expectedMediaId)
        assertThat(actual?.get(expectedMediaId)?.totalDuration).isEqualTo(expectedTotalDuration)
        assertThat(actual?.get(expectedMediaId)?.currentPosition).isEqualTo(expectedCurrentPosition)
    }

    @Test
    fun `test that deletePlaybackInformation that playbackInfoMap doesn't include deleted item even local data includes it`() =
        runTest {
            val expectedPlaybackInfo = createPlaybackInformation()
            val expectedDeleteMediaId: Long = 7654321
            val expectedDeleteTotalDuration: Long = 300000
            val expectedDeleteCurrentPosition: Long = 20000
            val expectedDeletePlaybackInfo = PlaybackInformation(
                expectedDeleteMediaId,
                expectedDeleteTotalDuration,
                expectedDeleteCurrentPosition
            )

            val expectedPlaybackInfoMap = mapOf(
                Pair(expectedMediaId, expectedPlaybackInfo),
                Pair(expectedDeleteMediaId, expectedDeletePlaybackInfo)
            )

            underTest.updatePlaybackInformation(expectedPlaybackInfo)
            underTest.updatePlaybackInformation(expectedDeletePlaybackInfo)
            underTest.deletePlaybackInformation(expectedDeleteMediaId)

            whenever(appPreferencesGateway.monitorString(anyOrNull(), anyOrNull())).thenReturn(
                flowOf(
                    Gson().toJson(expectedPlaybackInfoMap)))
            val actual = underTest.monitorPlaybackTimes().firstOrNull()

            assertThat(actual?.containsKey(expectedDeleteMediaId)).isFalse()
        }

    @Test
    fun `test that searchSubtitleFileInfoList return the empty list`() =
        runTest {
            val expectedName = "SubtitleTestName.srt"
            val expectedUrl = "subtitleUrl.com"
            val expectedMegaNode = mock<MegaNode> {
                on { name }.thenReturn(expectedName)
            }
            whenever(megaApi.httpServerGetLocalLink(expectedMegaNode)).thenReturn(expectedUrl)
            whenever(megaApi.search(any(),
                any(),
                any(),
                any())).thenReturn(listOf(expectedMegaNode))

            val actual = underTest.getSubtitleFileInfoList(".srt")

            assertThat(actual).isEmpty()
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

    private fun createTypedFolderNode() = DefaultFolderNode(
        id = expectedNodeId,
        parentId = expectedParentNodeId,
        name = expectedName,
        base64Id = expectedBase64Id,
        label = expectedLabel,
        hasVersion = expectedHasVersion,
        childFileCount = expectedGetNumChildFiles,
        childFolderCount = expectedGetNumChildFolders,
        isFavourite = expectedIsFavourite,
        isExported = expectedIsExported,
        isTakenDown = expectedIsTakenDown,
        isInRubbishBin = expectedInRubbishBin,
        isIncomingShare = expectedIncomingShare,
        isShared = expectedInShared,
        isPendingShare = expectedIsPendingShare,
        device = expectedDevice,
        isNodeKeyDecrypted = isNodeKetDecrypted,
    )

    private fun createTypedFileNode() = DefaultFileNode(
        id = expectedNodeId,
        parentId = expectedParentNodeId,
        name = expectedName,
        base64Id = expectedBase64Id,
        label = expectedLabel,
        hasVersion = expectedHasVersion,
        isFavourite = expectedIsFavourite,
        isExported = expectedIsExported,
        isTakenDown = expectedIsTakenDown,
        isIncomingShare = expectedIncomingShare,
        size = expectedSize,
        modificationTime = expectedModificationTime,
        fingerprint = expectedFingerprint,
        thumbnailPath = expectedThumbnailPath,
        type = expectedType,
        isNodeKeyDecrypted = isNodeKetDecrypted,
        hasPreview = expectedHasPreview,
    )

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

    private fun createPlaybackInformation() = PlaybackInformation(
        mediaId = expectedMediaId,
        totalDuration = expectedTotalDuration,
        currentPosition = expectedCurrentPosition
    )
}