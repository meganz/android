package mega.privacy.android.domain.usecase.transfers.sd

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.entity.transfer.TransferAppData
import mega.privacy.android.domain.repository.FileSystemRepository
import mega.privacy.android.domain.repository.TransferRepository
import mega.privacy.android.domain.usecase.transfers.GetTransferByTagUseCase
import mega.privacy.android.domain.usecase.transfers.completed.AddCompletedTransferIfNotExistUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import java.io.File

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HandleNotInProgressSDCardActiveTransfersUseCaseTest {

    private lateinit var underTest: HandleNotInProgressSDCardActiveTransfersUseCase

    private val addCompletedTransferIfNotExistUseCase =
        mock<AddCompletedTransferIfNotExistUseCase>()
    private val getTransferByTagUseCase = mock<GetTransferByTagUseCase>()
    private val moveFileToSdCardUseCase = mock<MoveFileToSdCardUseCase>()
    private val transferRepository = mock<TransferRepository>()
    private val fileSystemRepository = mock<FileSystemRepository>()
    private val scope = CoroutineScope(UnconfinedTestDispatcher())

    private val targetPathForSDK = "targetPathForSDK"
    private val finalTargetUri = "finalTargetUri"
    private val subFolders = "parentPath/subFolders"
    private val parentPath = "$targetPathForSDK/$subFolders"
    private val downloadedFileSize = 100L
    private val notInProgressSdCardAppDataMap =
        (0..10).associateWith { listOf(mock<TransferAppData.SdCardDownload>()) }
    private val singleNotInProgressSdCardAppDataMap = mapOf(
        1 to listOf(
            TransferAppData.SdCardDownload(
                targetPathForSDK = targetPathForSDK,
                finalTargetUri = finalTargetUri,
                parentPath = parentPath,
            )
        )
    )
    private val sdkDownloadedFile = mock<File> {
        on { exists() }.thenReturn(true)
        on { length() }.thenReturn(downloadedFileSize)
    }

    @BeforeAll
    fun setUp() {
        underTest = HandleNotInProgressSDCardActiveTransfersUseCase(
            addCompletedTransferIfNotExistUseCase = addCompletedTransferIfNotExistUseCase,
            getTransferByTagUseCase = getTransferByTagUseCase,
            moveFileToSdCardUseCase = moveFileToSdCardUseCase,
            transferRepository = transferRepository,
            fileSystemRepository = fileSystemRepository,
            scope = scope,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            addCompletedTransferIfNotExistUseCase,
            getTransferByTagUseCase,
            moveFileToSdCardUseCase,
            transferRepository,
            fileSystemRepository,
        )
    }

    @Test
    fun `test that AddCompletedTransferIfNotExistUseCase is invoked if GetTransferByTagUseCase returns a transfer`() =
        runTest {
            val nullTransfers = notInProgressSdCardAppDataMap.filter { it.key.mod(2) == 0 }

            val transfers = buildList {
                notInProgressSdCardAppDataMap.forEach { (tag, _) ->
                    if (tag in nullTransfers.keys) {
                        whenever(getTransferByTagUseCase(tag)).thenReturn(null)
                    } else {
                        val transfer = mock<Transfer> {
                            on { this.tag }.thenReturn(tag)
                        }
                        whenever(getTransferByTagUseCase(tag)).thenReturn(transfer)
                        add(transfer)
                    }
                }
            }

            underTest.invoke(notInProgressSdCardAppDataMap)

            verify(addCompletedTransferIfNotExistUseCase).invoke(transfers)
        }

    @Test
    fun `test that FileSystemRepository is not invoked if the appData does not have a valid parentPath`() =
        runTest {
            val singleNotInProgressSdCardAppDataMap = mapOf(
                1 to listOf(
                    TransferAppData.SdCardDownload(
                        targetPathForSDK = targetPathForSDK,
                        finalTargetUri = finalTargetUri,
                        parentPath = null
                    )
                )
            )

            underTest.invoke(singleNotInProgressSdCardAppDataMap)

            verifyNoInteractions(fileSystemRepository)
            verifyNoInteractions(moveFileToSdCardUseCase)
        }

    @Test
    fun `test that deleteFile in FileSystemRepository is invoked if the sdk download exists and final file exists and has the same size`() =
        runTest {
            whenever(fileSystemRepository.getFileByPath(targetPathForSDK))
                .thenReturn(sdkDownloadedFile)
            whenever(fileSystemRepository.getFileLengthFromSdCardContentUri(finalTargetUri))
                .thenReturn(downloadedFileSize)

            underTest.invoke(singleNotInProgressSdCardAppDataMap)

            verify(fileSystemRepository).deleteFile(sdkDownloadedFile)
        }

    @Test
    fun `test that MoveFileToSdCardUseCase is invoked if the sdk download exists and final file does not have the same length`() =
        runTest {
            whenever(fileSystemRepository.getFileByPath(targetPathForSDK))
                .thenReturn(sdkDownloadedFile)
            whenever(fileSystemRepository.getFileLengthFromSdCardContentUri(finalTargetUri))
                .thenReturn(0L)

            underTest.invoke(singleNotInProgressSdCardAppDataMap)

            verify(moveFileToSdCardUseCase).invoke(
                sdkDownloadedFile,
                finalTargetUri,
                subFolders.split(File.separator)
            )
        }

    @Test
    fun `test that deleteFileFromSdCardContentUri in FileSystemRepository is invoked if the sdk download exists and final file exists but does not have the same size`() =
        runTest {
            whenever(fileSystemRepository.getFileByPath(targetPathForSDK))
                .thenReturn(sdkDownloadedFile)
            whenever(fileSystemRepository.getFileLengthFromSdCardContentUri(finalTargetUri))
                .thenReturn(0L)

            underTest.invoke(singleNotInProgressSdCardAppDataMap)

            verify(fileSystemRepository).deleteFileFromSdCardContentUri(finalTargetUri)
        }

    @Test
    fun `test that removeInProgressTransfers in TransferRepository is invoked`() = runTest {
        underTest.invoke(notInProgressSdCardAppDataMap)
        verify(transferRepository).removeInProgressTransfers(notInProgressSdCardAppDataMap.keys.toSet())
    }
}