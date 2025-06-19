package mega.privacy.android.app.presentation.transfers.model.completed

import android.net.Uri
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.triggered
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.data.mapper.FileTypeInfoMapper
import mega.privacy.android.domain.entity.PdfFileTypeInfo
import mega.privacy.android.domain.entity.document.DocumentEntity
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.entity.transfer.CompletedTransfer
import mega.privacy.android.domain.entity.transfer.TransferState
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.node.GetNodeByHandleUseCase
import mega.privacy.android.domain.usecase.shares.GetNodeAccessPermission
import mega.privacy.android.domain.usecase.transfers.completed.DeleteCompletedTransferUseCase
import mega.privacy.android.domain.usecase.transfers.completed.GetDownloadDocumentFileUseCase
import mega.privacy.android.domain.usecase.transfers.completed.GetDownloadParentDocumentFileUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.RegisterExtension
import org.junit.jupiter.api.io.TempDir
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.mockito.kotlin.wheneverBlocking
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CompletedTransferActionsViewModelTest {

    private lateinit var underTest: CompletedTransferActionsViewModel

    private val getNodeAccessPermission = mock<GetNodeAccessPermission>()
    private val getDownloadParentDocumentFileUseCase = mock<GetDownloadParentDocumentFileUseCase>()
    private val getDownloadDocumentFileUseCase = mock<GetDownloadDocumentFileUseCase>()
    private val monitorConnectivityUseCase = mock<MonitorConnectivityUseCase>()
    private val deleteCompletedTransferUseCase = mock<DeleteCompletedTransferUseCase>()
    private val getNodeByHandleUseCase = mock<GetNodeByHandleUseCase>()
    private val fileTypeInfoMapper = mock<FileTypeInfoMapper>()

    @TempDir
    lateinit var temporaryFolder: File

    private val completedDownload = CompletedTransfer(
        id = 0,
        fileName = "fileName.txt",
        type = TransferType.DOWNLOAD,
        state = TransferState.STATE_COMPLETED,
        size = "3.57 MB",
        handle = 27169983390750L,
        path = "content://com.android.externalstorage.documents/tree/primary%Download//2023-03-24 00.13.20_1.pdf",
        displayPath = "storage/emulated/0/Download",
        isOffline = false,
        timestamp = 1684228012974L,
        error = "No error",
        errorCode = 0,
        originalPath = "content://com.android.externalstorage.documents/tree/primary%Download//2023-03-24 00.13.20_1.pdf",
        parentHandle = 11622336899311L,
        appData = emptyList(),
    )

    @BeforeAll
    fun initTest() {
        initializeTest()
    }

    private fun initializeTest() {
        underTest = CompletedTransferActionsViewModel(
            getNodeAccessPermission = getNodeAccessPermission,
            getDownloadParentDocumentFileUseCase = getDownloadParentDocumentFileUseCase,
            getDownloadDocumentFileUseCase = getDownloadDocumentFileUseCase,
            monitorConnectivityUseCase = monitorConnectivityUseCase,
            deleteCompletedTransferUseCase = deleteCompletedTransferUseCase,
            getNodeByHandleUseCase = getNodeByHandleUseCase,
            fileTypeInfoMapper = fileTypeInfoMapper
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            getNodeAccessPermission,
            getDownloadParentDocumentFileUseCase,
            getDownloadDocumentFileUseCase,
            monitorConnectivityUseCase,
            deleteCompletedTransferUseCase,
            getNodeByHandleUseCase,
            fileTypeInfoMapper
        )

        wheneverBlocking { monitorConnectivityUseCase() } doReturn flowOf(true)
    }

    @Test
    fun `test initial state is correct`() = runTest {
        val expectedState = CompletedTransferActionsUiState()

        initializeTest()

        underTest.uiState.test {
            assertThat(awaitItem()).isEqualTo(expectedState)
        }
    }

    @Test
    fun `test that monitorConnectivity updates state correctly`() = runTest {
        whenever(monitorConnectivityUseCase()) doReturn flowOf(false)

        initializeTest()
        advanceUntilIdle()

        underTest.uiState.test {
            assertThat(awaitItem().isOnline).isFalse()
        }
    }

    @Test
    fun `test that checkCompletedTransferActions updates state correctly for a transfer where isContentUriDownload is true`() =
        runTest {
            mockStatic(Uri::class.java).use {
                val uriPath =
                    UriPath("content://com.android.externalstorage.documents/tree/primary%Download")
                val parentDocumentFile = mock<DocumentEntity> {
                    on { uri } doReturn uriPath
                }
                val documentFile = mock<DocumentEntity> {
                    on { uri } doReturn uriPath
                }
                val uri = mock<Uri> {
                    on { scheme } doReturn "content"
                }

                whenever(getNodeAccessPermission(NodeId(completedDownload.handle))) doReturn AccessPermission.OWNER
                whenever(getDownloadParentDocumentFileUseCase(completedDownload.path)) doReturn parentDocumentFile
                whenever(Uri.parse(uriPath.value)) doReturn uri
                whenever(
                    getDownloadDocumentFileUseCase(
                        completedDownload.path,
                        completedDownload.fileName,
                    )
                ) doReturn documentFile

                initializeTest()

                underTest.checkCompletedTransferActions(completedDownload)

                underTest.uiState.test {
                    val state = awaitItem()
                    assertThat(state.completedTransfer).isEqualTo(completedDownload)
                    assertThat(state.isOnline).isTrue()
                    assertThat(state.amINodeOwner).isTrue()
                    assertThat(state.parentUri).isEqualTo(uri)
                    assertThat(state.fileUri).isEqualTo(uri)
                }
            }
        }

    @Test
    fun `test that checkCompletedTransferActions updates state correctly for a transfer where isContentUriDownload is false`() =
        runTest {
            val completedUpload = CompletedTransfer(
                id = 0,
                fileName = "fileName.txt",
                type = TransferType.GENERAL_UPLOAD,
                state = TransferState.STATE_COMPLETED,
                size = "3.57 MB",
                handle = 27169983390750L,
                path = "content://com.android.externalstorage.documents/tree/primary%Download//2023-03-24 00.13.20_1.pdf",
                displayPath = "storage/emulated/0/Download",
                isOffline = false,
                timestamp = 1684228012974L,
                error = "No error",
                errorCode = 0,
                originalPath = "/original/path/2023-03-24 00.13.20_1.pdf",
                parentHandle = 11622336899311L,
                appData = emptyList(),
            )

            whenever(getNodeAccessPermission(NodeId(completedUpload.handle))) doReturn AccessPermission.OWNER

            initializeTest()

            underTest.checkCompletedTransferActions(completedUpload)

            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.completedTransfer).isEqualTo(completedUpload)
                assertThat(state.isOnline).isTrue()
                assertThat(state.amINodeOwner).isTrue()
                assertThat(state.parentUri).isNull()
                assertThat(state.fileUri).isNull()
            }
        }

    @Test
    fun `test that openWith updates state correctly for a file`() = runTest {
        val download =
            completedDownload.copy(originalPath = temporaryFolder.absolutePath + File.separator + completedDownload.fileName)
        val file = File(temporaryFolder, download.fileName)
        file.createNewFile()
        val fileTypeInfo = PdfFileTypeInfo
        val expected = triggered(OpenWithEvent(file = file, fileType = fileTypeInfo.mimeType))

        whenever(fileTypeInfoMapper(download.fileName)) doReturn fileTypeInfo

        initializeTest()

        underTest.checkCompletedTransferActions(download)
        underTest.openWith(download)

        underTest.uiState.test {
            assertThat(awaitItem().openWithEvent).isEqualTo(expected)
        }
    }

    @Test
    fun `test that openWith updates state correctly for a uri`() = runTest {
        mockStatic(Uri::class.java).use {
            val uriPath =
                UriPath("content://com.android.externalstorage.documents/tree/primary%Download//${completedDownload.fileName}")
            val documentFile = mock<DocumentEntity> {
                on { uri } doReturn uriPath
            }
            val uri = mock<Uri> {
                on { scheme } doReturn "content"
            }
            val fileTypeInfo = PdfFileTypeInfo
            val expected = triggered(OpenWithEvent(uri = uri, fileType = fileTypeInfo.mimeType))

            whenever(
                getDownloadDocumentFileUseCase(
                    completedDownload.path,
                    completedDownload.fileName,
                )
            ) doReturn documentFile
            whenever(Uri.parse(uriPath.value)) doReturn uri
            whenever(fileTypeInfoMapper(completedDownload.fileName)) doReturn fileTypeInfo

            initializeTest()

            underTest.checkCompletedTransferActions(completedDownload)
            advanceUntilIdle()
            underTest.openWith(completedDownload)

            underTest.uiState.test {
                assertThat(awaitItem().openWithEvent).isEqualTo(expected)
            }
        }
    }

    @Test
    fun `test that openWith updates state correctly for a error uri`() = runTest {
        mockStatic(Uri::class.java).use {
            val fileTypeInfo = PdfFileTypeInfo
            val expected = triggered(OpenWithEvent())


            whenever(fileTypeInfoMapper(completedDownload.fileName)) doReturn fileTypeInfo

            initializeTest()

            underTest.checkCompletedTransferActions(completedDownload)
            advanceUntilIdle()
            underTest.openWith(completedDownload)

            underTest.uiState.test {
                assertThat(awaitItem().openWithEvent).isEqualTo(expected)
            }
        }
    }

    @Test
    fun `test that shareLink updates state correctly for a valid node`() = runTest {
        val untypedNode = mock<FileNode> {
            on { isTakenDown } doReturn false
        }
        val expected = triggered(ShareLinkEvent(untypedNode))

        whenever(getNodeByHandleUseCase(completedDownload.handle)) doReturn untypedNode

        initializeTest()

        underTest.shareLink(completedDownload.handle)

        underTest.uiState.test {
            assertThat(awaitItem().shareLinkEvent).isEqualTo(expected)
        }
    }

    @Test
    fun `test that shareLink updates state correctly for an invalid node`() = runTest {
        val expected = triggered(ShareLinkEvent())

        whenever(getNodeByHandleUseCase(completedDownload.handle)) doReturn null

        initializeTest()

        underTest.shareLink(completedDownload.handle)

        underTest.uiState.test {
            assertThat(awaitItem().shareLinkEvent).isEqualTo(expected)
        }
    }

    @Test
    fun `test that clearTransfer invokes correctly`() = runTest {
        initializeTest()

        underTest.clearTransfer(completedDownload)

        verify(deleteCompletedTransferUseCase).invoke(completedDownload, false)
    }

    companion object {
        private val testDispatcher = UnconfinedTestDispatcher()

        @JvmField
        @RegisterExtension
        val extension = CoroutineMainDispatcherExtension(testDispatcher)
    }
}