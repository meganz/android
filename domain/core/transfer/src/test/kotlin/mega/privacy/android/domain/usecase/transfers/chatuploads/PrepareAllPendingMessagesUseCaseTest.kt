package mega.privacy.android.domain.usecase.transfers.chatuploads

import app.cash.turbine.test
import com.google.common.truth.Truth
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.chat.PendingMessage
import mega.privacy.android.domain.entity.chat.PendingMessageState
import mega.privacy.android.domain.entity.chat.messages.pending.UpdatePendingMessageStateAndPathRequest
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.usecase.chat.message.MonitorPendingMessagesByStateUseCase
import mega.privacy.android.domain.usecase.chat.message.UpdatePendingMessageUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.anyValueClass
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PrepareAllPendingMessagesUseCaseTest {
    private lateinit var underTest: PrepareAllPendingMessagesUseCase

    private val monitorPendingMessagesByStateUseCase = mock<MonitorPendingMessagesByStateUseCase>()
    private val chatAttachmentNeedsCompressionUseCase =
        mock<ChatAttachmentNeedsCompressionUseCase>()
    private val updatePendingMessageUseCase = mock<UpdatePendingMessageUseCase>()

    @BeforeAll
    fun setup() {

        underTest = PrepareAllPendingMessagesUseCase(
            monitorPendingMessagesByStateUseCase,
            chatAttachmentNeedsCompressionUseCase,
            updatePendingMessageUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            monitorPendingMessagesByStateUseCase,
            chatAttachmentNeedsCompressionUseCase,
            updatePendingMessageUseCase,
        )
    }

    @Test
    fun `test that 0 is emitted and flow ends when there are no pending message in preparing state`() =
        runTest {
            whenever(monitorPendingMessagesByStateUseCase(PendingMessageState.PREPARING)) doReturn
                    flowOf(emptyList())

            underTest().test {
                val actual = awaitItem()
                Truth.assertThat(actual).isEqualTo(0)
                awaitComplete()
            }
        }

    @Test
    fun `test that the amount of pending messages in preparing state is emitted`() =
        runTest {
            val firstList = (0L..3L).map { stubPendingMessage(it) }
            val secondList = (6L..23L).map { stubPendingMessage(it) }

            whenever(chatAttachmentNeedsCompressionUseCase(anyValueClass())) doReturn true
            whenever(monitorPendingMessagesByStateUseCase(PendingMessageState.PREPARING)) doReturn
                    flow {
                        emit(firstList)
                        // need to wait to don't miss next emission, as there's a conflate() and collector uses launch and join
                        delay(1)
                        emit(secondList)
                        // no need to wait here as the last emission won't be missed
                        emit(emptyList())
                    }

            underTest().test {
                Truth.assertThat(awaitItem()).isEqualTo(firstList.size)
                Truth.assertThat(awaitItem()).isEqualTo(secondList.size)
                Truth.assertThat(awaitItem()).isEqualTo(0)
                awaitComplete()
            }
        }

    @Test
    fun `test that monitor pending messages is conflated`() =
        runTest {
            val firstList = (0L..3L).map { stubPendingMessage(it) }
            val secondList = (6L..23L).map { stubPendingMessage(it) }
            whenever(chatAttachmentNeedsCompressionUseCase(anyValueClass())) doReturn true
            whenever(monitorPendingMessagesByStateUseCase(PendingMessageState.PREPARING)) doReturn
                    flow {
                        emit(firstList)
                        emit(firstList + secondList)
                        emit(secondList)
                        delay(1)
                        emit(emptyList())
                    }

            underTest().test {
                Truth.assertThat(awaitItem()).isEqualTo(firstList.size)
                Truth.assertThat(awaitItem()).isEqualTo(secondList.size)
                Truth.assertThat(awaitItem()).isEqualTo(0)
                awaitComplete()
            }
        }

    @Test
    fun `test that pending message is set to compressing state when file needs compression`() =
        runTest {
            val pendingMessage = stubPendingMessage()

            whenever(monitorPendingMessagesByStateUseCase(PendingMessageState.PREPARING)) doReturn
                    flowOf(listOf(pendingMessage))
            whenever(chatAttachmentNeedsCompressionUseCase(anyValueClass())) doReturn true

            underTest().test { cancelAndConsumeRemainingEvents() }

            verify(updatePendingMessageUseCase)(
                UpdatePendingMessageStateAndPathRequest(
                    pendingMessage.id,
                    PendingMessageState.COMPRESSING,
                    pendingMessage.uriPath.value,
                )
            )
            verifyNoMoreInteractions(updatePendingMessageUseCase)
        }

    @Test
    fun `test that pending message is set to ready to upload state when file does not needs compression`() =
        runTest {
            val pendingMessage = stubPendingMessage()
            whenever(monitorPendingMessagesByStateUseCase(PendingMessageState.PREPARING)) doReturn
                    flowOf(listOf(pendingMessage))
            whenever(chatAttachmentNeedsCompressionUseCase(anyValueClass())) doReturn false

            underTest().test { cancelAndConsumeRemainingEvents() }

            verify(updatePendingMessageUseCase)(
                UpdatePendingMessageStateAndPathRequest(
                    pendingMessage.id,
                    PendingMessageState.READY_TO_UPLOAD,
                    pendingMessage.uriPath.value,
                )
            )
            verifyNoMoreInteractions(updatePendingMessageUseCase)
        }

    @Test
    fun `test that multiple pending messages are updated`() =
        runTest {
            val pendingMessages = (0L..3L).map { stubPendingMessage(it) }

            whenever(chatAttachmentNeedsCompressionUseCase(anyValueClass())) doReturn true
            whenever(monitorPendingMessagesByStateUseCase(PendingMessageState.PREPARING)) doReturn
                    flowOf(pendingMessages)
            whenever(chatAttachmentNeedsCompressionUseCase(any())) doReturn false

            underTest().test { cancelAndConsumeRemainingEvents() }

            pendingMessages.forEach {
                verify(updatePendingMessageUseCase)(
                    UpdatePendingMessageStateAndPathRequest(
                        it.id,
                        PendingMessageState.READY_TO_UPLOAD,
                        it.uriPath.value,
                    )
                )
            }
        }

    private fun stubPendingMessage(
        id: Long = 12L,
        fileName: String = "pendingMessage$id.mp4",
    ) = mock<PendingMessage> {
        on { this.id } doReturn id
        on { this.name } doReturn "pendingMessage$id name"
        on { this.uriPath } doReturn UriPath("/path/$fileName")
    }
}