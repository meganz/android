package mega.privacy.android.app.presentation.transfers.model.completed

import android.net.Uri
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.StateEventWithContentTriggered
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.data.extensions.toUriPath
import mega.privacy.android.data.mapper.FileTypeInfoMapper
import mega.privacy.android.domain.entity.PdfFileTypeInfo
import mega.privacy.android.domain.entity.document.DocumentEntity
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.offline.OfflineFileInformation
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.entity.transfer.CompletedTransfer
import mega.privacy.android.domain.entity.transfer.TransferState
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.node.GetNodeByHandleUseCase
import mega.privacy.android.domain.usecase.offline.GetOfflineNodeInformationByNodeIdUseCase
import mega.privacy.android.domain.usecase.shares.GetNodeAccessPermission
import mega.privacy.android.domain.usecase.transfers.completed.DeleteCompletedTransferUseCase
import mega.privacy.android.domain.usecase.transfers.completed.GetDownloadDocumentFileUseCase
import mega.privacy.android.domain.usecase.transfers.completed.GetDownloadParentDocumentFileUseCase
import mega.privacy.android.feature_flags.AppFeatures
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.RegisterExtension
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.any
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
    private val getFeatureFlagValueUseCase = mock<GetFeatureFlagValueUseCase>()
    private val getOfflineNodeInformationByNodeIdUseCase =
        mock<GetOfflineNodeInformationByNodeIdUseCase>()

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

    private val completedOffline = CompletedTransfer(
        id = 0,
        fileName = "fileName.txt",
        type = TransferType.DOWNLOAD,
        state = TransferState.STATE_COMPLETED,
        size = "3.57 MB",
        handle = 27169983390750L,
        path = "content://com.android.externalstorage.documents/tree/primary%Download//2023-03-24 00.13.20_1.pdf",
        displayPath = "storage/emulated/0/Download",
        isOffline = true,
        timestamp = 1684228012974L,
        error = "No error",
        errorCode = 0,
        originalPath = "content://com.android.externalstorage.documents/tree/primary%Download//2023-03-24 00.13.20_1.pdf",
        parentHandle = 11622336899311L,
        appData = emptyList(),
    )

    private val completedUpload = CompletedTransfer(
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
        originalPath = "content://com.android.externalstorage.documents/tree/primary%Download//2023-03-24 00.13.20_1.pdf",
        parentHandle = 11622336899311L,
        appData = emptyList(),
    )

    private fun mockCompletedTransferUriPath(completedTransfer: CompletedTransfer) {
        val uri = mock<Uri> {
            on { this.scheme } doReturn "content"
            on { this.toString() } doReturn completedTransfer.path
        }
        whenever(Uri.parse(completedTransfer.path)) doReturn uri
    }

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
            fileTypeInfoMapper = fileTypeInfoMapper,
            getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
            getOfflineNodeInformationByNodeIdUseCase = getOfflineNodeInformationByNodeIdUseCase,
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
            fileTypeInfoMapper,
            getFeatureFlagValueUseCase,
            getOfflineNodeInformationByNodeIdUseCase,
        )

        wheneverBlocking { monitorConnectivityUseCase() } doReturn flowOf(true)
        wheneverBlocking { getFeatureFlagValueUseCase(AppFeatures.SingleActivity) } doReturn false
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

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that onViewInFolder emits new download event when is invoked with a download completed transfer`(
        singleActivity: Boolean,
    ) = runTest {
        mockStatic(Uri::class.java).use {
            mockCompletedTransferUriPath(completedOffline)
            whenever(getFeatureFlagValueUseCase(AppFeatures.SingleActivity)) doReturn singleActivity

            underTest.onViewInFolder(completedDownload)

            assertThat((underTest.uiState.value.viewInFolderEvent as? StateEventWithContentTriggered)?.content).isInstanceOf(
                ViewInFolderEvent.Download::class.java
            )
            assertThat(((underTest.uiState.value.viewInFolderEvent as? StateEventWithContentTriggered)?.content as? ViewInFolderEvent.Found)?.singleActivity).isEqualTo(
                singleActivity
            )
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that onViewInFolder emits new download to offline event when is invoked with a download to offline completed transfer`(
        singleActivity: Boolean,
    ) = runTest {
        mockStatic(Uri::class.java).use {
            mockCompletedTransferUriPath(completedOffline)
            val offlineInfo = mock<OfflineFileInformation> {
                on { this.id } doReturn 425
                on { this.name } doReturn "parent"
            }
            val expected = ViewInFolderEvent.DownloadToOffline(
                singleActivity = singleActivity,
                fileName = completedOffline.fileName,
                parentNodeOfflineId = offlineInfo.id,
                title = offlineInfo.name,
                uriPath = completedOffline.path.toUriPath()
            )
            whenever(getFeatureFlagValueUseCase(AppFeatures.SingleActivity)) doReturn singleActivity
            whenever(getOfflineNodeInformationByNodeIdUseCase(NodeId(completedOffline.parentHandle))) doReturn offlineInfo
            underTest.onViewInFolder(completedOffline)

            assertThat((underTest.uiState.value.viewInFolderEvent as? StateEventWithContentTriggered)?.content)
                .isEqualTo(expected)
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that onViewInFolder emits new upload event when is invoked with an upload completed transfer`(
        singleActivity: Boolean,
    ) = runTest {
        whenever(getFeatureFlagValueUseCase(AppFeatures.SingleActivity)) doReturn singleActivity

        underTest.onViewInFolder(completedUpload)

        assertThat((underTest.uiState.value.viewInFolderEvent as? StateEventWithContentTriggered)?.content).isInstanceOf(
            ViewInFolderEvent.Upload::class.java
        )
        assertThat(((underTest.uiState.value.viewInFolderEvent as? StateEventWithContentTriggered)?.content as? ViewInFolderEvent.Found)?.singleActivity).isEqualTo(
            singleActivity
        )
    }

    @Test
    fun `test that onViewInFolder emits not found event when is invoked with a completed transfer that does not exist anymore`() =
        runTest {
            mockStatic(Uri::class.java).use {
                mockCompletedTransferUriPath(completedOffline)
                whenever(getOfflineNodeInformationByNodeIdUseCase(any())) doReturn null

                underTest.onViewInFolder(completedOffline)

                assertThat((underTest.uiState.value.viewInFolderEvent as? StateEventWithContentTriggered)?.content)
                    .isEqualTo(ViewInFolderEvent.NotFound)
            }
        }

    @Test
    fun `test that onConsumeViewInFolder consumes the event correctly`() = runTest {
        mockStatic(Uri::class.java).use {
            mockCompletedTransferUriPath(completedOffline)

            underTest.onViewInFolder(completedDownload)
            underTest.onConsumeViewInFolder()

            assertThat(underTest.uiState.value.viewInFolderEvent).isEqualTo(consumed())
        }
    }

    companion object {
        private val testDispatcher = UnconfinedTestDispatcher()

        @JvmField
        @RegisterExtension
        val extension = CoroutineMainDispatcherExtension(testDispatcher)
    }
}