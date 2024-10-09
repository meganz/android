package mega.privacy.android.domain.usecase.call

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.call.ChatCall
import mega.privacy.android.domain.entity.call.ChatCallStatus
import mega.privacy.android.domain.entity.call.ParticipantsCountChange
import mega.privacy.android.domain.entity.chat.ChatRoom
import mega.privacy.android.domain.repository.ChatRepository
import mega.privacy.android.domain.usecase.meeting.MonitorChatCallUpdatesUseCase
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.whenever
import mega.privacy.android.domain.repository.CallRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AmIAloneOnAnyCallUseCaseTest {
    private lateinit var underTest: AmIAloneOnAnyCallUseCase

    private val monitorChatCallUpdatesUseCase = mock<MonitorChatCallUpdatesUseCase>()
    private val chatRepository = mock<ChatRepository>()
    private val callRepository = mock<CallRepository>()

    @BeforeEach
    fun setUp() {
        underTest = AmIAloneOnAnyCallUseCase(
            callRepository = callRepository,
            monitorChatCallUpdatesUseCase = monitorChatCallUpdatesUseCase,
            chatRepository = chatRepository,
        )
    }

    @AfterEach
    fun tearDown() {
        reset(
            chatRepository,
            monitorChatCallUpdatesUseCase,
            callRepository
        )
    }

    @Test
    fun `test that onlyMeInTheCall is true if alone on a ongoing meeting call`() =
        runTest {
            val isMeetingCall = true
            val isAlone = true

            stubOngoingCall(
                isAlone = isAlone,
                isMeetingCall = isMeetingCall,
            )

            underTest().test {
                assertThat(awaitItem().onlyMeInTheCall).isTrue()
            }
        }

    @Test
    fun `test that onlyMeInTheCall is false if not alone on a ongoing meeting call`() =
        runTest {
            val isMeetingCall = true
            val isAlone = false

            stubOngoingCall(
                isAlone = isAlone,
                isMeetingCall = isMeetingCall,
            )

            underTest().test {
                assertThat(awaitItem().onlyMeInTheCall).isFalse()
            }
        }

    @Test
    fun `test that onlyMeInTheCall is false if in a one on one ongoing  call`() =
        runTest {
            val isMeetingCall = false
            val isAlone = true

            stubOngoingCall(
                isAlone = isAlone,
                isMeetingCall = isMeetingCall,
            )

            underTest().test {
                assertThat(awaitItem().onlyMeInTheCall).isFalse()
            }
        }

    @ParameterizedTest
    @EnumSource(
        value = ChatCallStatus::class,
        names = ["Destroyed", "UserNoPresent", "TerminatingUserParticipation"],
        mode = EnumSource.Mode.INCLUDE
    )
    fun `test statuses with no result`(currentStatus: ChatCallStatus) = runTest {
        whenever(callRepository.getCallHandleList(any())).thenReturn(emptyList())
        val call = mock<ChatCall> {
            on { changes } doReturn listOf()
            on { status } doReturn currentStatus
        }

        monitorChatCallUpdatesUseCase.stub {
            on { invoke() } doReturn flowOf(call)
        }

        underTest().test {
            awaitComplete()
        }
    }

    @ParameterizedTest
    @EnumSource(
        value = ChatCallStatus::class,
        names = ["Destroyed", "UserNoPresent", "TerminatingUserParticipation", "InProgress", "Joining"],
        mode = EnumSource.Mode.EXCLUDE
    )
    fun `test statuses with default result`(currentStatus: ChatCallStatus) =
        runTest {
            val thisChatId = 12L
            val callId = 123L
            whenever(callRepository.getCallHandleList(any())).thenReturn(emptyList())

            val call = mock<ChatCall> {
                on { chatId } doReturn thisChatId
                on { changes } doReturn listOf()
                on { status } doReturn currentStatus
                on { this.callId } doReturn callId
            }

            monitorChatCallUpdatesUseCase.stub {
                on { invoke() } doReturn flowOf(call)
            }

            underTest().test {
                assertThat(awaitItem()).isEqualTo(
                    ParticipantsCountChange(
                        chatId = thisChatId,
                        callId = callId,
                        onlyMeInTheCall = false,
                        isReceivedChange = true,
                    )
                )
                awaitComplete()
            }
        }

    @Test
    fun `test that onlyMeInTheCall is true if alone on a updated meeting call`() =
        runTest {
            val isMeetingCall = true
            val isAlone = true

            stubUpdatedCall(
                isAlone = isAlone,
                isMeetingCall = isMeetingCall,
            )

            underTest().test {
                assertThat(awaitItem().onlyMeInTheCall).isTrue()
                awaitComplete()
            }
        }

    @Test
    fun `test that onlyMeInTheCall is false if not alone on a updated meeting call`() =
        runTest {
            val isMeetingCall = true
            val isAlone = false

            stubUpdatedCall(
                isAlone = isAlone,
                isMeetingCall = isMeetingCall,
            )

            underTest().test {
                assertThat(awaitItem().onlyMeInTheCall).isFalse()
                awaitComplete()
            }
        }

    @Test
    fun `test that onlyMeInTheCall is false if in a one on one updated  call`() =
        runTest {
            val isMeetingCall = false
            val isAlone = true

            stubUpdatedCall(
                isAlone = isAlone,
                isMeetingCall = isMeetingCall,
            )

            underTest().test {
                assertThat(awaitItem().onlyMeInTheCall).isFalse()
                awaitComplete()
            }
        }

    private suspend fun stubOngoingCall(
        isAlone: Boolean,
        isMeetingCall: Boolean,
    ) {
        val thisChatId = 321L
        val myHandle = 42L
        val thisCallId = 123L
        val call = mock<ChatCall> {
            on { chatId } doReturn thisChatId
            on { peerIdParticipants } doReturn if (isAlone) listOf(myHandle) else listOf(
                myHandle,
                43L
            )
            on { callId } doReturn thisCallId
        }
        whenever(callRepository.getCallHandleList(ChatCallStatus.Connecting)).thenReturn(emptyList())
        whenever(callRepository.getCallHandleList(ChatCallStatus.Joining)).thenReturn(emptyList())
        whenever(callRepository.getCallHandleList(ChatCallStatus.InProgress)).thenReturn(
            listOf(
                thisCallId
            )
        )
        whenever(callRepository.getChatCall(thisCallId)).thenReturn(call)
        val chatRoom = mock<ChatRoom> {
            on { isGroup } doReturn false
            on { isMeeting } doReturn isMeetingCall
        }

        chatRepository.stub {
            onBlocking { getChatRoom(any()) } doReturn chatRoom
            onBlocking { getMyUserHandle() } doReturn myHandle
        }

        monitorChatCallUpdatesUseCase.stub {
            on { invoke() } doReturn flow { awaitCancellation() }
        }
    }

    private suspend fun stubUpdatedCall(
        isAlone: Boolean,
        isMeetingCall: Boolean,
        currentStatus: ChatCallStatus = ChatCallStatus.Joining,
    ) {
        val thisChatId = 321L
        val myHandle = 42L
        val thisCallId = 123L

        whenever(callRepository.getCallHandleList(any())).thenReturn(emptyList())

        val peerList = if (isAlone) listOf(myHandle) else listOf(myHandle, 1L)

        val call = mock<ChatCall> {
            on { chatId } doReturn thisChatId
            on { changes } doReturn listOf()
            on { status } doReturn currentStatus
            on { peerIdParticipants } doReturn peerList
            on { callId } doReturn thisCallId
        }

        monitorChatCallUpdatesUseCase.stub {
            on { invoke() } doReturn flowOf(call)
        }


        val chatRoom = mock<ChatRoom> {
            on { isGroup } doReturn false
            on { isMeeting } doReturn isMeetingCall
        }

        chatRepository.stub {
            onBlocking { getChatRoom(any()) } doReturn chatRoom
            onBlocking { getMyUserHandle() } doReturn myHandle
        }
    }
}