package mega.privacy.android.feature.sharelink.presentation

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.account.AccountDetail
import mega.privacy.android.domain.entity.account.AccountLevelDetail
import mega.privacy.android.domain.entity.link.LinkAndKey
import mega.privacy.android.domain.entity.PdfFileTypeInfo
import mega.privacy.android.domain.entity.UnknownFileTypeInfo
import mega.privacy.android.domain.entity.node.ExportedData
import mega.privacy.android.domain.entity.node.NodeChanges
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeUpdate
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.media.MediaAlbum
import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.domain.entity.photos.AlbumLink
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.usecase.GetAlbumPhotosUseCase
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.HasSensitiveDescendantUseCase
import mega.privacy.android.domain.usecase.HasSensitiveInheritedUseCase
import mega.privacy.android.domain.usecase.SetShowCopyrightUseCase
import mega.privacy.android.domain.usecase.ShouldShowCopyrightUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.link.SplitLinkAndKeyUseCase
import mega.privacy.android.domain.usecase.node.ExportNodesUseCase
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.media.MonitorUserAlbumByIdUseCase
import mega.privacy.android.domain.usecase.photos.AlbumHasSensitiveContentUseCase
import mega.privacy.android.domain.usecase.photos.ExportAlbumsUseCase
import mega.privacy.android.domain.usecase.thumbnailpreview.DownloadThumbnailUseCase
import mega.privacy.android.feature.sharelink.session.LinkPassword
import mega.privacy.android.feature.sharelink.session.ShareLinkPasswordCache
import mega.privacy.android.feature.sharelink.session.ShareLinkPublicLinkCache
import mega.privacy.android.feature.sharelink.session.ShareLinkSeparateKeyCache
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.shared.nodes.mapper.FileTypeIconMapper
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import java.io.File
import java.time.LocalDateTime
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@ExtendWith(CoroutineMainDispatcherExtension::class)
class ShareLinkViewModelTest {

    private lateinit var underTest: ShareLinkViewModel

    private val getNodeByIdUseCase = mock<GetNodeByIdUseCase>()
    private val exportNodesUseCase = mock<ExportNodesUseCase>()
    private val monitorAccountDetailUseCase = mock<MonitorAccountDetailUseCase>()
    private val splitLinkAndKeyUseCase = mock<SplitLinkAndKeyUseCase>()
    private val fileTypeIconMapper = mock<FileTypeIconMapper>()
    private val hasSensitiveInheritedUseCase = mock<HasSensitiveInheritedUseCase>()
    private val hasSensitiveDescendantUseCase = mock<HasSensitiveDescendantUseCase>()
    private val shouldShowCopyrightUseCase = mock<ShouldShowCopyrightUseCase>()
    private val setShowCopyrightUseCase = mock<SetShowCopyrightUseCase>()
    private val passwordCache = mock<ShareLinkPasswordCache>()
    private val separateKeyCache = mock<ShareLinkSeparateKeyCache>()

    // Real instance: a plain in-memory map, so a mock would only restate what it already does.
    private val publicLinkCache = ShareLinkPublicLinkCache()
    private val monitorNodeUpdatesUseCase = mock<MonitorNodeUpdatesUseCase>()
    private val monitorUserAlbumByIdUseCase = mock<MonitorUserAlbumByIdUseCase>()
    private val getAlbumPhotosUseCase = mock<GetAlbumPhotosUseCase>()
    private val exportAlbumsUseCase = mock<ExportAlbumsUseCase>()
    private val albumHasSensitiveContentUseCase = mock<AlbumHasSensitiveContentUseCase>()
    private val downloadThumbnailUseCase = mock<DownloadThumbnailUseCase>()
    private val nodeUpdates = MutableSharedFlow<NodeUpdate>()

    @BeforeEach
    fun setUp() {
        whenever(monitorAccountDetailUseCase()).thenReturn(flowOf(AccountDetail()))
        whenever(splitLinkAndKeyUseCase(any())).thenReturn(LinkAndKey(null, null))
        whenever(fileTypeIconMapper(any(), any())).thenReturn(FILE_ICON_RES)
        whenever(passwordCache.monitor(any())).thenReturn(flowOf(null))
        whenever(separateKeyCache.monitor(any())).thenReturn(flowOf(false))
        whenever(monitorNodeUpdatesUseCase()).thenReturn(nodeUpdates)
        whenever { hasSensitiveInheritedUseCase(any()) }.thenReturn(false)
        whenever { hasSensitiveDescendantUseCase(any()) }.thenReturn(false)
        whenever { shouldShowCopyrightUseCase() }.thenReturn(false)
        underTest = buildViewModel(listOf(NODE_HANDLE))
    }

    private fun buildViewModel(handles: List<Long>) =
        buildViewModel(ShareLinkSubject.Nodes(handles))

    private fun buildViewModel(subject: ShareLinkSubject) = ShareLinkViewModel(
        args = ShareLinkViewModel.Args(subject = subject),
        getNodeByIdUseCase = getNodeByIdUseCase,
        exportNodesUseCase = exportNodesUseCase,
        monitorAccountDetailUseCase = monitorAccountDetailUseCase,
        splitLinkAndKeyUseCase = splitLinkAndKeyUseCase,
        fileTypeIconMapper = fileTypeIconMapper,
        hasSensitiveInheritedUseCase = hasSensitiveInheritedUseCase,
        hasSensitiveDescendantUseCase = hasSensitiveDescendantUseCase,
        shouldShowCopyrightUseCase = shouldShowCopyrightUseCase,
        setShowCopyrightUseCase = setShowCopyrightUseCase,
        monitorNodeUpdatesUseCase = monitorNodeUpdatesUseCase,
        monitorUserAlbumByIdUseCase = monitorUserAlbumByIdUseCase,
        getAlbumPhotosUseCase = getAlbumPhotosUseCase,
        exportAlbumsUseCase = exportAlbumsUseCase,
        albumHasSensitiveContentUseCase = albumHasSensitiveContentUseCase,
        downloadThumbnailUseCase = downloadThumbnailUseCase,
        passwordCache = passwordCache,
        separateKeyCache = separateKeyCache,
        publicLinkCache = publicLinkCache,
    )

