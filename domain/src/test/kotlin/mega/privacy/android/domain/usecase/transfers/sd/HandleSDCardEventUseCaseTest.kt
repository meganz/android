package mega.privacy.android.domain.usecase.transfers.sd

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.transfer.DestinationUriAndSubFolders
import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.entity.transfer.TransferAppData
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.entity.transfer.TransferStage
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.repository.FileSystemRepository
import mega.privacy.android.domain.repository.TransferRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.provider.ArgumentsSource
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.stream.Stream

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HandleSDCardEventUseCaseTest {
    private lateinit var underTest: HandleSDCardEventUseCase

    private val moveFileToSdCardUseCase = mock<MoveFileToSdCardUseCase>()
    private val fileSystemRepository = mock<FileSystemRepository>()
    private val transferRepository = mock<TransferRepository>()

    private val scope = CoroutineScope(UnconfinedTestDispatcher())

    @BeforeAll
    fun setUp() {

        underTest = HandleSDCardEventUseCase(
            moveFileToSdCardUseCase = moveFileToSdCardUseCase,
            fileSystemRepository = fileSystemRepository,
            transferRepository = transferRepository,
            scope = scope,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            moveFileToSdCardUseCase,
            fileSystemRepository,
            transferRepository
        )
    }

    @ParameterizedTest(name = "if event is {0}")
    @ArgumentsSource(TransferEventProvider::class)
    fun `test that insertOrUpdateActiveTransfer is invoked correctly for sd transfers events`(
        event: TransferEvent,
    ) = runTest {
        val transfer = mockTransfer()
        val updatedAppData = getAppDataUpdatedWithParentPath(transfer)

        whenever(fileSystemRepository.isSDCardCachePath(any())).thenReturn(false)

        underTest(event, null)

        verify(
            transferRepository,
            times(if (event is TransferEvent.TransferFinishEvent) 0 else 1)
        ).insertOrUpdateActiveTransfer(transfer.copy(appData = updatedAppData))
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

    private fun mockTransfer() =
        mock<Transfer> {
            on { nodeHandle }.thenReturn(1L)
            on { appData }.thenReturn(
                listOf(
                    TransferAppData.SdCardDownload(
                        targetPathForSDK = TARGET_PATH,
                        finalTargetUri = TARGET_PATH
                    )
                )
            )
            on { tag }.thenReturn(1)
            on { fileName }.thenReturn("name")
            on { totalBytes }.thenReturn(11L)
            on { nodeHandle }.thenReturn(1L)
            on { localPath }.thenReturn("localPath")
            on { isFolderTransfer }.thenReturn(false)
            on { transferType }.thenReturn(TransferType.DOWNLOAD)
            on { parentPath }.thenReturn("parentPath")
        }

    private fun getAppDataUpdatedWithParentPath(transfer: Transfer) =
        transfer.appData.map { data ->
            if (data is TransferAppData.SdCardDownload && data.parentPath.isNullOrEmpty()) {
                data.copy(parentPath = transfer.parentPath)
            } else {
                data
            }
        }

    private companion object {
        const val TARGET_PATH = "path"
    }

    internal class TransferEventProvider : ArgumentsProvider {

        val transfer = mock<Transfer> {
            on { nodeHandle }.thenReturn(1L)
            on { appData }.thenReturn(
                listOf(
                    TransferAppData.SdCardDownload(
                        targetPathForSDK = TARGET_PATH,
                        finalTargetUri = TARGET_PATH
                    )
                )
            )
            on { tag }.thenReturn(1)
            on { fileName }.thenReturn("name")
            on { totalBytes }.thenReturn(11L)
            on { nodeHandle }.thenReturn(1L)
            on { localPath }.thenReturn("localPath")
            on { isFolderTransfer }.thenReturn(false)
            on { transferType }.thenReturn(TransferType.DOWNLOAD)
            on { parentPath }.thenReturn("parentPath")
        }

        override fun provideArguments(context: ExtensionContext): Stream<out Arguments>? {
            return Stream.of(
                Arguments.of(TransferEvent.TransferStartEvent(transfer)),
                Arguments.of(TransferEvent.TransferFinishEvent(transfer, null)),
                Arguments.of(TransferEvent.TransferUpdateEvent(transfer)),
                Arguments.of(TransferEvent.TransferTemporaryErrorEvent(transfer, null)),
                Arguments.of(TransferEvent.TransferDataEvent(transfer, null)),
                Arguments.of(TransferEvent.TransferPaused(transfer, true)),
                Arguments.of(TransferEvent.TransferPaused(transfer, false)),
                Arguments.of(
                    TransferEvent.FolderTransferUpdateEvent(
                        transfer,
                        TransferStage.STAGE_NONE,
                        1,
                        1,
                        1,
                        null,
                        null
                    )
                ),
                Arguments.of(
                    TransferEvent.FolderTransferUpdateEvent(
                        transfer,
                        TransferStage.STAGE_SCANNING,
                        1,
                        1,
                        1,
                        null,
                        null
                    )
                ),
                Arguments.of(
                    TransferEvent.FolderTransferUpdateEvent(
                        transfer,
                        TransferStage.STAGE_CREATING_TREE,
                        1,
                        1,
                        1,
                        null,
                        null
                    )
                ),
                Arguments.of(
                    TransferEvent.FolderTransferUpdateEvent(
                        transfer,
                        TransferStage.STAGE_TRANSFERRING_FILES,
                        1,
                        1,
                        1,
                        null,
                        null
                    )
                ),
            )
        }
    }
}