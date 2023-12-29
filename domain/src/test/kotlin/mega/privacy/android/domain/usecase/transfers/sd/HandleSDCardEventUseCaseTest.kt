package mega.privacy.android.domain.usecase.transfers.sd

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.entity.transfer.TransferAppData
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.repository.FileSystemRepository
import mega.privacy.android.domain.usecase.transfers.downloads.GetDownloadLocationForNodeIdUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HandleSDCardEventUseCaseTest {
    private lateinit var underTest: HandleSDCardEventUseCase

    private val insertSdTransferUseCase = mock<InsertSdTransferUseCase>()
    private val deleteSdTransferByTagUseCase = mock<DeleteSdTransferByTagUseCase>()
    private val getDownloadLocationForNodeIdUseCase = mock<GetDownloadLocationForNodeIdUseCase>()
    private val moveFileToSdCardUseCase = mock<MoveFileToSdCardUseCase>()
    private val fileSystemRepository = mock<FileSystemRepository>()

    private val scope = CoroutineScope(UnconfinedTestDispatcher())

    @BeforeAll
    fun setUp() {

        underTest = HandleSDCardEventUseCase(
            insertSdTransferUseCase,
            deleteSdTransferByTagUseCase,
            getDownloadLocationForNodeIdUseCase,
            moveFileToSdCardUseCase,
            fileSystemRepository,
            scope,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            insertSdTransferUseCase,
            deleteSdTransferByTagUseCase,
            getDownloadLocationForNodeIdUseCase,
            moveFileToSdCardUseCase,
            fileSystemRepository,
        )
    }

    @Test
    fun `test that insertSdTransferUseCase is invoked for sd transfers start event`() = runTest {
        val transfer = mockTransfer()
        val transferEvent = TransferEvent.TransferStartEvent(transfer)
        underTest(transferEvent)
        verify(insertSdTransferUseCase)(any())
    }

    @Test
    fun `test that file is moved to destination specified in app data when root transfer is finished`() =
        runTest {
            val transfer = mockTransfer()
            whenever(fileSystemRepository.isSDCardCachePath(any())).thenReturn(true)
            whenever(transfer.isRootTransfer).thenReturn(true)
            val transferEvent = TransferEvent.TransferFinishEvent(transfer, null)
            underTest(transferEvent)
            verify(moveFileToSdCardUseCase).invoke(any(), eq(TARGET_PATH))
        }

    @Test
    fun `test that file is moved to destination specified with use case when no root transfer is finished`() =
        runTest {
            val transfer = mockTransfer()
            whenever(fileSystemRepository.isSDCardCachePath(any())).thenReturn(true)
            whenever(transfer.isRootTransfer).thenReturn(false)
            whenever(getDownloadLocationForNodeIdUseCase(NodeId(any()))).thenReturn(TARGET_PATH2)
            val transferEvent = TransferEvent.TransferFinishEvent(transfer, null)
            underTest(transferEvent)
            verify(moveFileToSdCardUseCase).invoke(any(), eq(TARGET_PATH2))
        }

    @Test
    fun `test that deleteSdTransferByTagUseCase is invoked when root sd transfers finish event is received`() =
        runTest {
            val transfer = mockTransfer()
            whenever(transfer.isRootTransfer).thenReturn(true)
            whenever(transfer.isFolderTransfer).thenReturn(true)
            val transferEvent = TransferEvent.TransferFinishEvent(transfer, null)
            underTest(transferEvent)
            verify(deleteSdTransferByTagUseCase)(any())
        }

    @Test
    fun `test that deleteSdTransferByTagUseCase is not invoked when the received finish event is not a sd transfer`() =
        runTest {
            val transfer = mockTransfer()
            whenever(transfer.isRootTransfer).thenReturn(true)
            whenever(transfer.isFolderTransfer).thenReturn(true)
            whenever(transfer.appData).thenReturn(emptyList())
            val transferEvent = TransferEvent.TransferFinishEvent(transfer, null)
            underTest(transferEvent)
            verifyNoInteractions(deleteSdTransferByTagUseCase)
        }

    private fun mockTransfer() =
        mock<Transfer> {
            on { nodeHandle }.thenReturn(1L)
            on { appData }.thenReturn(listOf(TransferAppData.SdCardDownload(TARGET_PATH, null)))
            on { tag }.thenReturn(1)
            on { fileName }.thenReturn("name")
            on { totalBytes }.thenReturn(11L)
            on { nodeHandle }.thenReturn(1L)
            on { localPath }.thenReturn("localPath")
            on { isFolderTransfer }.thenReturn(false)
        }

    private companion object {
        const val TARGET_PATH = "path"
        const val TARGET_PATH2 = "path2"
    }
}