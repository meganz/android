package mega.privacy.android.app.presentation.filelink

import android.content.Intent
import android.net.Uri
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.StateEventWithContentTriggered
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.presentation.folderlink.model.LinkErrorState
import mega.privacy.android.core.nodecomponents.mapper.FileTypeIconMapper
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.StaticImageFileTypeInfo
import mega.privacy.android.domain.entity.node.NodeContentUri
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeNameCollision
import mega.privacy.android.domain.entity.node.NodeNameCollisionType
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.publiclink.PublicLinkFile
import mega.privacy.android.domain.entity.node.publiclink.PublicNodeNameCollisionResult
import mega.privacy.android.domain.exception.PublicNodeException
import mega.privacy.android.domain.usecase.HasCredentialsUseCase
import mega.privacy.android.domain.usecase.RootNodeExistsUseCase
import mega.privacy.android.domain.usecase.advertisements.QueryAdsUseCase
import mega.privacy.android.domain.usecase.filelink.GetFileUrlByPublicLinkUseCase
import mega.privacy.android.domain.usecase.filelink.GetPublicNodeUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiHttpServerIsRunningUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiHttpServerStartUseCase
import mega.privacy.android.domain.usecase.network.IsConnectedToInternetUseCase
import mega.privacy.android.domain.usecase.node.GetFileLinkNodeContentUriUseCase
import mega.privacy.android.domain.usecase.node.GetNodePreviewFileUseCase
import mega.privacy.android.domain.usecase.node.publiclink.CheckPublicNodesNameCollisionUseCase
import mega.privacy.android.domain.usecase.node.publiclink.CopyPublicNodeUseCase
import mega.privacy.android.domain.usecase.node.publiclink.MapNodeToPublicLinkUseCase
import mega.privacy.android.core.nodecomponents.mapper.NodeContentUriIntentMapper
import mega.privacy.android.navigation.MegaNavigator
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@ExtendWith(CoroutineMainDispatcherExtension::class)
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FileLinkViewModelTest {

    private lateinit var underTest: FileLinkViewModel
    private val isConnectedToInternetUseCase = mock<IsConnectedToInternetUseCase>()
    private val hasCredentialsUseCase = mock<HasCredentialsUseCase>()
    private val rootNodeExistsUseCase = mock<RootNodeExistsUseCase>()
    private val getPublicNodeUseCase = mock<GetPublicNodeUseCase>()
    private val copyPublicNodeUseCase = mock<CopyPublicNodeUseCase>()
    private val checkPublicNodesNameCollisionUseCase = mock<CheckPublicNodesNameCollisionUseCase>()
    private val httpServerStart = mock<MegaApiHttpServerStartUseCase>()
    private val httpServerIsRunning = mock<MegaApiHttpServerIsRunningUseCase>()
    private val getFileUrlByPublicLinkUseCase = mock<GetFileUrlByPublicLinkUseCase>()
    private val mapNodeToPublicLinkUseCase = mock<MapNodeToPublicLinkUseCase>()
    private val fileTypeIconMapper = mock<FileTypeIconMapper>()
    private val getFileLinkNodeContentUriUseCase = mock<GetFileLinkNodeContentUriUseCase>()
    private val megaNavigator = mock<MegaNavigator>()
    private val nodeContentUriIntentMapper = mock<NodeContentUriIntentMapper>()
    private val getNodePreviewFileUseCase = mock<GetNodePreviewFileUseCase>()
    private val queryAdsUseCase = mock<QueryAdsUseCase>()

    private val url = "https://mega.co.nz/abc"
    private val filePreviewPath = "data/cache/xyz.jpg"
    private val title = "abc"
    private val fileSize = 100000L
    private val serializedString = "serializedString"
    private val parentNodeHandle = 123L

    @BeforeEach
    fun resetMocks() {
        reset(
            isConnectedToInternetUseCase,
            hasCredentialsUseCase,
            rootNodeExistsUseCase,
            getPublicNodeUseCase,
            copyPublicNodeUseCase,
            checkPublicNodesNameCollisionUseCase,
            httpServerStart,
            httpServerIsRunning,
            getFileUrlByPublicLinkUseCase,
            mapNodeToPublicLinkUseCase,
            getFileLinkNodeContentUriUseCase,
            megaNavigator,
            nodeContentUriIntentMapper,
            getNodePreviewFileUseCase,
            queryAdsUseCase
        )
        initViewModel()
    }

    private fun initViewModel() {
        underTest = FileLinkViewModel(
            isConnectedToInternetUseCase = isConnectedToInternetUseCase,
            hasCredentialsUseCase = hasCredentialsUseCase,
            rootNodeExistsUseCase = rootNodeExistsUseCase,
            getPublicNodeUseCase = getPublicNodeUseCase,
            checkPublicNodesNameCollisionUseCase = checkPublicNodesNameCollisionUseCase,
            copyPublicNodeUseCase = copyPublicNodeUseCase,
            httpServerStart = httpServerStart,
            httpServerIsRunning = httpServerIsRunning,
            getFileUrlByPublicLinkUseCase = getFileUrlByPublicLinkUseCase,
            mapNodeToPublicLinkUseCase = mapNodeToPublicLinkUseCase,
            fileTypeIconMapper = fileTypeIconMapper,
            getFileLinkNodeContentUriUseCase = getFileLinkNodeContentUriUseCase,
            megaNavigator = megaNavigator,
            nodeContentUriIntentMapper = nodeContentUriIntentMapper,
            getNodePreviewFileUseCase = getNodePreviewFileUseCase,
            monitorMiscLoadedUseCase = mock(),
            queryAdsUseCase = queryAdsUseCase
        )
    }

    @Test
    fun `test that initial state is returned`() = runTest {
        underTest.state.test {
            val initial = awaitItem()
            assertThat(initial.showLoginScreenEvent).isEqualTo(consumed)
            assertThat(initial.hasDbCredentials).isFalse()
            assertThat(initial.url).isEmpty()
            assertThat(initial.fileNode).isNull()
            assertThat(initial.title).isEmpty()
            assertThat(initial.sizeInBytes).isEqualTo(0)
            assertThat(initial.handle).isEqualTo(-1)
            assertThat(initial.previewPath).isNull()
            assertThat(initial.iconResource).isNull()
            assertThat(initial.askForDecryptionKeyDialogEvent).isEqualTo(consumed)
            assertThat(initial.collisionsEvent).isEqualTo(consumed())
            assertThat(initial.copySuccessEvent).isEqualTo(consumed)
            assertThat(initial.openFile).isInstanceOf(consumed().javaClass)
        }
    }

    @Test
    fun `test that showLoginScreenEvent is not triggered when root node exists and db credentials are valid`() =
        runTest {
            whenever(hasCredentialsUseCase()).thenReturn(true)
            whenever(rootNodeExistsUseCase()).thenReturn(true)

            underTest.state.test {
                underTest.checkLoginRequired()
                val newValue = expectMostRecentItem()
                assertThat(newValue.showLoginScreenEvent).isEqualTo(consumed)
                assertThat(newValue.hasDbCredentials).isTrue()
            }
        }

    @Test
    fun `test that showLoginScreenEvent is consumed when onShowLoginScreenEventConsumed is invoked`() =
        runTest {
            underTest.state.test {
                underTest.onShowLoginScreenEventConsumed()
                val newValue = expectMostRecentItem()
                assertThat(newValue.showLoginScreenEvent).isEqualTo(consumed)
            }
        }

    @Test
    fun `test that showLoginScreenEvent is triggered when db credentials are valid and rootnode does not exist`() =
        runTest {
            whenever(hasCredentialsUseCase()).thenReturn(true)
            whenever(rootNodeExistsUseCase()).thenReturn(false)

            underTest.state.test {
                underTest.checkLoginRequired()
                val newValue = expectMostRecentItem()
                assertThat(newValue.showLoginScreenEvent).isEqualTo(triggered)
                assertThat(newValue.hasDbCredentials).isTrue()
            }
        }

    @Test
    fun `test that on getting valid public node correct values are set`() = runTest {
        val publicNode = mockFileNode()
        whenever(getPublicNodeUseCase(any())).thenReturn(publicNode)
        underTest.state.test {
            underTest.getPublicNode(url)
            val result = expectMostRecentItem()
            assertThat(result.title).isEqualTo(title)
            assertThat(result.sizeInBytes).isEqualTo(fileSize)
            assertThat(result.previewPath).isEqualTo(filePreviewPath)
            assertThat(result.iconResource).isEqualTo(null)
        }
    }

    @Test
    fun `test that on getting DecryptionKeyRequired exception askForDecryptionDialog values is set`() =
        runTest {
            val url = "https://mega.co.nz/abc"

            whenever(getPublicNodeUseCase(any())).thenThrow(PublicNodeException.DecryptionKeyRequired())
            underTest.state.test {
                underTest.getPublicNode(url)
                val result = expectMostRecentItem()
                assertThat(result.askForDecryptionKeyDialogEvent).isEqualTo(triggered)
            }
        }

    @Test
    fun `test that on getting InvalidDecryptionKey exception when fetching from decrypt dialog askForDecryptionDialog value is set`() =
        runTest {
            val url = "https://mega.co.nz/abc"

            whenever(getPublicNodeUseCase(any())).thenThrow(PublicNodeException.InvalidDecryptionKey())
            underTest.state.test {
                underTest.getPublicNode(url, true)
                val result = expectMostRecentItem()
                assertThat(result.askForDecryptionKeyDialogEvent).isEqualTo(triggered)
            }
        }

    @Test
    fun `test that that error state is set when InvalidDecryptionKey exception is returned but decryptionIntroduced is true`() =
        runTest {
            val url = "https://mega.co.nz/abc"

            whenever(getPublicNodeUseCase(any())).thenThrow(PublicNodeException.InvalidDecryptionKey())
            underTest.state.test {
                underTest.getPublicNode(url, false)
                val result = expectMostRecentItem()
                assertThat(result.askForDecryptionKeyDialogEvent).isEqualTo(consumed)
                assertThat(result.errorState).isEqualTo(LinkErrorState.Unavailable)
            }
        }

    @Test
    fun `test that error state is set when GenericError exception error is returned`() = runTest {
        val url = "https://mega.co.nz/abc"

        whenever(getPublicNodeUseCase(any())).thenThrow(PublicNodeException.GenericError())
        underTest.state.test {
            underTest.getPublicNode(url)
            val result = expectMostRecentItem()
            assertThat(result.errorState).isEqualTo(LinkErrorState.Unavailable)
        }
    }

    @Test
    fun `test that askForDecryptionDialog should be reset to false when resetAskForDecryptionKeyDialog is invoked`() =
        runTest {
            underTest.state.test {
                underTest.resetAskForDecryptionKeyDialog()
                val newValue = expectMostRecentItem()
                assertThat(newValue.askForDecryptionKeyDialogEvent).isEqualTo(consumed)
            }
        }

    @Test
    fun `test that openFile should be reset to consumed when resetOpenFile is invoked`() = runTest {
        underTest.state.test {
            underTest.resetOpenFile()
            val newValue = expectMostRecentItem()
            assertThat(newValue.openFile).isInstanceOf(consumed().javaClass)
        }
    }

    @Test
    fun `test that collision value is not null on importing node with same name `() = runTest {
        val nodeNameCollision = mock<NodeNameCollision.Default> {
            on { collisionHandle }.thenReturn(123)
            on { nodeHandle }.thenReturn(1234)
            on { name }.thenReturn("name")
            on { size }.thenReturn(12345)
            on { childFolderCount }.thenReturn(0)
            on { childFileCount }.thenReturn(0)
            on { lastModified }.thenReturn(12345)
            on { parentHandle }.thenReturn(1235)
            on { isFile }.thenReturn(false)
            on { serializedData }.thenReturn("serializedData")
        }
        val publicNodeNameCollisionResult = PublicNodeNameCollisionResult(
            listOf(), listOf(nodeNameCollision), NodeNameCollisionType.COPY
        )
        val publicNode = mock<TypedFileNode> {
            on { this.previewPath }.thenReturn(filePreviewPath)
            on { this.name }.thenReturn(title)
            on { this.size }.thenReturn(fileSize)
            on { this.serializedData }.thenReturn(serializedString)
            on { this.type }.thenReturn(
                StaticImageFileTypeInfo(
                    mimeType = "image/jpeg",
                    extension = "jpg"
                )
            )
        }

        whenever(getPublicNodeUseCase(any())).thenReturn(publicNode)
        whenever(
            checkPublicNodesNameCollisionUseCase(
                listOf(publicNode),
                parentNodeHandle,
                NodeNameCollisionType.COPY
            )
        ).thenReturn(publicNodeNameCollisionResult)

        underTest.state.test {
            underTest.getPublicNode(url)
            underTest.handleImportNode(parentNodeHandle)
            val newValue = expectMostRecentItem()
            assertThat(newValue.collisionsEvent).isNotEqualTo(consumed())
        }
    }

    @Test
    fun `test that collision value is null on importing node without same name`() = runTest {
        val publicNode = mock<TypedFileNode> {
            on { this.previewPath }.thenReturn(filePreviewPath)
            on { this.name }.thenReturn(title)
            on { this.size }.thenReturn(fileSize)
            on { this.serializedData }.thenReturn(serializedString)
            on { this.type }.thenReturn(
                StaticImageFileTypeInfo(
                    mimeType = "image/jpeg",
                    extension = "jpg"
                )
            )
        }
        val publicNodeNameCollisionResult = PublicNodeNameCollisionResult(
            listOf(publicNode), listOf(), NodeNameCollisionType.COPY
        )

        whenever(getPublicNodeUseCase(any())).thenReturn(publicNode)
        whenever(
            checkPublicNodesNameCollisionUseCase(
                listOf(publicNode),
                parentNodeHandle,
                NodeNameCollisionType.COPY
            )
        ).thenReturn(publicNodeNameCollisionResult)

        underTest.state.test {
            underTest.getPublicNode(url)
            underTest.handleImportNode(parentNodeHandle)
            val newValue = expectMostRecentItem()
            assertThat(newValue.collisionsEvent).isEqualTo(consumed())
        }
    }

    @Test
    fun `test that overQuotaError should be reset to consumed when resetDownloadFile is invoked`() =
        runTest {
            underTest.state.test {
                underTest.resetDownloadFile()
                val newValue = expectMostRecentItem()
                assertThat(newValue.overQuotaError).isInstanceOf(consumed().javaClass)
            }
        }

    @Test
    fun `test that foreignNodeError should be reset to consumed when resetDownloadFile is invoked`() =
        runTest {
            underTest.state.test {
                underTest.resetDownloadFile()
                val newValue = expectMostRecentItem()
                assertThat(newValue.foreignNodeError).isEqualTo(consumed)
            }
        }

    @Test
    fun `test that openFile is triggered when updateImageIntent is invoked`() = runTest {
        val intent = mock<Intent>()
        underTest.updateImageIntent(intent)
        underTest.state.test {
            val res = awaitItem()
            assertThat(res.openFile).isInstanceOf(triggered(intent).javaClass)
        }
    }

    @Test
    fun `test that openFile is triggered with correct pdf intent when updatePdfIntent is invoked`() =
        runTest {
            val uriMock = Mockito.mockStatic(Uri::class.java)
            val intent = mock<Intent>()
            val contentUriMock: Uri = mock()
            val path = "/path"


            whenever(httpServerIsRunning()).thenReturn(0)
            whenever(getFileUrlByPublicLinkUseCase(any())).thenReturn(path)
            whenever(Uri.parse(path)).thenReturn(contentUriMock)

            underTest.updatePdfIntent(intent, "pdf")
            underTest.state.test {
                val res = awaitItem()
                assertThat(res.openFile).isInstanceOf(triggered(intent).javaClass)
            }
            uriMock.close()
        }

    @Test
    fun `test that openFile is triggered when updateTextEditorIntent is invoked`() = runTest {
        val intent = mock<Intent>()
        underTest.updateTextEditorIntent(intent)
        underTest.state.test {
            val res = awaitItem()
            assertThat(res.openFile).isInstanceOf(triggered(intent).javaClass)
        }
    }

    @Test
    fun `test that downloadEvent is updated to trigger event when handle save file and download worker feature flag is true`() =
        runTest {
            val fileNode = mockFileNode()
            val publicNode = mock<PublicLinkFile>()
            whenever(getPublicNodeUseCase(any())).thenReturn(fileNode)
            whenever(mapNodeToPublicLinkUseCase(any(), anyOrNull())).thenReturn(publicNode)
            underTest.state.test {
                underTest.getPublicNode(url)
                underTest.handleSaveFile()
                val result = expectMostRecentItem()
                assertThat(result.downloadEvent).isInstanceOf(StateEventWithContentTriggered::class.java)
                assertThat((result.downloadEvent as StateEventWithContentTriggered).content.nodes)
                    .containsExactly(publicNode)
            }
        }

    private fun mockFileNode() =
        mock<TypedFileNode> {
            on { this.previewPath }.thenReturn(filePreviewPath)
            on { this.name }.thenReturn(title)
            on { this.size }.thenReturn(fileSize)
            on { this.serializedData }.thenReturn(serializedString)
            on { this.type }.thenReturn(
                StaticImageFileTypeInfo(
                    mimeType = "image/jpeg",
                    extension = "jpg"
                )
            )
            on { id }.thenReturn(NodeId(1234567890L))
        }

    @Test
    fun `test that getNodeContentUri returns as expected`() = runTest {
        val expectedNodeContentUri = mock<NodeContentUri.RemoteContentUri>()

        whenever(getFileLinkNodeContentUriUseCase(anyOrNull())).thenReturn(expectedNodeContentUri)
        assertThat(underTest.getNodeContentUri()).isEqualTo(expectedNodeContentUri)
    }

    @Test
    fun `test that downloadEvent is triggered when updateNodeToPreview is invoked`() =
        runTest {
            val node = mock<TypedFileNode>()
            val link = mock<PublicLinkFile>()
            whenever(mapNodeToPublicLinkUseCase(node, null)).thenReturn(link)
            whenever(getNodePreviewFileUseCase(node)).thenReturn(null)
            underTest.openOtherTypeFile(mock(), node) {}
            underTest.state.test {
                val res = awaitItem()
                assertThat(res.downloadEvent).isInstanceOf(StateEventWithContentTriggered::class.java)
                assertThat((res.downloadEvent as StateEventWithContentTriggered).content.nodes)
                    .containsExactly(link)
            }
        }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that show ads for link is set as expected`(shouldShowAdsForLink: Boolean) = runTest {
        val publicNode = mockFileNode()
        whenever(getPublicNodeUseCase(any())).thenReturn(publicNode)
        whenever(queryAdsUseCase(publicNode.id.longValue)).thenReturn(shouldShowAdsForLink)
        underTest.getPublicNode(url)

        underTest.state.test {
            val result = expectMostRecentItem()
            assertThat(result.shouldShowAdsForLink).isEqualTo(shouldShowAdsForLink)
        }
    }
}
