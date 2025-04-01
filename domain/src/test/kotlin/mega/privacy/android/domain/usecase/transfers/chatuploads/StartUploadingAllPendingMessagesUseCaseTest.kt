package mega.privacy.android.domain.usecase.transfers.chatuploads

import app.cash.turbine.test
import com.google.common.truth.Truth
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.chat.PendingMessage
import mega.privacy.android.domain.entity.chat.PendingMessageState
import mega.privacy.android.domain.entity.chat.messages.pending.UpdatePendingMessageStateRequest
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.repository.FileSystemRepository
import mega.privacy.android.domain.usecase.chat.message.MonitorPendingMessagesByStateUseCase
import mega.privacy.android.domain.usecase.chat.message.UpdatePendingMessageUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import java.io.File

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StartUploadingAllPendingMessagesUseCaseTest {
    private lateinit var underTest: StartUploadingAllPendingMessagesUseCase

    private val monitorPendingMessagesByStateUseCase = mock<MonitorPendingMessagesByStateUseCase>()
    private val updatePendingMessageUseCase = mock<UpdatePendingMessageUseCase>()
    private val startChatUploadAndWaitScanningFinishedUseCase =
        mock<StartChatUploadAndWaitScanningFinishedUseCase>()

    @BeforeAll
    fun setup() {


        underTest = StartUploadingAllPendingMessagesUseCase(
            startChatUploadAndWaitScanningFinishedUseCase,
            monitorPendingMessagesByStateUseCase,
            updatePendingMessageUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            startChatUploadAndWaitScanningFinishedUseCase,
            monitorPendingMessagesByStateUseCase,
            updatePendingMessageUseCase,
        )
    }

    @Test
    fun `test that 0 is emitted and flow ends when there are no pending message in ready to upload state`() =
        runTest {
            whenever(monitorPendingMessagesByStateUseCase(PendingMessageState.READY_TO_UPLOAD)) doReturn
                    flowOf(emptyList())

            underTest().test {
                val actual = awaitItem()
                Truth.assertThat(actual).isEqualTo(0)
                awaitComplete()
            }
        }

    @Test
    fun `test that the amount of pending messages in ready to upload state is emitted`() =
        runTest {
            val firstList = (0L..3L).map { stubPendingMessage(it) }
            val secondList = (6L..23L).map { stubPendingMessage(it) }
            whenever(monitorPendingMessagesByStateUseCase(PendingMessageState.READY_TO_UPLOAD)) doReturn
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
    fun `test that pending message starts uploading`() =
        runTest {
            val pendingMessage = stubPendingMessage()
            val file = File("/video.mp4")
            val fileName = pendingMessage.name
            whenever(monitorPendingMessagesByStateUseCase(PendingMessageState.READY_TO_UPLOAD)) doReturn
                    flowOf(listOf(pendingMessage))

            underTest().test { cancelAndConsumeRemainingEvents() }

            verify(startChatUploadAndWaitScanningFinishedUseCase).invoke(
                pendingMessage.uriPath,
                fileName,
                listOf(pendingMessage.id)
            )
        }

    @Test
    fun `test that pending message state is updated after start uploading the file`() =
        runTest {
            val pendingMessage = stubPendingMessage()
            whenever(monitorPendingMessagesByStateUseCase(PendingMessageState.READY_TO_UPLOAD)) doReturn
                    flowOf(listOf(pendingMessage))

            underTest().test { cancelAndConsumeRemainingEvents() }

            verify(updatePendingMessageUseCase)(
                UpdatePendingMessageStateRequest(
                    pendingMessage.id,
                    PendingMessageState.UPLOADING,
                )
            )
        }


    @Test
    fun `test that multiple pending messages start uploading`() =
        runTest {
            val pendingMessages = (0L..3L).map { stubPendingMessage(it) }

            whenever(monitorPendingMessagesByStateUseCase(PendingMessageState.READY_TO_UPLOAD)) doReturn
                    flowOf(pendingMessages)

            underTest().test { cancelAndConsumeRemainingEvents() }

            pendingMessages.forEachIndexed { i, it ->
                verify(startChatUploadAndWaitScanningFinishedUseCase)(
                    it.uriPath,
                    it.name,
                    listOf(it.id)
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