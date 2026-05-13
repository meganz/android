package mega.privacy.android.domain.usecase.node

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.entity.transfer.TransferAppData
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.usecase.transfers.downloads.DownloadNodeUseCase
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
import org.mockito.kotlin.whenever
import java.io.File
import java.io.FileNotFoundException
import java.nio.file.Files

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DownloadPreviewFileForNodeAndAwaitUseCaseTest {

    private val getFilePreviewDownloadPathUseCase: GetFilePreviewDownloadPathUseCase = mock()
    private val downloadNodeUseCase: DownloadNodeUseCase = mock()

    private lateinit var underTest: DownloadPreviewFileForNodeAndAwaitUseCase

    @BeforeEach
    fun setUp() {
        reset(
            getFilePreviewDownloadPathUseCase,
            downloadNodeUseCase,
        )
        underTest = DownloadPreviewFileForNodeAndAwaitUseCase(
            getFilePreviewDownloadPathUseCase = getFilePreviewDownloadPathUseCase,
            downloadNodeUseCase = downloadNodeUseCase,
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
                verify(downloadNodeUseCase, never()).invoke(any(), any(), any(), any())
            } finally {
                tempDir.deleteRecursively()
            }
        }

    @Test
    fun `test that invoke calls download use case with preview app data when no cached file exists`() =
        runTest {
            val node: TypedFileNode = mock()
            whenever(node.name).thenReturn("new.doc")
            val tempDir = Files.createTempDirectory("preview_dest_2").toFile()
            try {
                whenever(getFilePreviewDownloadPathUseCase()).thenReturn(tempDir.absolutePath)
                val finishEvent = TransferEvent.TransferFinishEvent(
                    transfer = mock(),
                    error = null,
                )
                whenever(
                    downloadNodeUseCase(
                        node = eq(node),
                        destinationPath = eq(tempDir.absolutePath),
                        appData = eq(listOf(TransferAppData.PreviewDownload)),
                        isHighPriority = eq(true),
                    )
                ).thenReturn(
                    flow {
                        File(tempDir, "new.doc").apply {
                            createNewFile()
                            writeText("ok")
                        }
                        emit(finishEvent)
                    }
                )

                val result = underTest(node)

                assertThat(result).isEqualTo(File(tempDir, "new.doc"))
                verify(downloadNodeUseCase).invoke(
                    node = eq(node),
                    destinationPath = eq(tempDir.absolutePath),
                    appData = eq(listOf(TransferAppData.PreviewDownload)),
                    isHighPriority = eq(true),
                )
            } finally {
                tempDir.deleteRecursively()
            }
        }

    @Test
    fun `test that invoke waits for finish event before returning the file`() = runTest {
        val node: TypedFileNode = mock()
        whenever(node.name).thenReturn("wait.bin")
        val tempDir = Files.createTempDirectory("preview_dest_3").toFile()
        try {
            whenever(getFilePreviewDownloadPathUseCase()).thenReturn(tempDir.absolutePath)
            val updateTransfer: Transfer = mock()
            val finishTransfer: Transfer = mock()
            whenever(downloadNodeUseCase(any(), any(), any(), any())).thenReturn(
                flow {
                    emit(TransferEvent.TransferStartEvent(updateTransfer))
                    emit(TransferEvent.TransferUpdateEvent(updateTransfer))
                    File(tempDir, "wait.bin").apply {
                        createNewFile()
                        writeText("done")
                    }
                    emit(TransferEvent.TransferFinishEvent(finishTransfer, error = null))
                }
            )

            val result = underTest(node)

            assertThat(result.readText()).isEqualTo("done")
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `test that invoke throws FileNotFoundException when finish event reports an error`() =
        runTest {
            val node: TypedFileNode = mock()
            whenever(node.name).thenReturn("failed.bin")
            val tempDir = Files.createTempDirectory("preview_dest_4").toFile()
            try {
                whenever(getFilePreviewDownloadPathUseCase()).thenReturn(tempDir.absolutePath)
                val error: MegaException = mock()
                whenever(downloadNodeUseCase(any(), any(), any(), any())).thenReturn(
                    flowOf(TransferEvent.TransferFinishEvent(mock(), error = error))
                )

                assertThrows<FileNotFoundException> { underTest(node) }
            } finally {
                tempDir.deleteRecursively()
            }
        }

    @Test
    fun `test that invoke throws FileNotFoundException when file is not produced after finish`() =
        runTest {
            val node: TypedFileNode = mock()
            whenever(node.name).thenReturn("missing.txt")
            val tempDir = Files.createTempDirectory("preview_dest_5").toFile()
            try {
                whenever(getFilePreviewDownloadPathUseCase()).thenReturn(tempDir.absolutePath)
                whenever(downloadNodeUseCase(any(), any(), any(), any())).thenReturn(
                    flowOf(TransferEvent.TransferFinishEvent(mock(), error = null))
                )

                assertThrows<FileNotFoundException> { underTest(node) }
            } finally {
                tempDir.deleteRecursively()
            }
        }
}
