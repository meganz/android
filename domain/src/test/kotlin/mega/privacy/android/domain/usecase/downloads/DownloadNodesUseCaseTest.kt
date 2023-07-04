package mega.privacy.android.domain.usecase.downloads

import app.cash.turbine.Event
import app.cash.turbine.test
import com.google.common.truth.Truth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.transfer.FinishProcessingTransfers
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.entity.transfer.TransferStage
import mega.privacy.android.domain.exception.node.NodeDoesNotExistsException
import mega.privacy.android.domain.repository.CancelTokenRepository
import mega.privacy.android.domain.repository.FileSystemRepository
import mega.privacy.android.domain.repository.TransferRepository
import mega.privacy.android.domain.usecase.canceltoken.CancelCancelTokenUseCase
import mega.privacy.android.domain.usecase.canceltoken.InvalidateCancelTokenUseCase
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DownloadNodesUseCaseTest {

    private val transferRepository: TransferRepository = mock()
    private val cancelTokenRepository: CancelTokenRepository = mock()
    private val fileNode: TypedFileNode = mock()
    private val folderNode: TypedFolderNode = mock()
    private val cancelCancelTokenUseCase: CancelCancelTokenUseCase = mock()
    private val invalidateCancelTokenUseCase: InvalidateCancelTokenUseCase = mock()
    private val fileSystemRepository: FileSystemRepository = mock()

    private lateinit var underTest: DownloadNodesUseCase

    @BeforeAll
    fun setup() {
        underTest =
            DownloadNodesUseCase(
                cancelCancelTokenUseCase,
                invalidateCancelTokenUseCase,
                transferRepository,
                fileSystemRepository,
            )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            transferRepository, cancelTokenRepository, fileSystemRepository,
            fileNode, folderNode, invalidateCancelTokenUseCase, cancelCancelTokenUseCase
        )
    }

    @AfterAll
    fun tearDown() {
        Dispatchers.resetMain()
    }


    @ParameterizedTest(name = "priority: {0}")
    @ValueSource(booleans = [true, false])
    fun `test that repository is called with the proper priority`(priority: Boolean) = runTest {
        underTest(nodeIds, DESTINATION_PATH_FOLDER, null, priority).test {
            nodeIds.forEach { nodeId ->
                verify(transferRepository).startDownload(
                    nodeId,
                    DESTINATION_PATH_FOLDER,
                    null,
                    priority,
                )
            }
            awaitComplete()
        }
    }

    @ParameterizedTest(name = "appdata: \"{0}\"")
    @ValueSource(strings = ["test1", "", " ", "some space"])
    fun `test that repository is called with the proper appData`(appData: String?) = runTest {
        underTest(listOf(nodeId), DESTINATION_PATH_FOLDER, appData, false).test {
            verify(transferRepository).startDownload(
                nodeId,
                DESTINATION_PATH_FOLDER,
                appData,
                false

            )
            awaitComplete()
        }
    }

    @Test
    fun `test that repository start download is invoked for each nodeId when start download is invoked`() =
        runTest {
            underTest(nodeIds, DESTINATION_PATH_FOLDER, null, false).test {
                nodeIds.forEach { nodeId ->
                    verify(transferRepository).startDownload(
                        nodeId, DESTINATION_PATH_FOLDER, null, false,
                    )
                }
                awaitComplete()
            }
        }

    @Test
    fun `test that cancel token is canceled when start download flow is canceled`() =
        runTest {
            nodeIds.forEach {
                whenever(
                    transferRepository.startDownload(
                        it, DESTINATION_PATH_FOLDER, null, false,
                    )
                ).thenReturn(
                    flow { delay(100) }
                )
            }
            underTest(nodeIds, DESTINATION_PATH_FOLDER, null, false).test {
                cancel()
                verify(cancelCancelTokenUseCase).invoke()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that cancel token is not invalidated when start download flow is canceled before completation`() =
        runTest {
            nodeIds.forEach {
                whenever(
                    transferRepository.startDownload(
                        it, DESTINATION_PATH_FOLDER, null, false,
                    )
                ).thenReturn(
                    flow { delay(100) }
                )
            }
            underTest(nodeIds, DESTINATION_PATH_FOLDER, null, false).test {
                cancel()
                verify(invalidateCancelTokenUseCase, never()).invoke()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that cancel token is not canceled if start download flow is not completed`() =
        runTest {
            nodeIds.forEach {
                whenever(
                    transferRepository.startDownload(
                        it, DESTINATION_PATH_FOLDER, null, false,
                    )
                ).thenReturn(
                    flow { delay(100) }
                )
            }
            underTest(nodeIds, DESTINATION_PATH_FOLDER, null, false).test {
                verify(cancelCancelTokenUseCase, never()).invoke()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that transfer events are emitted when each transfer is updated`() = runTest {
        nodeIds.forEach {
            whenever(
                transferRepository.startDownload(
                    it, DESTINATION_PATH_FOLDER, null, false,
                )
            ).thenReturn(
                flowOf(
                    mock<TransferEvent.TransferStartEvent>(),
                    mock<TransferEvent.TransferUpdateEvent>(),
                    mock<TransferEvent.TransferFinishEvent>(),
                )
            )
        }
        underTest(nodeIds, DESTINATION_PATH_FOLDER, null, false).test {
            repeat(nodeIds.size) {
                Truth.assertThat(awaitItem())
                    .isInstanceOf(TransferEvent.TransferStartEvent::class.java)
                Truth.assertThat(awaitItem())
                    .isInstanceOf(TransferEvent.TransferUpdateEvent::class.java)
                Truth.assertThat(awaitItem())
                    .isInstanceOf(TransferEvent.TransferFinishEvent::class.java)
            }
            awaitComplete()
        }
    }


    @Test
    fun `test that finish processing is emitted when all transfers are processed`() = runTest {

        nodeIds.forEachIndexed { index, nodeId ->
            stubFinishProcessingEvent(index.mod(2) == 0, nodeId)
        }

        underTest(nodeIds, DESTINATION_PATH_FOLDER, null, false).test {
            Truth.assertThat(cancelAndConsumeRemainingEvents().mapNotNull { event ->
                (event as? Event.Item)?.value?.takeIf { it is FinishProcessingTransfers }
            }).hasSize(1)
        }
    }

    @Test
    fun `test that finish processing is emitted when all transfers are processed including not found nodes`() =
        runTest {
            nodeIds.forEachIndexed { index, nodeId ->
                if (index == 5) {
                    whenever(
                        transferRepository.startDownload(
                            nodeId, DESTINATION_PATH_FOLDER, null, false,
                        )
                    ).thenAnswer {
                        throw NodeDoesNotExistsException()
                    }
                } else {
                    stubFinishProcessingEvent(index.mod(2) == 0, nodeId)
                }
            }

            underTest(nodeIds, DESTINATION_PATH_FOLDER, null, false).test {
                Truth.assertThat(cancelAndConsumeRemainingEvents().mapNotNull { event ->
                    (event as? Event.Item)?.value?.takeIf { it is FinishProcessingTransfers }
                }).hasSize(1)
            }
        }

    @Test
    fun `test that destination folder is created`() = runTest {
        underTest(nodeIds, DESTINATION_PATH_FOLDER, null, false).test {
            cancelAndIgnoreRemainingEvents()
        }
        verify(fileSystemRepository).createDirectory(DESTINATION_PATH_FOLDER)
    }

    @Test
    fun `test that cancel token is invalidated when all transfers are processed`() = runTest {

        nodeIds.forEachIndexed { index, nodeId ->
            stubFinishProcessingEvent(index.mod(2) == 0, nodeId)
        }

        underTest(nodeIds, DESTINATION_PATH_FOLDER, null, false).test {
            cancelAndIgnoreRemainingEvents()
        }
        verify(invalidateCancelTokenUseCase).invoke()
    }

    @Test
    fun `test that finish processing is not emitted if not all transfers are processed`() =
        runTest {

            nodeIds.dropLast(1).forEachIndexed { index, nodeId ->
                stubFinishProcessingEvent(index.mod(2) == 0, nodeId)
            }

            underTest(nodeIds, DESTINATION_PATH_FOLDER, null, false).test {
                Truth.assertThat(cancelAndConsumeRemainingEvents().mapNotNull { event ->
                    (event as? Event.Item)?.value?.takeIf { it is FinishProcessingTransfers }
                }).isEmpty()
            }
        }

    private fun stubFinishProcessingEvent(folder: Boolean, nodeId: NodeId) {
        whenever(
            transferRepository.startDownload(
                nodeId, DESTINATION_PATH_FOLDER, null, false,
            )
        ).thenAnswer {
            flowOf(
                //even -> files, odd -> folders
                if (folder) {
                    TransferEvent.TransferUpdateEvent(mock {
                        on { isFolderTransfer }.thenReturn(true)
                        on { stage }.thenReturn(TransferStage.STAGE_TRANSFERRING_FILES)
                        on { nodeHandle }.thenReturn(nodeId.longValue)
                    })
                } else {
                    TransferEvent.TransferStartEvent(mock {
                        on { isFolderTransfer }.thenReturn(false)
                        on { nodeHandle }.thenReturn(nodeId.longValue)
                    })
                }
            )
        }
    }


    companion object {
        private val nodeId = NodeId(1L)
        private val nodeIds = (0L..10L).map { NodeId(it) }
        private const val DESTINATION_PATH_FOLDER = "root/parent/destination"
    }
}