    @AfterEach
    fun tearDown() {
        reset(
            getNodeByIdUseCase,
            exportNodesUseCase,
            monitorAccountDetailUseCase,
            splitLinkAndKeyUseCase,
            fileTypeIconMapper,
            hasSensitiveInheritedUseCase,
            hasSensitiveDescendantUseCase,
            shouldShowCopyrightUseCase,
            setShowCopyrightUseCase,
            monitorNodeUpdatesUseCase,
            monitorUserAlbumByIdUseCase,
            getAlbumPhotosUseCase,
            exportAlbumsUseCase,
            albumHasSensitiveContentUseCase,
            downloadThumbnailUseCase,
            passwordCache,
            separateKeyCache,
        )
    }

    @Test
    fun `test that uiState is Data with the existing public link and node details when the node is already exported`() =
        runTest {
            val node = mock<TypedFileNode> {
                on { id } doReturn NodeId(NODE_HANDLE)
                on { name } doReturn "report.pdf"
                on { exportedData } doReturn ExportedData("https://mega.nz/file/abc#key123", 0L)
                on { size } doReturn 2048L
                on { modificationTime } doReturn 1_718_000_000L
                on { type } doReturn PdfFileTypeInfo
            }
            whenever(getNodeByIdUseCase(NodeId(NODE_HANDLE))).thenReturn(node)
            whenever(splitLinkAndKeyUseCase("https://mega.nz/file/abc#key123"))
                .thenReturn(LinkAndKey("https://mega.nz/file/abc", "key123"))

            underTest.uiState.test {
                val node = awaitData().primary
                assertThat(node.name).isEqualTo("report.pdf")
                assertThat(node.isFolder).isFalse()
                assertThat(node.iconRes).isEqualTo(FILE_ICON_RES)
                assertThat(node.sizeInBytes).isEqualTo(2048L)
                assertThat(node.modificationTime).isEqualTo(1_718_000_000L)
                assertThat(node.link).isEqualTo("https://mega.nz/file/abc#key123")
                assertThat(node.linkWithoutKey).isEqualTo("https://mega.nz/file/abc")
                assertThat(node.key).isEqualTo("key123")
                cancelAndIgnoreRemainingEvents()
            }
            verifyNoInteractions(exportNodesUseCase)
        }

