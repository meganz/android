package mega.privacy.android.domain.usecase.transfers.uploads

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.transfer.MultiTransferEvent
import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.entity.transfer.TransferAppData
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.entity.transfer.UploadFileInfo
import mega.privacy.android.domain.repository.CacheRepository
import mega.privacy.android.domain.repository.CancelTokenRepository
import mega.privacy.android.domain.repository.FileSystemRepository
import mega.privacy.android.domain.repository.TransferRepository
import mega.privacy.android.domain.usecase.canceltoken.CancelCancelTokenUseCase
import mega.privacy.android.domain.usecase.canceltoken.InvalidateCancelTokenUseCase
import mega.privacy.android.domain.usecase.transfers.MonitorTransferEventsUseCase
import mega.privacy.android.domain.usecase.transfers.active.HandleTransferEventUseCase
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.internal.verification.Times
import org.mockito.kotlin.anyVararg
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UploadFilesUseCaseTest {

    private val cancelCancelTokenUseCase = mock<CancelCancelTokenUseCase>()
    private val invalidateCancelTokenUseCase = mock<InvalidateCancelTokenUseCase>()
    private val handleTransferEventUseCase = mock<HandleTransferEventUseCase>()
    private val monitorTransferEventsUseCase = mock<MonitorTransferEventsUseCase>()
    private val transferRepository = mock<TransferRepository>()
    private val cancelTokenRepository = mock<CancelTokenRepository>()
    private val fileNode = mock<TypedFileNode>()
    private val fileSystemRepository = mock<FileSystemRepository>()
    private val transfer = mock<Transfer>()
    private val cacheRepository = mock<CacheRepository>()

    private lateinit var underTest: UploadFilesUseCase

    @BeforeAll
    fun setup() {

        underTest =
            UploadFilesUseCase(
                cancelCancelTokenUseCase = cancelCancelTokenUseCase,
                invalidateCancelTokenUseCase = invalidateCancelTokenUseCase,
                handleTransferEventUseCase = handleTransferEventUseCase,
                monitorTransferEventsUseCase = monitorTransferEventsUseCase,
                transferRepository = transferRepository,
                cacheRepository = cacheRepository
            )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            transferRepository, cancelTokenRepository, fileSystemRepository,
            handleTransferEventUseCase, fileNode, invalidateCancelTokenUseCase,
            cancelCancelTokenUseCase, transfer,
            monitorTransferEventsUseCase, cacheRepository,
        )
        commonStub()
    }

    private fun commonStub() {
        whenever(monitorTransferEventsUseCase()).thenReturn(emptyFlow())
    }

    @AfterAll
    fun tearDown() {
        Dispatchers.resetMain()
    }


    @ParameterizedTest(name = "priority: {0}")
    @ValueSource(booleans = [true, false])
    fun `test that repository start upload is called with the proper priority`(priority: Boolean) =
        runTest {
            underTest(listOf(UploadFileInfo(file, null)), parentId, priority).test {

                verify(transferRepository).startUpload(
                    ABSOLUTE_PATH,
                    parentId,
                    null,
                    null,
                    null,
                    false,
                    priority,
                )
                awaitComplete()
            }
        }

    @ParameterizedTest(name = "priority: {0}")
    @ValueSource(booleans = [true, false])
    fun `test that repository start upload is called with the proper isSourceTemporary`(
        isSourceTemporary: Boolean,
    ) =
        runTest {
            whenever(cacheRepository.isFileInCacheDirectory(argThat { this.absolutePath == ABSOLUTE_PATH })) doReturn isSourceTemporary
            underTest(listOf(UploadFileInfo(file, null)), parentId, false).test {

                verify(transferRepository).startUpload(
                    ABSOLUTE_PATH,
                    parentId,
                    null,
                    null,
                    null,
                    isSourceTemporary,
                    false,
                )
                awaitComplete()
            }
        }

    @ParameterizedTest(name = "appdata: \"{0}\"")
    @MethodSource("provideAppDataExceptChat")
    fun `test that repository start upload is called with the proper appData when appData is not a chat upload`(
        appData: List<TransferAppData>?,
    ) = runTest {
        underTest(
            listOf(UploadFileInfo(file, null, appData)), parentId, isHighPriority = false,
        ).test {
            verify(transferRepository).startUpload(
                ABSOLUTE_PATH,
                parentId,
                null,
                null,
                appData,
                isSourceTemporary = false,
                shouldStartFirst = false,
            )
            awaitComplete()
        }
    }

    @Test
    fun `test that repository start upload is called with the proper appData for each file`() =
        runTest {
            val uploadFileInfos = fileNodesAndNullNames.mapIndexed { index, it ->
                it.copy(appData = (0..index).map { mock<TransferAppData.SdCardDownload>() })
            }
            underTest(
                uploadFileInfos, parentId, isHighPriority = false,
            ).test {
                uploadFileInfos.forEach {
                    verify(transferRepository).startUpload(
                        it.uriPath.value,
                        parentId,
                        null,
                        null,
                        it.appData,
                        isSourceTemporary = false,
                        shouldStartFirst = false,
                    )
                }
                awaitComplete()
            }
        }

    @ParameterizedTest(name = "appdata: \"{0}\"")
    @MethodSource("provideChatAppData")
    fun `test that repository start upload for chat is called when appData is chat transfer app data`(
        appData: List<TransferAppData.ChatUploadAppData>,
    ) = runTest {
        underTest(
            listOf(UploadFileInfo(file, null, appData)), parentId,
            isHighPriority = false
        ).test {
            verify(transferRepository).startUploadForChat(
                ABSOLUTE_PATH,
                parentId,
                null,
                appData,
                isSourceTemporary = false,
            )
            awaitComplete()
        }
    }

    @ParameterizedTest(name = "priority: {0}")
    @ValueSource(booleans = [true, false])
    fun `test that repository start upload for chat is called with the proper isSourceTemporary`(
        isSourceTemporary: Boolean,
    ) =
        runTest {
            whenever(cacheRepository.isFileInCacheDirectory(argThat { this.absolutePath == ABSOLUTE_PATH })) doReturn isSourceTemporary
            val chatAppData = listOf(TransferAppData.ChatUpload(12345L))
            underTest(listOf(UploadFileInfo(file, null, chatAppData)), parentId, false).test {

                verify(transferRepository).startUploadForChat(
                    ABSOLUTE_PATH,
                    parentId,
                    null,
                    chatAppData,
                    isSourceTemporary = isSourceTemporary,
                )
                awaitComplete()
            }
        }


    private fun provideAppDataExceptChat() = listOf(
        listOf(TransferAppData.BackgroundTransfer),
        listOf(TransferAppData.SdCardDownload("target", null)),
        listOf(TransferAppData.CameraUpload),
        listOf(TransferAppData.CameraUpload, TransferAppData.SdCardDownload("target", null)),
        listOf(null),
    )

    private fun provideChatAppData() = listOf(
        listOf(TransferAppData.ChatUpload(12345L)),
        listOf(TransferAppData.VoiceClip),
        listOf(TransferAppData.ChatUpload(234L), TransferAppData.ChatUpload(345L)),
        listOf(TransferAppData.ChatUpload(234L), TransferAppData.VoiceClip),
    )


    @Test
    fun `test that repository start upload is invoked for each nodeId when start upload is invoked`() =
        runTest {
            underTest(
                fileNodesAndNullNames, parentId,
                isHighPriority = false,
            ).test {
                fileNodesAndNullNames.forEach { uploadFileInfo ->
                    verify(transferRepository).startUpload(
                        uploadFileInfo.uriPath.value,
                        parentId,
                        null,
                        null,
                        null,
                        isSourceTemporary = false,
                        shouldStartFirst = false,
                    )
                }
                awaitComplete()
            }
        }


    @Test
    fun `test that cancel token is canceled when start upload flow is canceled`() =
        runTest {
            stubDelay()
            underTest(
                fileNodesAndNullNames, parentId,
                isHighPriority = false,
            ).test {
                cancel()
                verify(cancelCancelTokenUseCase).invoke()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that cancel token is not invalidated when start upload flow is canceled before completion`() =
        runTest {
            stubDelay()
            underTest(
                fileNodesAndNullNames, parentId,
                isHighPriority = false,
            ).test {
                cancel()
                verify(invalidateCancelTokenUseCase, never()).invoke()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that cancel token is not canceled if start upload flow is not completed`() =
        runTest {
            stubDelay()
            underTest(
                fileNodesAndNullNames, parentId,
                isHighPriority = false,
            ).test {
                verify(cancelCancelTokenUseCase, never()).invoke()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that transfer single node events are emitted when each transfer is updated`() =
        runTest {
            stubSingleEvents()
            underTest(
                fileNodesAndNullNames, parentId,
                isHighPriority = false,
            )
                .filterIsInstance<MultiTransferEvent.SingleTransferEvent>().test {
                    repeat(fileNodesAndNullNames.size) {
                        assertThat(awaitItem().transferEvent)
                            .isInstanceOf(TransferEvent.TransferStartEvent::class.java)
                        assertThat(awaitItem().transferEvent)
                            .isInstanceOf(TransferEvent.TransferUpdateEvent::class.java)
                        assertThat(awaitItem().transferEvent)
                            .isInstanceOf(TransferEvent.TransferFinishEvent::class.java)
                    }
                    awaitComplete()
                }
        }


    @Test
    fun `test that addOrUpdateActiveTransferUseCase is invoked when each transfer is updated`() =
        runTest {
            stubSingleEvents()
            underTest(
                fileNodesAndNullNames, parentId,
                isHighPriority = false,
            )
                .filterIsInstance<MultiTransferEvent.SingleTransferEvent>().test {
                    cancelAndConsumeRemainingEvents()
                }
            verify(
                handleTransferEventUseCase,
                Times(fileNodesAndNullNames.size * 3)
            ).invoke(anyVararg())
        }

    @Test
    fun `test that fileName is used in startUpload when is not null`() = runTest {
        val name = "RenamedFile"
        underTest(listOf(UploadFileInfo(file, name)), parentId, false).test {

            verify(transferRepository).startUpload(
                ABSOLUTE_PATH,
                parentId,
                name,
                null,
                null,
                false,
                false,
            )
            awaitComplete()
        }
    }

    @ParameterizedTest(name = "appdata: \"{0}\"")
    @MethodSource("provideChatAppData")
    fun `test that fileName is used in startUploadForChat when is not null`(
        appData: List<TransferAppData.ChatUploadAppData>,
    ) = runTest {
        val name = "RenamedFile"
        underTest(listOf(UploadFileInfo(file, name, appData)), parentId, false).test {
            verify(transferRepository).startUploadForChat(
                ABSOLUTE_PATH,
                parentId,
                name,
                appData,
                isSourceTemporary = false,
            )
            awaitComplete()
        }
    }

    private fun stubDelay() {
        fileNodesAndNullNames.forEach { fiuploadFileInfoe ->
            whenever(
                transferRepository.startUpload(
                    fiuploadFileInfoe.uriPath.value,
                    parentId,
                    null,
                    null,
                    null,
                    isSourceTemporary = false,
                    shouldStartFirst = false,
                )
            ).thenReturn(
                flow { delay(100) }
            )
        }
    }

    private fun stubSingleEvents() {
        whenever(transfer.isFolderTransfer).thenReturn(false)
        val transferEventFlow = flowOf(
            mock<TransferEvent.TransferStartEvent> { on { it.transfer }.thenReturn(transfer) },
            mock<TransferEvent.TransferUpdateEvent> { on { it.transfer }.thenReturn(transfer) },
            mock<TransferEvent.TransferFinishEvent> { on { it.transfer }.thenReturn(transfer) },
        )
        fileNodesAndNullNames.forEach { uploadFileInfo ->
            whenever(
                transferRepository.startUpload(
                    uploadFileInfo.uriPath.value,
                    parentId,
                    null,
                    null,
                    null,
                    isSourceTemporary = false,
                    shouldStartFirst = false,
                )
            ).thenReturn(transferEventFlow)
        }
    }


    companion object {
        private val file = mock<File> {
            on { name }.thenReturn(FILE_NAME)
            on { path }.thenReturn(ABSOLUTE_PATH)
            on { absolutePath }.thenReturn(ABSOLUTE_PATH)
        }
        private val fileNodesAndNullNames = (0L..10L).map { nodeId ->
            UploadFileInfo(
                mock<File> {
                    on { name }.thenReturn("$FILE_NAME$nodeId")
                    on { path }.thenReturn("$ABSOLUTE_PATH$nodeId")
                    on { absolutePath }.thenReturn("$ABSOLUTE_PATH$nodeId")
                },
                null
            )
        }
        private val parentId = NodeId(1L)

        private const val ABSOLUTE_PATH = "/root/parent/destination/File"
        private const val FILE_NAME = "File"
    }
}