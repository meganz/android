package mega.privacy.android.domain.usecase.transfers.sd

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.transfer.DestinationUriAndSubFolders
import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.entity.transfer.TransferAppData
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.repository.FileSystemRepository
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
    private val moveFileToSdCardUseCase = mock<MoveFileToSdCardUseCase>()
    private val fileSystemRepository = mock<FileSystemRepository>()

    private val scope = CoroutineScope(UnconfinedTestDispatcher())

    @BeforeAll
    fun setUp() {

        underTest = HandleSDCardEventUseCase(
            insertSdTransferUseCase,
            deleteSdTransferByTagUseCase,
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
            moveFileToSdCardUseCase,
            fileSystemRepository,
        )
    }

    @Test
    fun `test that insertSdTransferUseCase is invoked for sd transfers start event`() = runTest {
        val transfer = mockTransfer()
        val transferEvent = TransferEvent.TransferStartEvent(transfer)
        whenever(fileSystemRepository.isSDCardCachePath(any())).thenReturn(false)
        underTest(transferEvent, null)
        verify(insertSdTransferUseCase)(any())
    }

    @Test
    fun `test that file is moved to destination specified in DestinationUriAndSubFolders when transfer is finished`() =
        runTest {
            val transfer = mockTransfer()
            whenever(fileSystemRepository.isSDCardCachePath(any())).thenReturn(true)
            val subFolders = mock<List<String>>()
            val transferEvent = TransferEvent.TransferFinishEvent(transfer, null)
            underTest(transferEvent, DestinationUriAndSubFolders(TARGET_PATH, subFolders))
            verify(moveFileToSdCardUseCase).invoke(any(), eq(TARGET_PATH), eq(subFolders))
        }

    @Test
    fun `test that deleteSdTransferByTagUseCase is invoked when root sd transfers finish event is received`() =
        runTest {
            val transfer = mockTransfer()
            whenever(transfer.isRootTransfer).thenReturn(true)
            whenever(transfer.isFolderTransfer).thenReturn(true)
            val transferEvent = TransferEvent.TransferFinishEvent(transfer, null)
            underTest(transferEvent, null)
            verify(deleteSdTransferByTagUseCase)(any())
        }

    @Test
    fun `test that deleteSdTransferByTagUseCase is not invoked when the received finish event is not a sd transfer`() =
        runTest {
            val transfer = mockTransfer()
            whenever(transfer.isRootTransfer).thenReturn(true)
            whenever(transfer.isFolderTransfer).thenReturn(true)
            whenever(transfer.appData).thenReturn(emptyList())
            whenever(fileSystemRepository.isSDCardCachePath(any())).thenReturn(false)
            val transferEvent = TransferEvent.TransferFinishEvent(transfer, null)
            underTest(transferEvent, null)
            verifyNoInteractions(deleteSdTransferByTagUseCase)
        }

    private fun mockTransfer() =
        mock<Transfer> {
            on { nodeHandle }.thenReturn(1L)
            on { appData }.thenReturn(
                listOf(TransferAppData.SdCardDownload(TARGET_PATH, TARGET_PATH))
            )
            on { tag }.thenReturn(1)
            on { fileName }.thenReturn("name")
            on { totalBytes }.thenReturn(11L)
            on { nodeHandle }.thenReturn(1L)
            on { localPath }.thenReturn("localPath")
            on { isFolderTransfer }.thenReturn(false)
            on { transferType }.thenReturn(TransferType.DOWNLOAD)
        }

    private companion object {
        const val TARGET_PATH = "path"
    }
}