    @Test
    fun `test that uiState is Data with a link created via exportNodesUseCase when the node has no public link`() =
        runTest {
            val node = mock<TypedFileNode> {
                on { id } doReturn NodeId(NODE_HANDLE)
                on { name } doReturn "video.mp4"
                on { exportedData } doReturn null
                on { size } doReturn 10L
                on { modificationTime } doReturn 5L
                on { type } doReturn UnknownFileTypeInfo(mimeType = "video/mp4", extension = "mp4")
            }
            whenever(getNodeByIdUseCase(NodeId(NODE_HANDLE))).thenReturn(node)
            whenever(exportNodesUseCase(listOf(NODE_HANDLE), CALLER_NAME))
                .thenReturn(mapOf(NODE_HANDLE to "https://mega.nz/file/new#newkey"))
            whenever(splitLinkAndKeyUseCase("https://mega.nz/file/new#newkey"))
                .thenReturn(LinkAndKey("https://mega.nz/file/new", "newkey"))

            underTest.uiState.test {
                val node = awaitData().primary
                assertThat(node.link).isEqualTo("https://mega.nz/file/new#newkey")
                assertThat(node.linkWithoutKey).isEqualTo("https://mega.nz/file/new")
                assertThat(node.key).isEqualTo("newkey")
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that uiState Data has hasNewLinks true when the node had no link to begin with`() =
        runTest {
            val node = mock<TypedFileNode> {
                on { id } doReturn NodeId(NODE_HANDLE)
                on { name } doReturn "video.mp4"
                on { exportedData } doReturn null
                on { type } doReturn UnknownFileTypeInfo(mimeType = "video/mp4", extension = "mp4")
            }
            whenever(getNodeByIdUseCase(NodeId(NODE_HANDLE))).thenReturn(node)
            whenever(exportNodesUseCase(listOf(NODE_HANDLE), CALLER_NAME))
                .thenReturn(mapOf(NODE_HANDLE to "https://mega.nz/file/new#newkey"))
            whenever(splitLinkAndKeyUseCase("https://mega.nz/file/new#newkey"))
                .thenReturn(LinkAndKey("https://mega.nz/file/new", "newkey"))

            underTest.uiState.test {
                assertThat(awaitData().hasNewLinks).isTrue()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that uiState Data has hasNewLinks false when the node was already exported`() =
        runTest {
            val node = mock<TypedFileNode> {
                on { id } doReturn NodeId(NODE_HANDLE)
                on { name } doReturn "report.pdf"
                on { exportedData } doReturn ExportedData("https://mega.nz/file/abc#key123", 0L)
                on { type } doReturn PdfFileTypeInfo
            }
            whenever(getNodeByIdUseCase(NodeId(NODE_HANDLE))).thenReturn(node)
            whenever(splitLinkAndKeyUseCase("https://mega.nz/file/abc#key123"))
                .thenReturn(LinkAndKey("https://mega.nz/file/abc", "key123"))

            underTest.uiState.test {
                assertThat(awaitData().hasNewLinks).isFalse()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that uiState Data marks the node as a folder with no size when the node is a folder`() =
        runTest {
            val node = mock<TypedFolderNode> {
                on { id } doReturn NodeId(NODE_HANDLE)
                on { name } doReturn "Documents"
                on { exportedData } doReturn ExportedData("https://mega.nz/folder/fid#fkey", 0L)
            }
            whenever(getNodeByIdUseCase(NodeId(NODE_HANDLE))).thenReturn(node)
            whenever(splitLinkAndKeyUseCase("https://mega.nz/folder/fid#fkey"))
                .thenReturn(LinkAndKey("https://mega.nz/folder/fid", "fkey"))

            underTest.uiState.test {
                val node = awaitData().primary
                assertThat(node.isFolder).isTrue()
                assertThat(node.sizeInBytes).isNull()
                assertThat(node.modificationTime).isNull()
                assertThat(node.key).isEqualTo("fkey")
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that uiState Data carries the account type from monitorAccountDetailUseCase`() =
        runTest {
            val node = mock<TypedFileNode> {
                on { id } doReturn NodeId(NODE_HANDLE)
                on { name } doReturn "report.pdf"
                on { exportedData } doReturn ExportedData("https://mega.nz/file/abc#key123", 0L)
                on { type } doReturn PdfFileTypeInfo
            }
            whenever(getNodeByIdUseCase(NodeId(NODE_HANDLE))).thenReturn(node)
            val levelDetail = mock<AccountLevelDetail> {
                on { this.accountType } doReturn AccountType.PRO_I
            }
            whenever(monitorAccountDetailUseCase())
                .thenReturn(flowOf(AccountDetail(levelDetail = levelDetail)))

            underTest.uiState.test {
                val data = awaitData { it.accountType != null }
                assertThat(data.accountType).isEqualTo(AccountType.PRO_I)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that uiState is Error and does not export when the node is not found`() = runTest {
        whenever(getNodeByIdUseCase(NodeId(NODE_HANDLE))).thenReturn(null)

        underTest.uiState.test {
            var item = awaitItem()
            while (item !is ShareLinkUiState.Error) {
                item = awaitItem()
            }
            assertThat(item).isEqualTo(ShareLinkUiState.Error)
            cancelAndIgnoreRemainingEvents()
        }
        verifyNoInteractions(exportNodesUseCase)
    }

    @Test
    fun `test that a cached password marks the link as password protected`() =
        runTest {
            val node = mock<TypedFileNode> {
                on { id } doReturn NodeId(NODE_HANDLE)
                on { name } doReturn "report.pdf"
                on { exportedData } doReturn ExportedData("https://mega.nz/file/abc#key123", 0L)
                on { type } doReturn PdfFileTypeInfo
            }
            whenever(getNodeByIdUseCase(NodeId(NODE_HANDLE))).thenReturn(node)
            whenever(passwordCache.monitor(NODE_HANDLE))
                .thenReturn(flowOf(LinkPassword("Str0ngP@ss", ENCRYPTED_LINK)))

            underTest.uiState.test {
                val data = awaitData { it.isPasswordSet }
                assertThat(data.linkWithPassword).isEqualTo(ENCRYPTED_LINK)
                assertThat(data.password).isEqualTo("Str0ngP@ss")
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that a cached separate-key preference marks the link and key as separate`() =
        runTest {
            val node = mock<TypedFileNode> {
                on { id } doReturn NodeId(NODE_HANDLE)
                on { name } doReturn "report.pdf"
                on { exportedData } doReturn ExportedData("https://mega.nz/file/abc#key123", 0L)
                on { type } doReturn PdfFileTypeInfo
            }
            whenever(getNodeByIdUseCase(NodeId(NODE_HANDLE))).thenReturn(node)
            whenever(splitLinkAndKeyUseCase("https://mega.nz/file/abc#key123"))
                .thenReturn(LinkAndKey("https://mega.nz/file/abc", "key123"))
            whenever(separateKeyCache.monitor(NODE_HANDLE)).thenReturn(flowOf(true))

            underTest.uiState.test {
                val node = awaitData { it.isKeySeparate }.primary
                assertThat(node.linkWithoutKey).isEqualTo("https://mega.nz/file/abc")
                assertThat(node.key).isEqualTo("key123")
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that the link and key are not separate by default`() =
        runTest {
            val node = mock<TypedFileNode> {
                on { id } doReturn NodeId(NODE_HANDLE)
                on { name } doReturn "report.pdf"
                on { exportedData } doReturn ExportedData("https://mega.nz/file/abc#key123", 0L)
                on { type } doReturn PdfFileTypeInfo
            }
            whenever(getNodeByIdUseCase(NodeId(NODE_HANDLE))).thenReturn(node)
            whenever(splitLinkAndKeyUseCase("https://mega.nz/file/abc#key123"))
                .thenReturn(LinkAndKey("https://mega.nz/file/abc", "key123"))

            underTest.uiState.test {
                assertThat(awaitData().isKeySeparate).isFalse()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that enabling the separate-key preference mid-session marks the link and key as separate`() =
        runTest {
            val node = mock<TypedFileNode> {
                on { id } doReturn NodeId(NODE_HANDLE)
                on { name } doReturn "report.pdf"
                on { exportedData } doReturn ExportedData("https://mega.nz/file/abc#key123", 0L)
                on { type } doReturn PdfFileTypeInfo
            }
            whenever(getNodeByIdUseCase(NodeId(NODE_HANDLE))).thenReturn(node)
            whenever(splitLinkAndKeyUseCase("https://mega.nz/file/abc#key123"))
                .thenReturn(LinkAndKey("https://mega.nz/file/abc", "key123"))
            val separateKeyFlow = MutableStateFlow(false)
            whenever(separateKeyCache.monitor(NODE_HANDLE)).thenReturn(separateKeyFlow)

            underTest.uiState.test {
                assertThat(awaitData().isKeySeparate).isFalse()

                separateKeyFlow.value = true

                val node = awaitData { it.isKeySeparate }.primary
                assertThat(node.linkWithoutKey).isEqualTo("https://mega.nz/file/abc")
                assertThat(node.key).isEqualTo("key123")
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that uiState is Data with one nodeLink per shared handle in order`() = runTest {
        val folder = mock<TypedFolderNode> {
            on { id } doReturn NodeId(NODE_HANDLE)
            on { name } doReturn "Documents"
            on { exportedData } doReturn ExportedData("https://mega.nz/folder/fid#fkey", 0L)
            on { childFolderCount } doReturn 6
            on { childFileCount } doReturn 12
        }
        val file = mock<TypedFileNode> {
            on { id } doReturn NodeId(SECOND_HANDLE)
            on { name } doReturn "report.pdf"
            on { exportedData } doReturn ExportedData("https://mega.nz/file/abc#key123", 0L)
            on { type } doReturn PdfFileTypeInfo
        }
        whenever(getNodeByIdUseCase(NodeId(NODE_HANDLE))).thenReturn(folder)
        whenever(getNodeByIdUseCase(NodeId(SECOND_HANDLE))).thenReturn(file)

        val underTest = buildViewModel(listOf(NODE_HANDLE, SECOND_HANDLE))
        underTest.uiState.test {
            val data = awaitData()
            assertThat(data.isMultiNode).isTrue()
            assertThat(data.nodeLinks.map { it.name })
                .containsExactly("Documents", "report.pdf").inOrder()
            assertThat(data.nodeLinks.map { it.link }).containsExactly(
                "https://mega.nz/folder/fid#fkey",
                "https://mega.nz/file/abc#key123",
            ).inOrder()
            assertThat(data.nodeLinks[0].childFolderCount).isEqualTo(6)
            assertThat(data.nodeLinks[0].childFileCount).isEqualTo(12)
            cancelAndIgnoreRemainingEvents()
        }
        verifyNoInteractions(exportNodesUseCase)
    }

    @Test
    fun `test that only nodes without a public link are batch exported`() = runTest {
        val exported = mock<TypedFileNode> {
            on { id } doReturn NodeId(NODE_HANDLE)
            on { name } doReturn "a.pdf"
            on { exportedData } doReturn ExportedData("https://mega.nz/file/exists#k", 0L)
            on { type } doReturn PdfFileTypeInfo
        }
        val pending = mock<TypedFileNode> {
            on { id } doReturn NodeId(SECOND_HANDLE)
            on { name } doReturn "b.pdf"
            on { exportedData } doReturn null
            on { type } doReturn PdfFileTypeInfo
        }
        whenever(getNodeByIdUseCase(NodeId(NODE_HANDLE))).thenReturn(exported)
        whenever(getNodeByIdUseCase(NodeId(SECOND_HANDLE))).thenReturn(pending)
        whenever(exportNodesUseCase(listOf(SECOND_HANDLE), CALLER_NAME))
            .thenReturn(mapOf(SECOND_HANDLE to "https://mega.nz/file/new#nk"))

        val underTest = buildViewModel(listOf(NODE_HANDLE, SECOND_HANDLE))
        underTest.uiState.test {
            val data = awaitData()
            assertThat(data.nodeLinks.map { it.link }).containsExactly(
                "https://mega.nz/file/exists#k",
                "https://mega.nz/file/new#nk",
            ).inOrder()
            cancelAndIgnoreRemainingEvents()
        }
        verify(exportNodesUseCase).invoke(listOf(SECOND_HANDLE), CALLER_NAME)
    }

    @Test
    fun `test that a node whose link cannot be resolved is dropped from nodeLinks`() = runTest {
        val exported = mock<TypedFileNode> {
            on { id } doReturn NodeId(NODE_HANDLE)
            on { name } doReturn "a.pdf"
            on { exportedData } doReturn ExportedData("https://mega.nz/file/exists#k", 0L)
            on { type } doReturn PdfFileTypeInfo
        }
        val pending = mock<TypedFileNode> {
            on { id } doReturn NodeId(SECOND_HANDLE)
            on { name } doReturn "b.pdf"
            on { exportedData } doReturn null
            on { type } doReturn PdfFileTypeInfo
        }
        whenever(getNodeByIdUseCase(NodeId(NODE_HANDLE))).thenReturn(exported)
        whenever(getNodeByIdUseCase(NodeId(SECOND_HANDLE))).thenReturn(pending)
        whenever(exportNodesUseCase(listOf(SECOND_HANDLE), CALLER_NAME)).thenReturn(emptyMap())

        val underTest = buildViewModel(listOf(NODE_HANDLE, SECOND_HANDLE))
        underTest.uiState.test {
            val data = awaitData()
            assertThat(data.nodeLinks.map { it.name }).containsExactly("a.pdf")
            assertThat(data.isMultiNode).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that a sensitive node holds the export behind a warning until confirmed`() = runTest {
        val node = mock<TypedFileNode> {
            on { id } doReturn NodeId(NODE_HANDLE)
            on { name } doReturn "secret.pdf"
            on { exportedData } doReturn null
            on { isMarkedSensitive } doReturn true
            on { type } doReturn PdfFileTypeInfo
        }
        whenever(getNodeByIdUseCase(NodeId(NODE_HANDLE))).thenReturn(node)
        whenever(exportNodesUseCase(listOf(NODE_HANDLE), CALLER_NAME))
            .thenReturn(mapOf(NODE_HANDLE to "https://mega.nz/file/new#k"))

        underTest.uiState.test {
            val warning = awaitWarning()
            assertThat(warning.type).isEqualTo(SensitiveWarningType.Items)
            verify(exportNodesUseCase, never()).invoke(any(), any())

            underTest.onSensitiveWarningConfirmed()

            assertThat(awaitData()).isInstanceOf(ShareLinkUiState.Data::class.java)
            cancelAndIgnoreRemainingEvents()
        }
        verify(exportNodesUseCase).invoke(listOf(NODE_HANDLE), CALLER_NAME)
    }

    @Test
    fun `test that an inherited-sensitive node triggers the Items warning`() = runTest {
        val node = mock<TypedFileNode> {
            on { id } doReturn NodeId(NODE_HANDLE)
            on { name } doReturn "inherited.pdf"
            on { exportedData } doReturn null
            on { isMarkedSensitive } doReturn false
            on { type } doReturn PdfFileTypeInfo
        }
        whenever(getNodeByIdUseCase(NodeId(NODE_HANDLE))).thenReturn(node)
        whenever(hasSensitiveInheritedUseCase(NodeId(NODE_HANDLE))).thenReturn(true)

        underTest.uiState.test {
            assertThat(awaitWarning().type).isEqualTo(SensitiveWarningType.Items)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that a folder with sensitive descendants triggers the Folder warning`() = runTest {
        val folder = mock<TypedFolderNode> {
            on { id } doReturn NodeId(NODE_HANDLE)
            on { name } doReturn "Documents"
            on { exportedData } doReturn null
            on { isMarkedSensitive } doReturn false
        }
        whenever(getNodeByIdUseCase(NodeId(NODE_HANDLE))).thenReturn(folder)
        whenever(hasSensitiveDescendantUseCase(NodeId(NODE_HANDLE))).thenReturn(true)

        underTest.uiState.test {
            val warning = awaitWarning()
            assertThat(warning.type).isEqualTo(SensitiveWarningType.Folder)
            assertThat(warning.nodeCount).isEqualTo(1)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that dismissing the warning abandons the export`() = runTest {
        val node = mock<TypedFileNode> {
            on { id } doReturn NodeId(NODE_HANDLE)
            on { name } doReturn "secret.pdf"
            on { exportedData } doReturn null
            on { isMarkedSensitive } doReturn true
            on { type } doReturn PdfFileTypeInfo
        }
        whenever(getNodeByIdUseCase(NodeId(NODE_HANDLE))).thenReturn(node)

        underTest.uiState.test {
            awaitWarning()

            underTest.onSensitiveWarningDismissed()

            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
        verify(exportNodesUseCase, never()).invoke(any(), any())
    }

    @Test
    fun `test that a non-sensitive selection exports without a warning`() = runTest {
        val node = mock<TypedFileNode> {
            on { id } doReturn NodeId(NODE_HANDLE)
            on { name } doReturn "public.pdf"
            on { exportedData } doReturn null
            on { isMarkedSensitive } doReturn false
            on { type } doReturn PdfFileTypeInfo
        }
        whenever(getNodeByIdUseCase(NodeId(NODE_HANDLE))).thenReturn(node)
        whenever(exportNodesUseCase(listOf(NODE_HANDLE), CALLER_NAME))
            .thenReturn(mapOf(NODE_HANDLE to "https://mega.nz/file/new#k"))

        underTest.uiState.test {
            awaitData()
            cancelAndIgnoreRemainingEvents()
        }
        verify(exportNodesUseCase).invoke(listOf(NODE_HANDLE), CALLER_NAME)
    }

    @Test
    fun `test that an already-exported sensitive node does not trigger a warning`() = runTest {
        val node = mock<TypedFileNode> {
            on { id } doReturn NodeId(NODE_HANDLE)
            on { name } doReturn "shared.pdf"
            on { exportedData } doReturn ExportedData("https://mega.nz/file/abc#key123", 0L)
            on { isMarkedSensitive } doReturn true
            on { type } doReturn PdfFileTypeInfo
        }
        whenever(getNodeByIdUseCase(NodeId(NODE_HANDLE))).thenReturn(node)

        underTest.uiState.test {
            assertThat(awaitData().primary.link).isEqualTo("https://mega.nz/file/abc#key123")
            cancelAndIgnoreRemainingEvents()
        }
        verify(exportNodesUseCase, never()).invoke(any(), any())
    }

    @Test
    fun `test that copyright consent is shown before any export when consent is due`() = runTest {
        val node = mock<TypedFileNode> {
            on { id } doReturn NodeId(NODE_HANDLE)
            on { name } doReturn "new.pdf"
            on { exportedData } doReturn null
            on { type } doReturn PdfFileTypeInfo
        }
        whenever(getNodeByIdUseCase(NodeId(NODE_HANDLE))).thenReturn(node)
        whenever { shouldShowCopyrightUseCase() }.thenReturn(true)

        underTest.uiState.test {
            assertThat(awaitCopyright()).isEqualTo(ShareLinkUiState.CopyrightConsent)
            verify(exportNodesUseCase, never()).invoke(any(), any())
            cancelAndIgnoreRemainingEvents()
        }
        verify(setShowCopyrightUseCase, never()).invoke(any())
    }

    @Test
    fun `test that agreeing to copyright persists acceptance and proceeds to Data`() = runTest {
        val node = mock<TypedFileNode> {
            on { id } doReturn NodeId(NODE_HANDLE)
            on { name } doReturn "new.pdf"
            on { exportedData } doReturn null
            on { type } doReturn PdfFileTypeInfo
        }
        whenever(getNodeByIdUseCase(NodeId(NODE_HANDLE))).thenReturn(node)
        whenever { shouldShowCopyrightUseCase() }.thenReturn(true)
        whenever(exportNodesUseCase(listOf(NODE_HANDLE), CALLER_NAME))
            .thenReturn(mapOf(NODE_HANDLE to "https://mega.nz/file/new#k"))

        underTest.uiState.test {
            awaitCopyright()

            underTest.onCopyrightAgreed()

            assertThat(awaitData()).isInstanceOf(ShareLinkUiState.Data::class.java)
            cancelAndIgnoreRemainingEvents()
        }
        verify(setShowCopyrightUseCase).invoke(false)
        verify(exportNodesUseCase).invoke(listOf(NODE_HANDLE), CALLER_NAME)
    }

    @Test
    fun `test that declining copyright abandons the load and does not export`() = runTest {
        val node = mock<TypedFileNode> {
            on { id } doReturn NodeId(NODE_HANDLE)
            on { name } doReturn "new.pdf"
            on { exportedData } doReturn null
            on { type } doReturn PdfFileTypeInfo
        }
        whenever(getNodeByIdUseCase(NodeId(NODE_HANDLE))).thenReturn(node)
        whenever { shouldShowCopyrightUseCase() }.thenReturn(true)

        underTest.uiState.test {
            awaitCopyright()

            underTest.onCopyrightDisagreed()

            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
        verify(exportNodesUseCase, never()).invoke(any(), any())
        verify(setShowCopyrightUseCase, never()).invoke(any())
    }

    @Test
    fun `test that copyright consent is not shown when consent is not due`() = runTest {
        val node = mock<TypedFileNode> {
            on { id } doReturn NodeId(NODE_HANDLE)
            on { name } doReturn "report.pdf"
            on { exportedData } doReturn ExportedData("https://mega.nz/file/abc#key123", 0L)
            on { type } doReturn PdfFileTypeInfo
        }
        whenever(getNodeByIdUseCase(NodeId(NODE_HANDLE))).thenReturn(node)

        underTest.uiState.test {
            awaitData()
            cancelAndIgnoreRemainingEvents()
        }
        verify(setShowCopyrightUseCase, never()).invoke(any())
    }

    private suspend fun ReceiveTurbine<ShareLinkUiState>.awaitCopyright(): ShareLinkUiState {
        while (true) {
            val item = awaitItem()
            if (item is ShareLinkUiState.CopyrightConsent) return item
        }
    }

    private suspend fun ReceiveTurbine<ShareLinkUiState>.awaitWarning(): ShareLinkUiState.SensitiveWarning {
        while (true) {
            val item = awaitItem()
            if (item is ShareLinkUiState.SensitiveWarning) return item
        }
    }

    private suspend fun ReceiveTurbine<ShareLinkUiState>.awaitErrorState(): ShareLinkUiState {
        while (true) {
            val item = awaitItem()
            if (item is ShareLinkUiState.Error) return item
        }
    }

    private suspend fun ReceiveTurbine<ShareLinkUiState>.awaitData(
        predicate: (ShareLinkUiState.Data) -> Boolean = { true },
    ): ShareLinkUiState.Data {
        while (true) {
            val item = awaitItem()
            if (item is ShareLinkUiState.Data && predicate(item)) return item
        }
    }

    private suspend fun stubExportedNode(expirationSeconds: Long?) {
        val node = mock<TypedFileNode> {
            on { id } doReturn NodeId(NODE_HANDLE)
            on { name } doReturn "report.pdf"
            on { exportedData } doReturn ExportedData(LINK, 0L, expirationSeconds)
            on { type } doReturn PdfFileTypeInfo
        }
        whenever(getNodeByIdUseCase(NodeId(NODE_HANDLE))).thenReturn(node)
        whenever(splitLinkAndKeyUseCase(LINK)).thenReturn(LinkAndKey(LINK_WITHOUT_KEY, "key123"))
    }

    @Test
    fun `test that expirationTime is null and isExpired is false when the link never expires`() =
        runTest {
            stubExportedNode(expirationSeconds = null)

            underTest.uiState.test {
                val node = awaitData().primary
                assertThat(node.expirationTime).isNull()
                assertThat(node.isExpired).isFalse()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that expirationTime is null when the exported data reports a zero expiry`() =
        runTest {
            stubExportedNode(expirationSeconds = 0L)

            underTest.uiState.test {
                val node = awaitData().primary
                assertThat(node.expirationTime).isNull()
                assertThat(node.isExpired).isFalse()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that expirationTime is exposed in milliseconds and isExpired is false for a future expiry`() =
        runTest {
            val futureSeconds = (System.currentTimeMillis().milliseconds + 30.days).inWholeSeconds
            stubExportedNode(expirationSeconds = futureSeconds)

            underTest.uiState.test {
                val node = awaitData().primary
                assertThat(node.expirationTime)
                    .isEqualTo(futureSeconds.seconds.inWholeMilliseconds)
                assertThat(node.isExpired).isFalse()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that isExpired is true for an expiry in the past`() =
        runTest {
            val pastSeconds = (System.currentTimeMillis().milliseconds - 1.days).inWholeSeconds
            stubExportedNode(expirationSeconds = pastSeconds)

            underTest.uiState.test {
                val node = awaitData().primary
                assertThat(node.expirationTime).isEqualTo(pastSeconds.seconds.inWholeMilliseconds)
                assertThat(node.isExpired).isTrue()
                cancelAndIgnoreRemainingEvents()
            }
        }

    /** Emits a node update naming [NODE_HANDLE], as the SDK does after the link is re-exported. */
    private suspend fun emitNodeUpdate() {
        val changed = mock<TypedFileNode> { on { id } doReturn NodeId(NODE_HANDLE) }
        nodeUpdates.emit(NodeUpdate(mapOf(changed to listOf(NodeChanges.Public_link))))
    }

    @Test
    fun `test that a node update clears the expiry when it has been removed`() =
        runTest {
            val futureSeconds = (System.currentTimeMillis().milliseconds + 30.days).inWholeSeconds
            stubExportedNode(expirationSeconds = futureSeconds)

            underTest.uiState.test {
                assertThat(awaitData().primary.expirationTime)
                    .isEqualTo(futureSeconds.seconds.inWholeMilliseconds)

                stubExportedNode(expirationSeconds = null)
                emitNodeUpdate()

                assertThat(awaitData().primary.expirationTime).isNull()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that a node update picks up a newly set expiry`() =
        runTest {
            stubExportedNode(expirationSeconds = null)

            underTest.uiState.test {
                assertThat(awaitData().primary.expirationTime).isNull()

                val futureSeconds =
                    (System.currentTimeMillis().milliseconds + 30.days).inWholeSeconds
                stubExportedNode(expirationSeconds = futureSeconds)
                emitNodeUpdate()

                assertThat(awaitData().primary.expirationTime)
                    .isEqualTo(futureSeconds.seconds.inWholeMilliseconds)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that a node update refreshes the file size and modification time`() = runTest {
        val node = mock<TypedFileNode> {
            on { id } doReturn NodeId(NODE_HANDLE)
            on { name } doReturn "report.pdf"
            on { exportedData } doReturn ExportedData(LINK, 0L)
            on { size } doReturn 100L
            on { modificationTime } doReturn 1_000L
            on { type } doReturn PdfFileTypeInfo
        }
        whenever(getNodeByIdUseCase(NodeId(NODE_HANDLE))).thenReturn(node)
        whenever(splitLinkAndKeyUseCase(LINK)).thenReturn(LinkAndKey(LINK_WITHOUT_KEY, "key123"))

        underTest.uiState.test {
            val initial = awaitData().primary
            assertThat(initial.sizeInBytes).isEqualTo(100L)
            assertThat(initial.modificationTime).isEqualTo(1_000L)

            val updatedNode = mock<TypedFileNode> {
                on { id } doReturn NodeId(NODE_HANDLE)
                on { name } doReturn "report.pdf"
                on { exportedData } doReturn ExportedData(LINK, 0L)
                on { size } doReturn 200L
                on { modificationTime } doReturn 2_000L
                on { type } doReturn PdfFileTypeInfo
            }
            whenever(getNodeByIdUseCase(NodeId(NODE_HANDLE))).thenReturn(updatedNode)
            emitNodeUpdate()

            val refreshed = awaitData().primary
            assertThat(refreshed.sizeInBytes).isEqualTo(200L)
            assertThat(refreshed.modificationTime).isEqualTo(2_000L)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that a node update refreshes a folder's child counts`() = runTest {
        val folder = mock<TypedFolderNode> {
            on { id } doReturn NodeId(NODE_HANDLE)
            on { name } doReturn "Documents"
            on { exportedData } doReturn ExportedData(LINK, 0L)
            on { childFolderCount } doReturn 2
            on { childFileCount } doReturn 4
        }
        whenever(getNodeByIdUseCase(NodeId(NODE_HANDLE))).thenReturn(folder)
        whenever(splitLinkAndKeyUseCase(LINK)).thenReturn(LinkAndKey(LINK_WITHOUT_KEY, "key123"))

        underTest.uiState.test {
            val initial = awaitData().primary
            assertThat(initial.childFolderCount).isEqualTo(2)
            assertThat(initial.childFileCount).isEqualTo(4)

            val updatedFolder = mock<TypedFolderNode> {
                on { id } doReturn NodeId(NODE_HANDLE)
                on { name } doReturn "Documents"
                on { exportedData } doReturn ExportedData(LINK, 0L)
                on { childFolderCount } doReturn 5
                on { childFileCount } doReturn 9
            }
            whenever(getNodeByIdUseCase(NodeId(NODE_HANDLE))).thenReturn(updatedFolder)
            emitNodeUpdate()

            val refreshed = awaitData().primary
            assertThat(refreshed.childFolderCount).isEqualTo(5)
            assertThat(refreshed.childFileCount).isEqualTo(9)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that a node update for an unrelated node does not re-read the node`() =
        runTest {
            stubExportedNode(expirationSeconds = null)

            underTest.uiState.test {
                awaitData()
                val unrelated = mock<TypedFileNode> { on { id } doReturn NodeId(SECOND_HANDLE) }
                nodeUpdates.emit(NodeUpdate(mapOf(unrelated to listOf(NodeChanges.Public_link))))
                expectNoEvents()
                cancelAndIgnoreRemainingEvents()
            }

            verify(getNodeByIdUseCase, times(1)).invoke(NodeId(NODE_HANDLE))
        }

    @Test
    fun `test that the copyright consent is not shown again when a node update arrives`() =
        runTest {
            stubExportedNode(expirationSeconds = null)

            underTest.uiState.test {
                awaitData()
                emitNodeUpdate()
                expectNoEvents()
                cancelAndIgnoreRemainingEvents()
            }

            verify(shouldShowCopyrightUseCase, times(1)).invoke()
        }

    private fun stubAlbum(
        title: String = ALBUM_TITLE,
        cover: TypedFileNode? = null,
        photos: List<Photo> = emptyList(),
        link: String = ALBUM_LINK,
    ) {
        whenever(monitorUserAlbumByIdUseCase(AlbumId(ALBUM_ID)))
            .thenReturn(flowOf(userAlbum(title = title, cover = cover)))
        whenever(getAlbumPhotosUseCase(AlbumId(ALBUM_ID), false)).thenReturn(flowOf(photos))
        whenever { albumHasSensitiveContentUseCase(AlbumId(ALBUM_ID)) }.thenReturn(false)
        whenever { exportAlbumsUseCase(listOf(AlbumId(ALBUM_ID))) }
            .thenReturn(listOf(AlbumId(ALBUM_ID) to AlbumLink(link)))
        whenever(splitLinkAndKeyUseCase(link))
            .thenReturn(LinkAndKey(ALBUM_LINK_WITHOUT_KEY, ALBUM_KEY))
    }

    private fun userAlbum(title: String = ALBUM_TITLE, cover: TypedFileNode? = null) =
        MediaAlbum.User(
            id = AlbumId(ALBUM_ID),
            title = title,
            creationTime = 0L,
            modificationTime = 0L,
            isExported = true,
            cover = cover,
        )

    private fun photo(id: Long, thumbnailFilePath: String?, modifiedYear: Int) = Photo.Image(
        id = id,
        parentId = 0L,
        name = "photo-$id.jpg",
        isFavourite = false,
        creationTime = LocalDateTime.of(modifiedYear, 1, 1, 0, 0),
        modificationTime = LocalDateTime.of(modifiedYear, 1, 1, 0, 0),
        thumbnailFilePath = thumbnailFilePath,
        previewFilePath = null,
        fileTypeInfo = UnknownFileTypeInfo(mimeType = "image/jpeg", extension = "jpg"),
    )

    private fun coverNode(id: Long, thumbnailPath: String?) = mock<TypedFileNode> {
        on { this.id } doReturn NodeId(id)
        on { this.thumbnailPath } doReturn thumbnailPath
    }

    private fun buildAlbumViewModel() = buildViewModel(ShareLinkSubject.Album(ALBUM_ID))

    @Test
    fun `test that uiState is Data with the album title cover and photo count when an album is shared`() =
        runTest {
            val thumbnail = File.createTempFile("album-cover", ".jpg").apply { deleteOnExit() }
            val cover = coverNode(id = 1L, thumbnailPath = thumbnail.absolutePath)
            stubAlbum(cover = cover, photos = listOf(photo(1L, null, 2024), photo(2L, null, 2023)))

            buildAlbumViewModel().uiState.test {
                val state = awaitData()
                assertThat(state.primary.name).isEqualTo(ALBUM_TITLE)
                assertThat(state.primary.handle).isEqualTo(ALBUM_ID)
                assertThat(state.isAlbum).isTrue()
                assertThat(state.album?.photoCount).isEqualTo(2)
                assertThat(state.album?.coverThumbnailPath).isEqualTo(thumbnail.absolutePath)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that the album link is split into the link without key and the key`() = runTest {
        stubAlbum()

        buildAlbumViewModel().uiState.test {
            val item = awaitData().primary
            assertThat(item.link).isEqualTo(ALBUM_LINK)
            assertThat(item.linkWithoutKey).isEqualTo(ALBUM_LINK_WITHOUT_KEY)
            assertThat(item.key).isEqualTo(ALBUM_KEY)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that an album carries no expiry and no icon`() = runTest {
        stubAlbum()

        buildAlbumViewModel().uiState.test {
            val item = awaitData().primary
            assertThat(item.expirationTime).isNull()
            assertThat(item.isExpired).isFalse()
            assertThat(item.iconRes).isNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that the cover thumbnail is downloaded when it is not cached`() = runTest {
        val missing = File.createTempFile("album-missing", ".jpg").apply { delete() }
        stubAlbum(cover = coverNode(id = 7L, thumbnailPath = missing.absolutePath))

        buildAlbumViewModel().uiState.test {
            assertThat(awaitData().album?.coverThumbnailPath).isNull()
            cancelAndIgnoreRemainingEvents()
        }

        verify(downloadThumbnailUseCase).invoke(7L)
    }

    @Test
    fun `test that uiState is Error when the album export returns no link`() = runTest {
        stubAlbum()
        whenever { exportAlbumsUseCase(listOf(AlbumId(ALBUM_ID))) }.thenReturn(emptyList())

        buildAlbumViewModel().uiState.test {
            awaitErrorState()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that uiState is Error when the album export fails`() = runTest {
        stubAlbum()
        whenever { exportAlbumsUseCase(listOf(AlbumId(ALBUM_ID))) }
            .thenThrow(RuntimeException("boom"))

        buildAlbumViewModel().uiState.test {
            awaitErrorState()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that the copyright consent gates the album export and is asked once`() = runTest {
        stubAlbum()
        whenever { shouldShowCopyrightUseCase() }.thenReturn(true)

        val underTest = buildAlbumViewModel()
        underTest.uiState.test {
            awaitCopyright()
            verifyNoInteractions(exportAlbumsUseCase)

            underTest.onCopyrightAgreed()
            assertThat(awaitData().primary.link).isEqualTo(ALBUM_LINK)
            cancelAndIgnoreRemainingEvents()
        }

        verify(shouldShowCopyrightUseCase, times(1)).invoke()
        verify(setShowCopyrightUseCase).invoke(false)
    }

    @Test
    fun `test that declining the copyright consent abandons the album export`() = runTest {
        stubAlbum()
        whenever { shouldShowCopyrightUseCase() }.thenReturn(true)

        val underTest = buildAlbumViewModel()
        underTest.uiState.test {
            awaitCopyright()
            underTest.onCopyrightDisagreed()
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }

        verifyNoInteractions(exportAlbumsUseCase)
    }

    @Test
    fun `test that the hidden items warning gates the album export`() = runTest {
        stubAlbum()
        whenever { albumHasSensitiveContentUseCase(AlbumId(ALBUM_ID)) }.thenReturn(true)

        val underTest = buildAlbumViewModel()
        underTest.uiState.test {
            val warning = awaitWarning()
            assertThat(warning.type).isEqualTo(SensitiveWarningType.Items)
            assertThat(warning.nodeCount).isEqualTo(1)
            verifyNoInteractions(exportAlbumsUseCase)

            underTest.onSensitiveWarningConfirmed()
            assertThat(awaitData().primary.link).isEqualTo(ALBUM_LINK)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that dismissing the hidden items warning abandons the album export`() = runTest {
        stubAlbum()
        whenever { albumHasSensitiveContentUseCase(AlbumId(ALBUM_ID)) }.thenReturn(true)

        val underTest = buildAlbumViewModel()
        underTest.uiState.test {
            awaitWarning()
            underTest.onSensitiveWarningDismissed()
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }

        verifyNoInteractions(exportAlbumsUseCase)
    }

    @Test
    fun `test that the album separate key option is read from the separate key cache`() = runTest {
        stubAlbum()
        whenever(separateKeyCache.monitor(ALBUM_ID)).thenReturn(flowOf(true))

        buildAlbumViewModel().uiState.test {
            assertThat(awaitData { it.isKeySeparate }.isKeySeparate).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that an album never reads the password cache`() = runTest {
        stubAlbum()

        buildAlbumViewModel().uiState.test {
            assertThat(awaitData().isPasswordSet).isFalse()
            cancelAndIgnoreRemainingEvents()
        }

        verifyNoInteractions(passwordCache)
    }

    @Test
    fun `test that the album title and photo count follow later album updates`() = runTest {
        val albums = MutableStateFlow(userAlbum())
        val photos = MutableStateFlow(emptyList<Photo>())
        whenever(monitorUserAlbumByIdUseCase(AlbumId(ALBUM_ID))).thenReturn(albums)
        whenever(getAlbumPhotosUseCase(AlbumId(ALBUM_ID), false)).thenReturn(photos)
        whenever { albumHasSensitiveContentUseCase(AlbumId(ALBUM_ID)) }.thenReturn(false)
        whenever { exportAlbumsUseCase(listOf(AlbumId(ALBUM_ID))) }
            .thenReturn(listOf(AlbumId(ALBUM_ID) to AlbumLink(ALBUM_LINK)))
        whenever(splitLinkAndKeyUseCase(ALBUM_LINK))
            .thenReturn(LinkAndKey(ALBUM_LINK_WITHOUT_KEY, ALBUM_KEY))

        buildAlbumViewModel().uiState.test {
            assertThat(awaitData().album?.photoCount).isEqualTo(0)

            albums.value = albums.value.copy(title = "Renamed")
            photos.value = listOf(photo(1L, null, 2024))

            val renamed = awaitData { it.primary.name == "Renamed" && it.album?.photoCount == 1 }
            assertThat(renamed.primary.link).isEqualTo(ALBUM_LINK)
            cancelAndIgnoreRemainingEvents()
        }

        verify(exportAlbumsUseCase, times(1)).invoke(listOf(AlbumId(ALBUM_ID)))
    }

    private companion object {
        const val ALBUM_ID = 987L
        const val ALBUM_TITLE = "Holiday"
        const val ALBUM_LINK = "https://mega.nz/collection/xyz#albumkey"
        const val ALBUM_LINK_WITHOUT_KEY = "https://mega.nz/collection/xyz"
        const val ALBUM_KEY = "albumkey"
        const val LINK = "https://mega.nz/file/abc#key123"
        const val LINK_WITHOUT_KEY = "https://mega.nz/file/abc"
        const val NODE_HANDLE = 123L
        const val SECOND_HANDLE = 456L
        const val CALLER_NAME = "ShareLinkViewModel"
        const val ENCRYPTED_LINK = "https://mega.nz/#P!enc"
        val FILE_ICON_RES = iconPackR.drawable.ic_pdf_medium_solid
    }
}
