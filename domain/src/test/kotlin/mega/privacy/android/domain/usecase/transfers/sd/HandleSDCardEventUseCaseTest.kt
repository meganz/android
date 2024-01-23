package mega.privacy.android.domain.usecase.transfers.sd

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.entity.transfer.TransferAppData
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.repository.FileSystemRepository
import mega.privacy.android.domain.repository.TransferRepository
import mega.privacy.android.domain.usecase.transfers.downloads.GetOrCreateStorageDownloadLocationUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import java.io.File

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HandleSDCardEventUseCaseTest {
    private lateinit var underTest: HandleSDCardEventUseCase

    private val insertSdTransferUseCase = mock<InsertSdTransferUseCase>()
    private val deleteSdTransferByTagUseCase = mock<DeleteSdTransferByTagUseCase>()
    val getOrCreateStorageDownloadLocationUseCase =
        mock<GetOrCreateStorageDownloadLocationUseCase>()
    private val moveFileToSdCardUseCase = mock<MoveFileToSdCardUseCase>()
    private val fileSystemRepository = mock<FileSystemRepository>()
    private val transfersRepository = mock<TransferRepository>()

    private val scope = CoroutineScope(UnconfinedTestDispatcher())

    @BeforeAll
    fun setUp() {

        underTest = HandleSDCardEventUseCase(
            insertSdTransferUseCase,
            deleteSdTransferByTagUseCase,
            getOrCreateStorageDownloadLocationUseCase,
            moveFileToSdCardUseCase,
            fileSystemRepository,
            transfersRepository,
            scope,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            insertSdTransferUseCase,
            deleteSdTransferByTagUseCase,
            getOrCreateStorageDownloadLocationUseCase,
            moveFileToSdCardUseCase,
            fileSystemRepository,
            transfersRepository,
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
    fun `test that file is moved to the destination specified by its own path in the sd saved path when no root transfer is finished`() =
        runTest {
            val transfer = mockTransfer()
            val sdPath = "sdPath"
            val cachePath = "cachePath"
            val destination = "someFolder"
            val fileName = "finalFile.txt"
            val fileLocalPath = "$cachePath/$destination/$fileName"
            val expectedPath = "$sdPath/$destination"
            val cacheFolder = mock<File> {
                on { path } doReturn cachePath
            }
            whenever(transfer.isRootTransfer).thenReturn(false)
            whenever(transfer.localPath).thenReturn(fileLocalPath)
            whenever(fileSystemRepository.isSDCardCachePath(any())).thenReturn(true)
            whenever(transfersRepository.getOrCreateSDCardTransfersCacheFolder())
                .thenReturn(cacheFolder)
            whenever(getOrCreateStorageDownloadLocationUseCase()).thenReturn(sdPath)
            val transferEvent = TransferEvent.TransferFinishEvent(transfer, null)
            underTest(transferEvent)
            verify(moveFileToSdCardUseCase).invoke(eq(File(fileLocalPath)), eq(expectedPath))
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
    }
}