package mega.privacy.android.app.presentation.meeting.chat.view.message.attachment

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.presentation.mapper.file.FileSizeStringMapper
import mega.privacy.android.app.presentation.time.mapper.DurationInSecondsTextMapper
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.Progress
import mega.privacy.android.domain.entity.StaticImageFileTypeInfo
import mega.privacy.android.domain.entity.chat.messages.PendingFileAttachmentMessage
import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.entity.transfer.TransferAppData
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.usecase.transfers.MonitorTransferEventsUseCase
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

    private val monitorTransferEventsUseCase = mock<MonitorTransferEventsUseCase>()
    private val fileSizeStringMapper = mock<FileSizeStringMapper>()
    private val durationInSecondsTextMapper = mock<DurationInSecondsTextMapper>()

    @BeforeEach
    internal fun resetMocks() {
        reset(
            monitorTransferEventsUseCase,
            fileSizeStringMapper,
            durationInSecondsTextMapper,
        )
        commonStub()
    }

    private fun commonStub() {
        whenever(fileSizeStringMapper(any())).thenReturn("1 byte")
    }

    @Test
    fun `test that first ui state adds file path`() {
        setup(emptyFlow())
        val path = "root/file.jpg"
        val attachmentMessage = stubMessage()
        whenever(attachmentMessage.file) doReturn File(path)
        val actual = underTest.createFirstUiState(attachmentMessage)
        assertThat(actual.previewUri).endsWith(path)
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
        val eventsFlow = MutableSharedFlow<TransferEvent>()
        setup(eventsFlow)
        val attachmentMessage = stubMessage()
        val transfer = mock<Transfer> {
            on { appData } doReturn listOf(TransferAppData.ChatUpload(pendingMsgId))
            on { progress } doReturn expected
        }
        underTest.updateAndGetUiStateFlow(attachmentMessage).test {
            assertThat(awaitItem().loadProgress).isNull()
            eventsFlow.emit(TransferEvent.TransferUpdateEvent(transfer))
            assertThat(awaitItem().loadProgress).isEqualTo(expected)
        }
    }

    @Test
    fun `test that error state is updated when a new attachment message with error is received`() =
        runTest {
            underTest.updateAndGetUiStateFlow(stubMessage()).test {
                assertThat(awaitItem().isError).isFalse()
            }
            underTest.updateAndGetUiStateFlow(stubMessage(true)).test {
                assertThat(awaitItem().isError).isTrue()
            }
        }

    internal fun setup(transferEvents: Flow<TransferEvent>) = runTest {
        whenever(monitorTransferEventsUseCase()) doReturn transferEvents
        underTest = PendingAttachmentMessageViewModel(
            monitorTransferEventsUseCase,
            fileSizeStringMapper,
            durationInSecondsTextMapper,
        )
    }

    private fun stubMessage(isError: Boolean = false) = mock<PendingFileAttachmentMessage> {
        on { fileName } doReturn "file.jpg"
        on { fileType } doReturn StaticImageFileTypeInfo("image/jpg", "jpg")
        on { fileSize } doReturn 8435L
        on { msgId } doReturn pendingMsgId
        on { file } doReturn File("file.jpg")
        on { isSendError() } doReturn isError
    }

    private val pendingMsgId = 879L
}