package mega.privacy.android.feature.sharelink.presentation

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
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
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.link.SplitLinkAndKeyUseCase
import mega.privacy.android.domain.usecase.node.ExportNodeUseCase
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
import org.mockito.kotlin.reset
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@ExtendWith(CoroutineMainDispatcherExtension::class)
class ShareLinkViewModelTest {

    private lateinit var underTest: ShareLinkViewModel

    private val getNodeByIdUseCase = mock<GetNodeByIdUseCase>()
    private val exportNodeUseCase = mock<ExportNodeUseCase>()
    private val monitorAccountDetailUseCase = mock<MonitorAccountDetailUseCase>()
    private val splitLinkAndKeyUseCase = mock<SplitLinkAndKeyUseCase>()
    private val fileTypeIconMapper = mock<FileTypeIconMapper>()
    private val passwordCache = mock<ShareLinkPasswordCache>()
    private val separateKeyCache = mock<ShareLinkSeparateKeyCache>()

    @BeforeEach
    fun setUp() {
        whenever(monitorAccountDetailUseCase()).thenReturn(flowOf(AccountDetail()))
        whenever(splitLinkAndKeyUseCase(any())).thenReturn(LinkAndKey(null, null))
        whenever(fileTypeIconMapper(any(), any())).thenReturn(FILE_ICON_RES)
        whenever(passwordCache.monitor(any())).thenReturn(flowOf(null))
        whenever(separateKeyCache.monitor(any())).thenReturn(flowOf(false))
        underTest = ShareLinkViewModel(
            args = ShareLinkViewModel.Args(handles = listOf(NODE_HANDLE)),
            getNodeByIdUseCase = getNodeByIdUseCase,
            exportNodeUseCase = exportNodeUseCase,
            monitorAccountDetailUseCase = monitorAccountDetailUseCase,
            splitLinkAndKeyUseCase = splitLinkAndKeyUseCase,
            fileTypeIconMapper = fileTypeIconMapper,
            passwordCache = passwordCache,
            separateKeyCache = separateKeyCache,
        )
    }

    @AfterEach
    fun tearDown() {
        reset(
            getNodeByIdUseCase,
            exportNodeUseCase,
            monitorAccountDetailUseCase,
            splitLinkAndKeyUseCase,
            fileTypeIconMapper,
            passwordCache,
            separateKeyCache,
        )
    }

    @Test
    fun `test that uiState is Data with the existing public link and node details when the node is already exported`() =
        runTest {
            val node = mock<TypedFileNode> {
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
                val data = awaitData()
                assertThat(data.nodeName).isEqualTo("report.pdf")
                assertThat(data.isFolder).isFalse()
                assertThat(data.iconRes).isEqualTo(FILE_ICON_RES)
                assertThat(data.sizeInBytes).isEqualTo(2048L)
                assertThat(data.modificationTime).isEqualTo(1_718_000_000L)
                assertThat(data.link).isEqualTo("https://mega.nz/file/abc#key123")
                assertThat(data.linkWithoutKey).isEqualTo("https://mega.nz/file/abc")
                assertThat(data.key).isEqualTo("key123")
                cancelAndIgnoreRemainingEvents()
            }
            verifyNoInteractions(exportNodeUseCase)
        }

    @Test
    fun `test that uiState is Data with a link created via exportNodeUseCase when the node has no public link`() =
        runTest {
            val node = mock<TypedFileNode> {
                on { name } doReturn "video.mp4"
                on { exportedData } doReturn null
                on { size } doReturn 10L
                on { modificationTime } doReturn 5L
                on { type } doReturn UnknownFileTypeInfo(mimeType = "video/mp4", extension = "mp4")
            }
            whenever(getNodeByIdUseCase(NodeId(NODE_HANDLE))).thenReturn(node)
            whenever(exportNodeUseCase(NodeId(NODE_HANDLE), null, CALLER_NAME))
                .thenReturn("https://mega.nz/file/new#newkey")
            whenever(splitLinkAndKeyUseCase("https://mega.nz/file/new#newkey"))
                .thenReturn(LinkAndKey("https://mega.nz/file/new", "newkey"))

            underTest.uiState.test {
                val data = awaitData()
                assertThat(data.link).isEqualTo("https://mega.nz/file/new#newkey")
                assertThat(data.linkWithoutKey).isEqualTo("https://mega.nz/file/new")
                assertThat(data.key).isEqualTo("newkey")
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that uiState Data marks the node as a folder with no size when the node is a folder`() =
        runTest {
            val node = mock<TypedFolderNode> {
                on { name } doReturn "Documents"
                on { exportedData } doReturn ExportedData("https://mega.nz/folder/fid#fkey", 0L)
            }
            whenever(getNodeByIdUseCase(NodeId(NODE_HANDLE))).thenReturn(node)
            whenever(splitLinkAndKeyUseCase("https://mega.nz/folder/fid#fkey"))
                .thenReturn(LinkAndKey("https://mega.nz/folder/fid", "fkey"))

            underTest.uiState.test {
                val data = awaitData()
                assertThat(data.isFolder).isTrue()
                assertThat(data.sizeInBytes).isNull()
                assertThat(data.modificationTime).isNull()
                assertThat(data.key).isEqualTo("fkey")
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that uiState Data carries the account type from monitorAccountDetailUseCase`() =
        runTest {
            val node = mock<TypedFileNode> {
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
        verifyNoInteractions(exportNodeUseCase)
    }

    @Test
    fun `test that a cached password marks the link as password protected`() =
        runTest {
            val node = mock<TypedFileNode> {
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
                on { name } doReturn "report.pdf"
                on { exportedData } doReturn ExportedData("https://mega.nz/file/abc#key123", 0L)
                on { type } doReturn PdfFileTypeInfo
            }
            whenever(getNodeByIdUseCase(NodeId(NODE_HANDLE))).thenReturn(node)
            whenever(splitLinkAndKeyUseCase("https://mega.nz/file/abc#key123"))
                .thenReturn(LinkAndKey("https://mega.nz/file/abc", "key123"))
            whenever(separateKeyCache.monitor(NODE_HANDLE)).thenReturn(flowOf(true))

            underTest.uiState.test {
                val data = awaitData { it.isKeySeparate }
                assertThat(data.linkWithoutKey).isEqualTo("https://mega.nz/file/abc")
                assertThat(data.key).isEqualTo("key123")
                cancelAndIgnoreRemainingEvents()
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
        const val CALLER_NAME = "ShareLinkViewModel"
        const val ENCRYPTED_LINK = "https://mega.nz/#P!enc"
        val FILE_ICON_RES = iconPackR.drawable.ic_pdf_medium_solid
    }
}
