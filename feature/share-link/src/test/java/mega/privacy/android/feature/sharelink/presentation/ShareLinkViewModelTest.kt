package mega.privacy.android.feature.sharelink.presentation

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.MutableStateFlow
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
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.HasSensitiveDescendantUseCase
import mega.privacy.android.domain.usecase.HasSensitiveInheritedUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.link.SplitLinkAndKeyUseCase
import mega.privacy.android.domain.usecase.node.ExportNodesUseCase
import mega.privacy.android.feature.sharelink.session.LinkPassword
import mega.privacy.android.feature.sharelink.session.ShareLinkPasswordCache
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
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

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
    private val passwordCache = mock<ShareLinkPasswordCache>()
    private val separateKeyCache = mock<ShareLinkSeparateKeyCache>()

    @BeforeEach
    fun setUp() {
        whenever(monitorAccountDetailUseCase()).thenReturn(flowOf(AccountDetail()))
        whenever(splitLinkAndKeyUseCase(any())).thenReturn(LinkAndKey(null, null))
        whenever(fileTypeIconMapper(any(), any())).thenReturn(FILE_ICON_RES)
        whenever(passwordCache.monitor(any())).thenReturn(flowOf(null))
        whenever(separateKeyCache.monitor(any())).thenReturn(flowOf(false))
        whenever { hasSensitiveInheritedUseCase(any()) }.thenReturn(false)
        whenever { hasSensitiveDescendantUseCase(any()) }.thenReturn(false)
        underTest = buildViewModel(listOf(NODE_HANDLE))
    }

    private fun buildViewModel(handles: List<Long>) = ShareLinkViewModel(
        args = ShareLinkViewModel.Args(handles = handles),
        getNodeByIdUseCase = getNodeByIdUseCase,
        exportNodesUseCase = exportNodesUseCase,
        monitorAccountDetailUseCase = monitorAccountDetailUseCase,
        splitLinkAndKeyUseCase = splitLinkAndKeyUseCase,
        fileTypeIconMapper = fileTypeIconMapper,
        hasSensitiveInheritedUseCase = hasSensitiveInheritedUseCase,
        hasSensitiveDescendantUseCase = hasSensitiveDescendantUseCase,
        passwordCache = passwordCache,
        separateKeyCache = separateKeyCache,
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

    private suspend fun ReceiveTurbine<ShareLinkUiState>.awaitWarning(): ShareLinkUiState.SensitiveWarning {
        while (true) {
            val item = awaitItem()
            if (item is ShareLinkUiState.SensitiveWarning) return item
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

    private companion object {
        const val NODE_HANDLE = 123L
        const val SECOND_HANDLE = 456L
        const val CALLER_NAME = "ShareLinkViewModel"
        const val ENCRYPTED_LINK = "https://mega.nz/#P!enc"
        val FILE_ICON_RES = iconPackR.drawable.ic_pdf_medium_solid
    }
}
