package mega.privacy.android.app.presentation.meeting.chat.view.message.attachment

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.presentation.mapper.file.FileSizeStringMapper
import mega.privacy.android.app.presentation.time.mapper.DurationInSecondsTextMapper
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.core.ui.mapper.FileTypeIconMapper
import mega.privacy.android.domain.entity.Progress
import mega.privacy.android.domain.entity.StaticImageFileTypeInfo
import mega.privacy.android.domain.entity.chat.messages.PendingFileAttachmentMessage
import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.entity.transfer.TransferAppData
import mega.privacy.android.domain.usecase.transfers.chatuploads.MonitorPendingMessageTransferEventsUseCase
import mega.privacy.android.domain.usecase.transfers.chatuploads.MonitorPendingMessagesCompressionProgressUseCase
import mega.privacy.android.domain.usecase.transfers.paused.AreTransfersPausedUseCase
import mega.privacy.android.domain.usecase.transfers.paused.MonitorPausedTransfersUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import java.io.File

@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PendingAttachmentMessageViewModelTest {
    lateinit var underTest: PendingAttachmentMessageViewModel

    private val monitorPendingMessageTransferEventsUseCase =
        mock<MonitorPendingMessageTransferEventsUseCase>()
    private val monitorPausedTransfersUseCase = mock<MonitorPausedTransfersUseCase>()
    private val areTransfersPausedUseCase = mock<AreTransfersPausedUseCase>()
    private val fileSizeStringMapper = mock<FileSizeStringMapper>()
    private val durationInSecondsTextMapper = mock<DurationInSecondsTextMapper>()
    private val fileTypeIconMapper = FileTypeIconMapper()
    private val monitorPendingMessagesCompressionProgressUseCase =
        mock<MonitorPendingMessagesCompressionProgressUseCase>()

    @BeforeEach
    internal fun resetMocks() {
        reset(
            areTransfersPausedUseCase,
            fileSizeStringMapper,
            durationInSecondsTextMapper,
        )
        commonStub()
    }

    private fun commonStub() {
        whenever(monitorPendingMessageTransferEventsUseCase()) doReturn emptyFlow()
        whenever(monitorPendingMessagesCompressionProgressUseCase()) doReturn emptyFlow()
        whenever(monitorPausedTransfersUseCase()) doReturn emptyFlow()
        whenever(fileSizeStringMapper(any())).thenReturn("1 byte")
    }

    @Test
    fun `test that first ui state adds file path`() {
        setup(emptyFlow())
        val filePathOrUri = "root/file.jpg"
        val attachmentMessage = stubMessage()

        whenever(attachmentMessage.filePath) doReturn filePathOrUri

        val actual = underTest.createFirstUiState(attachmentMessage)
        assertThat(actual.previewUri).isEqualTo(filePathOrUri)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that first ui state adds isError flag`(
        expected: Boolean,
    ) {
        setup(emptyFlow())
        val attachmentMessage = stubMessage()

        whenever(attachmentMessage.isSendError()) doReturn expected

        val actual = underTest.createFirstUiState(attachmentMessage)
        assertThat(actual.isError).isEqualTo(expected)
    }

    @Test
    fun `test that transfer events updates the pending message progress`() = runTest {
        val expected = Progress(0.33f)
        val eventsFlow = MutableSharedFlow<Pair<List<Long>, Transfer>>()
        setup(eventsFlow)
        val attachmentMessage = stubMessage()
        val transfer = mock<Transfer> {
            on { appData } doReturn listOf(TransferAppData.ChatUpload(pendingMsgId))
            on { progress } doReturn expected
        }

        underTest.updateAndGetUiStateFlow(attachmentMessage).test {
            assertThat(awaitItem().loadProgress).isNull()
            eventsFlow.emit(Pair(listOf(pendingMsgId), transfer))
            assertThat(awaitItem().loadProgress).isEqualTo(expected)
        }
    }

    @Test
    fun `test that transfer events updates the pending message transfers paused property`() =
        runTest {
            val eventsFlow = MutableSharedFlow<Pair<List<Long>, Transfer>>()
            setup(eventsFlow)
            val attachmentMessage = stubMessage()
            val transfer = mock<Transfer> {
                on { appData } doReturn listOf(TransferAppData.ChatUpload(pendingMsgId))
            }

            whenever(areTransfersPausedUseCase()) doReturn true

            underTest.updateAndGetUiStateFlow(attachmentMessage).test {
                assertThat(awaitItem().areTransfersPaused).isFalse()
                eventsFlow.emit(Pair(listOf(pendingMsgId), transfer))
                assertThat(awaitItem().areTransfersPaused).isTrue()
            }
        }

    @Test
    fun `test that paused transfers updates the pending message transfers paused property`() =
        runTest {
            val eventsFlow = MutableSharedFlow<Pair<List<Long>, Transfer>>()
            setup(eventsFlow)
            val pausedFlow = MutableSharedFlow<Boolean>()
            whenever(monitorPausedTransfersUseCase()) doReturn pausedFlow
            val attachmentMessage = stubMessage()
            val transfer = mock<Transfer> {
                on { appData } doReturn listOf(TransferAppData.ChatUpload(pendingMsgId))
            }

            eventsFlow.emit(Pair(listOf(pendingMsgId), transfer))
            underTest.updateAndGetUiStateFlow(attachmentMessage).test {
                assertThat(awaitItem().areTransfersPaused).isFalse()
                pausedFlow.emit(true)
                underTest.updatePausedTransfers(true)
                assertThat(awaitItem().areTransfersPaused).isTrue()
                pausedFlow.emit(false)
                underTest.updatePausedTransfers(false)
                assertThat(awaitItem().areTransfersPaused).isFalse()
            }
        }

    @Test
    fun `test that error state is updated when a new attachment message with error is received`() =
        runTest {
            setup(emptyFlow())
            underTest.updateAndGetUiStateFlow(stubMessage()).test {
                assertThat(awaitItem().isError).isFalse()
            }
            underTest.updateAndGetUiStateFlow(stubMessage(true)).test {
                assertThat(awaitItem().isError).isTrue()
            }
        }

    @Test
    fun `test that pending messages compression progress is updated`() = runTest {
        val message = stubMessage()
        val compressionProgressFlow = MutableStateFlow<Map<Long, Progress>>(emptyMap())
        whenever(monitorPendingMessagesCompressionProgressUseCase()) doReturn compressionProgressFlow
        val expected = Progress(0.5f)
        setup(emptyFlow())

        underTest.updateAndGetUiStateFlow(message).test {
            assertThat(awaitItem().compressionProgress).isNull()
            compressionProgressFlow.emit(mapOf(message.msgId to expected))
            assertThat(awaitItem().compressionProgress).isEqualTo(expected)
        }
    }

    internal fun setup(transferEvents: Flow<Pair<List<Long>, Transfer>>) = runTest {
        whenever(monitorPendingMessageTransferEventsUseCase()) doReturn transferEvents
        underTest = PendingAttachmentMessageViewModel(
            monitorPendingMessageTransferEventsUseCase = monitorPendingMessageTransferEventsUseCase,
            monitorPausedTransfersUseCase = monitorPausedTransfersUseCase,
            areTransfersPausedUseCase = areTransfersPausedUseCase,
            fileSizeStringMapper = fileSizeStringMapper,
            durationInSecondsTextMapper = durationInSecondsTextMapper,
            fileTypeIconMapper = fileTypeIconMapper,
            monitorPendingMessagesCompressionProgressUseCase = monitorPendingMessagesCompressionProgressUseCase
        )
    }

    private fun stubMessage(isError: Boolean = false) = mock<PendingFileAttachmentMessage> {
        on { fileName } doReturn filePath
        on { fileType } doReturn StaticImageFileTypeInfo("image/jpg", "jpg")
        on { fileSize } doReturn 8435L
        on { msgId } doReturn pendingMsgId
        on { filePath } doReturn filePath
        on { file } doReturn File(filePath)
        on { isSendError() } doReturn isError
    }

    private val filePath = "file.jpg"
    private val pendingMsgId = 879L
}