package mega.privacy.android.domain.usecase.node

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.transfer.ActiveTransferTotals
import mega.privacy.android.domain.entity.transfer.MonitorOngoingActiveTransfersResult
import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.entity.transfer.TransferAppData
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.usecase.transfers.active.MonitorOngoingActiveTransfersUseCase
import mega.privacy.android.domain.usecase.transfers.downloads.StartDownloadsWorkerAndWaitUntilIsStartedUseCase
import mega.privacy.android.domain.usecase.transfers.pending.InsertPendingDownloadsForNodesUseCase
import mega.privacy.android.domain.usecase.transfers.previews.GetPreviewDownloadUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyBlocking
import org.mockito.kotlin.whenever
import java.io.File
import java.nio.file.Files

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DownloadPreviewFileForNodeAndAwaitUseCaseTest {

    private val getFilePreviewDownloadPathUseCase: GetFilePreviewDownloadPathUseCase = mock()
    private val getPreviewDownloadUseCase: GetPreviewDownloadUseCase = mock()
    private val insertPendingDownloadsForNodesUseCase: InsertPendingDownloadsForNodesUseCase =
        mock()
    private val startDownloadsWorkerAndWaitUntilIsStartedUseCase:
            StartDownloadsWorkerAndWaitUntilIsStartedUseCase = mock()
    private val monitorOngoingActiveTransfersUseCase: MonitorOngoingActiveTransfersUseCase = mock()

    private lateinit var underTest: DownloadPreviewFileForNodeAndAwaitUseCase

    @BeforeEach
    fun setUp() {
        reset(
            getFilePreviewDownloadPathUseCase,
            getPreviewDownloadUseCase,
            insertPendingDownloadsForNodesUseCase,
            startDownloadsWorkerAndWaitUntilIsStartedUseCase,
            monitorOngoingActiveTransfersUseCase,
        )
        underTest = DownloadPreviewFileForNodeAndAwaitUseCase(
            getFilePreviewDownloadPathUseCase = getFilePreviewDownloadPathUseCase,
            getPreviewDownloadUseCase = getPreviewDownloadUseCase,
            insertPendingDownloadsForNodesUseCase = insertPendingDownloadsForNodesUseCase,
            startDownloadsWorkerAndWaitUntilIsStartedUseCase = startDownloadsWorkerAndWaitUntilIsStartedUseCase,
            monitorOngoingActiveTransfersUseCase = monitorOngoingActiveTransfersUseCase,
        )
    }

    @Test
    fun `test that invoke returns existing dest file when file already exists with content`() =
        runTest {
            val node: TypedFileNode = mock()
            whenever(node.name).thenReturn("existing.pdf")
            val tempDir = Files.createTempDirectory("preview_dest_1").toFile()
            try {
                val dest = File(tempDir, "existing.pdf").apply {
                    createNewFile()
                    writeText("data")
                }
                whenever(getFilePreviewDownloadPathUseCase()).thenReturn(tempDir.absolutePath)

                val result = underTest(node)

                assertThat(result).isEqualTo(dest)
                verify(insertPendingDownloadsForNodesUseCase, never()).invoke(
                    any(),
                    any(),
                    any(),
                    any(),
                )
                verify(startDownloadsWorkerAndWaitUntilIsStartedUseCase, never()).invoke()
            } finally {
                tempDir.deleteRecursively()
            }
        }

    @Test
    fun `test that invoke skips insert when preview download already exists`() = runTest {
        val node: TypedFileNode = mock()
        whenever(node.name).thenReturn("inflight.mp4")
        val existingTransfer: Transfer = mock()
        val tempDir = Files.createTempDirectory("preview_dest_2").toFile()
        try {
            whenever(getFilePreviewDownloadPathUseCase()).thenReturn(tempDir.absolutePath)
            whenever(getPreviewDownloadUseCase(node)).thenReturn(existingTransfer)
            val inProgress = previewActionGroup("inflight.mp4", finishedFiles = 0)
            val finished = previewActionGroup("inflight.mp4", finishedFiles = 1)
            whenever(monitorOngoingActiveTransfersUseCase(TransferType.DOWNLOAD)).thenReturn(
                flow {
                    emit(transferResult(listOf(inProgress)))
                    File(tempDir, "inflight.mp4").apply {
                        createNewFile()
                        writeText("x")
                    }
                    emit(transferResult(listOf(finished)))
                }
            )

            val result = underTest(node)

            assertThat(result.readText()).isEqualTo("x")
            verify(insertPendingDownloadsForNodesUseCase, never()).invoke(
                any(),
                any(),
                any(),
                any(),
            )
            verify(startDownloadsWorkerAndWaitUntilIsStartedUseCase, never()).invoke()
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `test that invoke inserts pending download and starts worker when no preview exists`() =
        runTest {
            val node: TypedFileNode = mock()
            whenever(node.name).thenReturn("new.doc")
            val tempDir = Files.createTempDirectory("preview_dest_3").toFile()
            try {
                whenever(getFilePreviewDownloadPathUseCase()).thenReturn(tempDir.absolutePath)
                whenever(getPreviewDownloadUseCase(node)).thenReturn(null)
                val finishedGroup = previewActionGroup("new.doc", finishedFiles = 1)
                whenever(monitorOngoingActiveTransfersUseCase(TransferType.DOWNLOAD)).thenReturn(
                    flowOf(transferResult(listOf(finishedGroup)))
                )
                whenever(insertPendingDownloadsForNodesUseCase(any(), any(), any(), any()))
                    .thenAnswer {
                        File(tempDir, "new.doc").apply {
                            createNewFile()
                            writeText("ok")
                        }
                        null
                    }

                val result = underTest(node)

                assertThat(result).isEqualTo(File(tempDir, "new.doc"))
                verify(insertPendingDownloadsForNodesUseCase).invoke(
                    nodes = eq(listOf(node)),
                    destination = any(),
                    isHighPriority = eq(true),
                    appData = eq(TransferAppData.PreviewDownload),
                )
                verifyBlocking(startDownloadsWorkerAndWaitUntilIsStartedUseCase) { invoke() }
            } finally {
                tempDir.deleteRecursively()
            }
        }

    @Test
    fun `test that invoke waits until preview group finishes and file exists`() = runTest {
        val node: TypedFileNode = mock()
        whenever(node.name).thenReturn("wait.bin")
        val tempDir = Files.createTempDirectory("preview_dest_4").toFile()
        try {
            whenever(getFilePreviewDownloadPathUseCase()).thenReturn(tempDir.absolutePath)
            whenever(getPreviewDownloadUseCase(node)).thenReturn(null)
            val inProgress = previewActionGroup("wait.bin", finishedFiles = 0)
            val finished = previewActionGroup("wait.bin", finishedFiles = 1)
            whenever(monitorOngoingActiveTransfersUseCase(TransferType.DOWNLOAD)).thenReturn(
                flow {
                    emit(transferResult(listOf(inProgress)))
                    File(tempDir, "wait.bin").apply {
                        createNewFile()
                        writeText("done")
                    }
                    emit(transferResult(listOf(finished)))
                }
            )
            whenever(insertPendingDownloadsForNodesUseCase(any(), any(), any(), any()))
                .thenAnswer { null }

            val result = underTest(node)

            assertThat(result.readText()).isEqualTo("done")
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `test that invoke throws NoSuchElementException when flow completes before file is ready`() =
        runTest {
            val node: TypedFileNode = mock()
            whenever(node.name).thenReturn("missing.txt")
            whenever(getFilePreviewDownloadPathUseCase()).thenReturn("/no/such/preview/dir/")
            whenever(getPreviewDownloadUseCase(node)).thenReturn(null)
            val finishedGroup = previewActionGroup("missing.txt", finishedFiles = 1)
            whenever(monitorOngoingActiveTransfersUseCase(TransferType.DOWNLOAD)).thenReturn(
                flowOf(transferResult(listOf(finishedGroup)))
            )
            whenever(insertPendingDownloadsForNodesUseCase(any(), any(), any(), any()))
                .thenAnswer { null }

            assertThrows<NoSuchElementException> {
                underTest(node)
            }
        }

    private fun previewActionGroup(
        fileName: String,
        finishedFiles: Int,
    ) = ActiveTransferTotals.ActionGroup(
        groupId = 1,
        totalFiles = 1,
        finishedFiles = finishedFiles,
        completedFiles = finishedFiles,
        alreadyTransferred = 0,
        destination = "/cache/preview/",
        selectedNames = listOf(fileName),
        singleTransferTag = null,
        startTime = 0L,
        pausedFiles = 0,
        totalBytes = 100L,
        transferredBytes = if (finishedFiles == 1) 100L else 0L,
        pendingTransferNodeId = null,
        appData = listOf(TransferAppData.PreviewDownload),
    )

    private fun transferResult(groups: List<ActiveTransferTotals.ActionGroup>) =
        MonitorOngoingActiveTransfersResult(
            activeTransferTotals = ActiveTransferTotals(
                transfersType = TransferType.DOWNLOAD,
                totalTransfers = groups.size,
                totalFileTransfers = groups.size,
                pausedFileTransfers = 0,
                totalFinishedTransfers = groups.count { it.finished() },
                totalFinishedFileTransfers = 0,
                totalCompletedFileTransfers = 0,
                totalBytes = 100L,
                transferredBytes = 0L,
                totalAlreadyTransferredFiles = 0,
                totalCancelled = 0,
                actionGroups = groups,
            ),
            paused = false,
            transfersOverQuota = false,
            storageOverQuota = false,
        )
}
