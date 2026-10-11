package mega.privacy.android.domain.usecase.transfers.downloads

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.DefaultTypedFileNode
import mega.privacy.android.domain.entity.node.chat.ChatDefaultFile
import mega.privacy.android.domain.entity.transfer.DownloadBatchItem
import mega.privacy.android.domain.entity.transfer.TransferAppData
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.entity.transfer.TransferQuotaState
import mega.privacy.android.domain.repository.FileSystemRepository
import mega.privacy.android.domain.repository.TransferRepository
import mega.privacy.android.domain.usecase.transfers.overquota.BroadcastTransferQuotaStateUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DownloadNodeInBatchUseCaseTest {
    private lateinit var underTest: DownloadNodeInBatchUseCase

    private val fileSystemRepository = mock<FileSystemRepository>()
    private val transferRepository = mock<TransferRepository>()
    private val broadcastTransferQuotaStateUseCase = mock<BroadcastTransferQuotaStateUseCase>()

    private val destinationPath = "destinationPath"

    @BeforeAll
    fun setup() {
        underTest = DownloadNodeInBatchUseCase(
            fileSystemRepository = fileSystemRepository,
            transferRepository = transferRepository,
            broadcastTransferQuotaStateUseCase = broadcastTransferQuotaStateUseCase,
        )
    }

    @BeforeEach
    fun cleanUp() {
        reset(
            fileSystemRepository,
            transferRepository,
            broadcastTransferQuotaStateUseCase,
        )
        whenever(transferRepository.startDownloadBatch(any())) doReturn emptyFlow()
    }

    @Test
    fun `test that invoke creates the destination directory`() = runTest {
        underTest(mock<DefaultTypedFileNode>(), destinationPath, null, false).test {
            awaitComplete()
        }

        verify(fileSystemRepository).createDirectory(destinationPath)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that invoke starts a batch with the node as its only item`(
        isHighPriority: Boolean,
    ) = runTest {
        val node = mock<DefaultTypedFileNode>()
        val appData = listOf(TransferAppData.VoiceClip)

        underTest(node, destinationPath, appData, isHighPriority).test {
            awaitComplete()
        }

        verify(transferRepository).startDownloadBatch(
            listOf(
                DownloadBatchItem(
                    node = node,
                    destinationPath = destinationPath,
                    fileName = null,
                    appData = appData,
                    isHighPriority = isHighPriority,
                )
            )
        )
    }

    @Test
    fun `test that invoke adds chat app data when the node is a chat file`() = runTest {
        val node = mock<ChatDefaultFile> {
            on { chatId } doReturn 4857L
            on { messageId } doReturn 974L
            on { messageIndex } doReturn 347
        }

        underTest(node, destinationPath, listOf(TransferAppData.VoiceClip), false).test {
            awaitComplete()
        }

        verify(transferRepository).startDownloadBatch(
            listOf(
                DownloadBatchItem(
                    node = node,
                    destinationPath = destinationPath,
                    fileName = null,
                    appData = listOf(
                        TransferAppData.VoiceClip,
                        TransferAppData.ChatDownload(4857L, 974L, 347),
                    ),
                    isHighPriority = false,
                )
            )
        )
    }

    @Test
    fun `test that invoke broadcasts the predicted quota state when the batch reports it`() =
        runTest {
            val quotaStateEvent =
                TransferEvent.TransferQuotaStateEvent(mock(), TransferQuotaState.Orange)
            whenever(transferRepository.startDownloadBatch(any())) doReturn flowOf(quotaStateEvent)

            underTest(mock<DefaultTypedFileNode>(), destinationPath, null, false).test {
                awaitComplete()
            }

            verify(broadcastTransferQuotaStateUseCase).invoke(TransferQuotaState.Orange)
        }

    @Test
    fun `test that invoke emits every batch event except the quota state`() = runTest {
        val startEvent = mock<TransferEvent.TransferStartEvent>()
        val updateEvent = mock<TransferEvent.TransferUpdateEvent>()
        val finishEvent = mock<TransferEvent.TransferFinishEvent>()
        whenever(transferRepository.startDownloadBatch(any())) doReturn flowOf(
            TransferEvent.TransferQuotaStateEvent(mock(), TransferQuotaState.Green),
            startEvent,
            updateEvent,
            finishEvent,
        )

        underTest(mock<DefaultTypedFileNode>(), destinationPath, null, false).test {
            assertThat(awaitItem()).isEqualTo(startEvent)
            assertThat(awaitItem()).isEqualTo(updateEvent)
            assertThat(awaitItem()).isEqualTo(finishEvent)
            awaitComplete()
        }
    }

    @Test
    fun `test that invoke does not broadcast when the batch reports no quota state`() = runTest {
        whenever(transferRepository.startDownloadBatch(any())) doReturn
                flowOf(mock<TransferEvent.TransferFinishEvent>())

        underTest(mock<DefaultTypedFileNode>(), destinationPath, null, false).test {
            awaitItem()
            awaitComplete()
        }

        verifyNoInteractions(broadcastTransferQuotaStateUseCase)
    }
}
