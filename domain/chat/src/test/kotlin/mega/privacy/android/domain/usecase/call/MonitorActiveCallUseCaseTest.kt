package mega.privacy.android.domain.usecase.call

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.call.ChatCall
import mega.privacy.android.domain.entity.call.ChatCallChanges
import mega.privacy.android.domain.entity.call.ChatCallStatus
import mega.privacy.android.domain.repository.CallRepository
import mega.privacy.android.domain.usecase.meeting.MonitorChatCallUpdatesUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.kotlin.any
import org.mockito.kotlin.clearInvocations
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MonitorActiveCallUseCaseTest {

    private lateinit var underTest: MonitorActiveCallUseCase

    private val sharedMonitorCallUpdatesFlow = MutableSharedFlow<ChatCall>()

    private val monitorChatCallUpdatesUseCase: MonitorChatCallUpdatesUseCase = mock {
        on { invoke() } doReturn sharedMonitorCallUpdatesFlow
    }
    private val callRepository = mock<CallRepository>()
    private val mockChatCall = mock<ChatCall>()

    @BeforeAll
    fun setup() {
        underTest = MonitorActiveCallUseCase(
            monitorChatCallUpdatesUseCase,
            callRepository
        )
    }

    @BeforeEach
    fun resetMocks() = runTest {
        reset(callRepository)
        whenever(callRepository.getCallChatIdList(any())).thenReturn(emptyList())
    }

    @ParameterizedTest
    @EnumSource(
        value = ChatCallStatus::class,
        names = ["Initial", "UserNoPresent", "Connecting", "Joining", "InProgress"]
    )
    fun `test that initial state is true when there is an active call with valid status`(status: ChatCallStatus) =
        runTest {
            whenever(callRepository.getCallChatIdList(any()))
                .thenReturn(emptyList())
            val chatId = 123L
            whenever(callRepository.getCallChatIdList(status))
                .thenReturn(listOf(chatId))
            whenever(callRepository.getChatCall(chatId)).thenReturn(mockChatCall)
            underTest().test {
                val result = awaitItem()
                assertThat(result).isNotNull()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @ParameterizedTest
    @EnumSource(
        value = ChatCallStatus::class,
        names = ["Initial", "UserNoPresent", "Connecting", "Joining", "InProgress", "WaitingRoom"]
    )
    fun `test that returns true when there is an update with active call with valid status`(status: ChatCallStatus) {
        runTest {
            whenever(callRepository.getCallChatIdList(any()))
                .thenReturn(emptyList())
            whenever(callRepository.getChatCall(any())).thenReturn(mockChatCall)
            underTest().test {
                assertThat(awaitItem()).isNull() // check initial to be sure test is working
                whenever(callRepository.getCallChatIdList(status))
                    .thenReturn(listOf(123L))
                sharedMonitorCallUpdatesFlow.emit(statusUpdate)

                val result = awaitItem()
                assertThat(result).isNotNull()
                cancelAndIgnoreRemainingEvents()
            }
        }
    }


    @Test
    fun `test that initial state is false when there are no active calls`() = runTest {
        whenever(callRepository.getCallChatIdList(any())).thenReturn(emptyList())

        underTest().test {
            val result = awaitItem()
            assertThat(result).isNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that returns false when there is an update with no active calls`() = runTest {
        whenever(callRepository.getCallChatIdList(any()))
            .thenReturn(listOf(123L))
        whenever(callRepository.getChatCall(any())).thenReturn(mockChatCall)
        underTest().test {
            assertThat(awaitItem()).isNotNull() // check initial to be sure test is working
            whenever(callRepository.getCallChatIdList(any()))
                .thenReturn(emptyList())
            sharedMonitorCallUpdatesFlow.emit(statusUpdate)

            val result = awaitItem()
            assertThat(result).isNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @ParameterizedTest
    @EnumSource(
        value = ChatCallChanges::class,
        names = ["LocalAVFlags", "RingingStatus", "CallComposition", "OnHold", "Speaker", "AudioLevel", "NetworkQuality"]
    )
    fun `test that does not emit new value when update is not a Status change`(change: ChatCallChanges) =
        runTest {
            whenever(callRepository.getCallChatIdList(any())).thenReturn(emptyList())

            val nonStatusUpdate = mock<ChatCall> {
                on { this.changes } doReturn listOf(change)
            }

            underTest().test {
                val initialResult = awaitItem()
                assertThat(initialResult).isNull()

                // clear initial repository calls to later check that there are no more interactions
                clearInvocations(callRepository)

                sharedMonitorCallUpdatesFlow.emit(nonStatusUpdate)

                expectNoEvents()
                cancelAndIgnoreRemainingEvents()
            }
            verifyNoMoreInteractions(callRepository)
        }

    @ParameterizedTest
    @EnumSource(
        value = ChatCallStatus::class,
        names = ["Initial", "UserNoPresent", "Connecting", "Joining", "InProgress", "WaitingRoom"]
    )
    fun `test that returns true without calling repository when ChatCall has active status`(status: ChatCallStatus) =
        runTest {
            whenever(callRepository.getCallChatIdList(any())).thenReturn(emptyList())

            val activeStatusUpdate = mock<ChatCall> {
                on { this.status } doReturn status
                on { this.changes } doReturn listOf(ChatCallChanges.Status)
            }

            underTest().test {
                val initialResult = awaitItem()
                assertThat(initialResult).isNull()

                // clear initial repository calls to later check that there are no more interactions
                clearInvocations(callRepository)

                sharedMonitorCallUpdatesFlow.emit(activeStatusUpdate)

                val result = awaitItem()
                assertThat(result).isNotNull()
                cancelAndIgnoreRemainingEvents()
            }

            // Verify that repository was not called after emitting the update
            verifyNoMoreInteractions(callRepository)
        }

    val statusUpdate = mock<ChatCall> {
        on { this.changes } doReturn listOf(ChatCallChanges.Status)
    }
}
