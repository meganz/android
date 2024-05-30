package mega.privacy.android.domain.usecase.chat.message.retry

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.chat.PendingMessageState
import mega.privacy.android.domain.entity.chat.messages.AttachmentMessage
import mega.privacy.android.domain.entity.chat.messages.PendingFileAttachmentMessage
import mega.privacy.android.domain.entity.chat.messages.PendingVoiceClipMessage
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.chat.message.AttachNodeWithPendingMessageUseCase
import mega.privacy.android.domain.usecase.transfers.chatuploads.GetOrCreateMyChatsFilesFolderIdUseCase
import mega.privacy.android.domain.usecase.transfers.chatuploads.StartChatUploadsWithWorkerUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.File

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RetryPendingMessageUseCaseTest {
    private lateinit var underTest: RetryPendingMessageUseCase

    private val startChatUploadsWithWorkerUseCase = mock<StartChatUploadsWithWorkerUseCase>()
    private val attachNodeWithPendingMessageUseCase = mock<AttachNodeWithPendingMessageUseCase>()
    private val getOrCreateMyChatsFilesFolderIdUseCase = mock<GetOrCreateMyChatsFilesFolderIdUseCase>()


    @BeforeAll
    internal fun setUp() {
        underTest = RetryPendingMessageUseCase(
            startChatUploadsWithWorkerUseCase,
            attachNodeWithPendingMessageUseCase,
            getOrCreateMyChatsFilesFolderIdUseCase,
        )
    }

    @BeforeEach
    internal fun resetMocks() =
        reset(
            startChatUploadsWithWorkerUseCase,
            attachNodeWithPendingMessageUseCase,
        )

    @Test
    fun `test that can resend return true for pending file attachment message`() = runTest {
        assertThat(underTest.canRetryMessage(mock<PendingFileAttachmentMessage>())).isTrue()
    }

    @Test
    fun `test that can resend return true for pending voice clip message`() = runTest {
        assertThat(underTest.canRetryMessage(mock<PendingVoiceClipMessage>())).isTrue()
    }

    @Test
    fun `test that can resend return false for general attachment message`() = runTest {
        assertThat(underTest.canRetryMessage(mock<AttachmentMessage>())).isFalse()
    }

    @Test
    fun `test that general attachment messages throws an exception`() = runTest {
        assertThrows<IllegalArgumentException> { underTest(mock<AttachmentMessage>()) }
    }

    @ParameterizedTest
    @EnumSource(
        PendingMessageState::class,
        names = ["ERROR_UPLOADING", "ERROR_ATTACHING"],
        mode = EnumSource.Mode.EXCLUDE
    )
    fun `test that pending attachment message throws an exception when it is not in error state`(
        state: PendingMessageState,
    ) = runTest {
        assertThrows<IllegalArgumentException> {
            underTest(mock<PendingFileAttachmentMessage> {
                on { it.state } doReturn state
            })
        }
    }

    @Test
    fun `test that startChatUploadsWithWorkerUseCase is invoked when message is in error uploading state`() =
        runTest {
            val file = mock<File>()
            val msgId = 15L
            val myChatFilesFolderId = NodeId(11L)
            val message = mock<PendingFileAttachmentMessage> {
                on { it.state } doReturn PendingMessageState.ERROR_UPLOADING
                on { it.file } doReturn file
                on { it.msgId } doReturn msgId
            }
            whenever(
                startChatUploadsWithWorkerUseCase(
                    any(),
                    NodeId(any()),
                    any()
                )
            ) doReturn emptyFlow()
            whenever(getOrCreateMyChatsFilesFolderIdUseCase()) doReturn myChatFilesFolderId

            underTest(message)

            verify(startChatUploadsWithWorkerUseCase).invoke(file, myChatFilesFolderId, msgId)
        }

    @Test
    fun `test that attachNodeWithPendingMessageUseCase is invoked when message is in error attaching state`() =
        runTest {
            val nodeId = NodeId(11L)
            val msgId = 15L
            val message = mock<PendingFileAttachmentMessage> {
                on { it.state } doReturn PendingMessageState.ERROR_ATTACHING
                on { it.nodeId } doReturn nodeId
                on { it.msgId } doReturn msgId
            }

            underTest(message)

            verify(attachNodeWithPendingMessageUseCase).invoke(msgId, nodeId)
        }

    @Test
    fun `test that message in error attaching state throws an exception when it has no node id`() =
        runTest {
            assertThrows<IllegalArgumentException> {
                underTest(mock<PendingFileAttachmentMessage> {
                    on { it.state } doReturn PendingMessageState.ERROR_ATTACHING
                    on { it.nodeId } doReturn null
                })
            }
        }
}