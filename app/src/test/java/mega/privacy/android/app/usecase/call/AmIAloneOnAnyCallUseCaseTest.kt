package mega.privacy.android.app.usecase.call

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.components.ChatManagement
import mega.privacy.android.domain.entity.call.ChatCall
import mega.privacy.android.domain.entity.call.ChatCallStatus
import mega.privacy.android.domain.entity.call.ParticipantsCountChange
import mega.privacy.android.domain.entity.chat.ChatRoom
import mega.privacy.android.domain.repository.ChatRepository
import mega.privacy.android.domain.usecase.meeting.MonitorChatCallUpdatesUseCase
import nz.mega.sdk.MegaChatApiAndroid
import nz.mega.sdk.MegaChatCall
import nz.mega.sdk.MegaChatRoom
import nz.mega.sdk.MegaHandleList
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.whenever
import test.mega.privacy.android.app.extensions.asHotFlow

@OptIn(ExperimentalCoroutinesApi::class)
class AmIAloneOnAnyCallUseCaseTest {
    private lateinit var underTest: AmIAloneOnAnyCallUseCase

    private val getCallUseCase = mock<GetCallUseCase>()
    private val monitorChatCallUpdatesUseCase = mock<MonitorChatCallUpdatesUseCase>()
    private val chatManagement = mock<ChatManagement>()
    private val chatRepository = mock<ChatRepository>()

    @BeforeEach
    fun setUp() {
        underTest = AmIAloneOnAnyCallUseCase(
            getCallUseCase = getCallUseCase,
            monitorChatCallUpdatesUseCase = monitorChatCallUpdatesUseCase,
            chatManagement = chatManagement,
            chatRepository = chatRepository,
        )
    }

    @AfterEach
    fun tearDown() {
        reset(
            getCallUseCase,
            chatRepository,
            monitorChatCallUpdatesUseCase,
            chatManagement,
        )
    }

    @Test
    fun `test that onlyMeInTheCall is true if alone on a ongoing meeting call`() = runTest {
        val isMeetingCall = true
        val isAlone = true
        val requestSent = false

        stubOngoingCall(
            isAlone = isAlone,
            isMeetingCall = isMeetingCall,
            requestSent = requestSent
        )

        underTest().test {
            assertThat(awaitItem().onlyMeInTheCall).isTrue()
        }
    }

    @Test
    fun `test that onlyMeInTheCall is false if not alone on a ongoing meeting call`() = runTest {
        val isMeetingCall = true
        val isAlone = false
        val requestSent = false

        stubOngoingCall(
            isAlone = isAlone,
            isMeetingCall = isMeetingCall,
            requestSent = requestSent
        )

        underTest().test {
            assertThat(awaitItem().onlyMeInTheCall).isFalse()
        }
    }

    @Test
    fun `test that onlyMeInTheCall is false if in a one on one ongoing  call`() = runTest {
        val isMeetingCall = false
        val isAlone = true
        val requestSent = false

        stubOngoingCall(
            isAlone = isAlone,
            isMeetingCall = isMeetingCall,
            requestSent = requestSent
        )

        underTest().test {
            assertThat(awaitItem().onlyMeInTheCall).isFalse()
        }
    }

    @Test
    fun `test that waiting for others is true if request has been sent in ongoing call`() =
        runTest {
            val isMeetingCall = true
            val isAlone = true
            val requestSent = true

            stubOngoingCall(
                isAlone = isAlone,
                isMeetingCall = isMeetingCall,
                requestSent = requestSent
            )

            underTest().test {
                assertThat(awaitItem().waitingForOthers).isTrue()
            }
        }

    @Test
    fun `test that waitingForOthers is false if no request has been sent in ongoing call`() =
        runTest {
            val isMeetingCall = true
            val isAlone = true
            val requestSent = false

            stubOngoingCall(
                isAlone = isAlone,
                isMeetingCall = isMeetingCall,
                requestSent = requestSent
            )

            underTest().test {
                assertThat(awaitItem().waitingForOthers).isFalse()
            }
        }

    @Test
    fun `test that waitingForOthers is false in a one on one ongoing call`() = runTest {
        val isMeetingCall = false
        val isAlone = true
        val requestSent = true

        stubOngoingCall(
            isAlone = isAlone,
            isMeetingCall = isMeetingCall,
            requestSent = requestSent
        )

        underTest().test {
            assertThat(awaitItem().waitingForOthers).isFalse()
        }
    }

    @ParameterizedTest
    @EnumSource(
        value = ChatCallStatus::class,
        names = ["Destroyed", "UserNoPresent", "TerminatingUserParticipation"],
        mode = EnumSource.Mode.INCLUDE
    )
    fun `test statuses with no result`(currentStatus: ChatCallStatus) = runTest {
        val call = mock<ChatCall>{
            on {changes} doReturn listOf()
            on { status } doReturn currentStatus
        }

        monitorChatCallUpdatesUseCase.stub {
            on{ invoke() } doReturn call.asHotFlow()
        }

        underTest().test {
            assertThat(cancelAndConsumeRemainingEvents()).isEmpty()
        }
    }

    @ParameterizedTest
    @EnumSource(
        value = ChatCallStatus::class,
        names = ["Destroyed", "UserNoPresent", "TerminatingUserParticipation", "InProgress", "Joining"],
        mode = EnumSource.Mode.EXCLUDE
    )
    fun `test statuses with default result`(currentStatus: ChatCallStatus) = runTest {
        val thisChatId = 12L
        getCallUseCase.stub {
            on { getCallsInProgressAndOnHold() } doReturn arrayListOf()
        }

        val call = mock<ChatCall>{
            on { chatId } doReturn thisChatId
            on {changes} doReturn listOf()
            on { status } doReturn currentStatus
        }

        monitorChatCallUpdatesUseCase.stub {
            on{ invoke() } doReturn call.asHotFlow()
        }

        underTest().test {
            assertThat(awaitItem()).isEqualTo(
                ParticipantsCountChange(
                    chatId = thisChatId,
                    onlyMeInTheCall = false,
                    waitingForOthers = false,
                    isReceivedChange = true,
                )
            )
        }
    }

    @Test
    fun `test that onlyMeInTheCall is true if alone on a updated meeting call`() = runTest {
        val isMeetingCall = true
        val isAlone = true
        val requestSent = false

        stubUpdatedCall(
            isAlone = isAlone,
            isMeetingCall = isMeetingCall,
            requestSent = requestSent
        )

        underTest().test {
            assertThat(awaitItem().onlyMeInTheCall).isTrue()
        }
    }

    @Test
    fun `test that onlyMeInTheCall is false if not alone on a updated meeting call`() = runTest {
        val isMeetingCall = true
        val isAlone = false
        val requestSent = false

        stubUpdatedCall(
            isAlone = isAlone,
            isMeetingCall = isMeetingCall,
            requestSent = requestSent
        )

        underTest().test {
            assertThat(awaitItem().onlyMeInTheCall).isFalse()
        }
    }

    @Test
    fun `test that onlyMeInTheCall is false if in a one on one updated  call`() = runTest {
        val isMeetingCall = false
        val isAlone = true
        val requestSent = false

        stubUpdatedCall(
            isAlone = isAlone,
            isMeetingCall = isMeetingCall,
            requestSent = requestSent
        )

        underTest().test {
            assertThat(awaitItem().onlyMeInTheCall).isFalse()
        }
    }

    @Test
    fun `test that waiting for others is true if request has been sent in updated call`() =
        runTest {
            val isMeetingCall = true
            val isAlone = true
            val requestSent = true

            stubUpdatedCall(
                isAlone = isAlone,
                isMeetingCall = isMeetingCall,
                requestSent = requestSent
            )

            underTest().test {
                assertThat(awaitItem().waitingForOthers).isTrue()
            }
        }

    @Test
    fun `test that waitingForOthers is false if no request has been sent in updated call`() =
        runTest {
            val isMeetingCall = true
            val isAlone = true
            val requestSent = false

            stubUpdatedCall(
                isAlone = isAlone,
                isMeetingCall = isMeetingCall,
                requestSent = requestSent
            )

            underTest().test {
                assertThat(awaitItem().waitingForOthers).isFalse()
            }
        }

    @Test
    fun `test that waitingForOthers is false in a one on one updated call`() = runTest {
        val isMeetingCall = false
        val isAlone = true
        val requestSent = true

        stubUpdatedCall(
            isAlone = isAlone,
            isMeetingCall = isMeetingCall,
            requestSent = requestSent
        )

        underTest().test {
            assertThat(awaitItem().waitingForOthers).isFalse()
        }
    }

    private fun stubOngoingCall(
        isAlone: Boolean,
        isMeetingCall: Boolean,
        requestSent: Boolean,
    ) {
        val thisChatId = 321L
        val myHandle = 42L
        val thisCallId = 123L
        val userCount = if (isAlone) 1L else 2L
        val peerList = mock<MegaHandleList> {
            on { size() } doReturn userCount
            on { get(any()) } doReturn myHandle
        }
        val call = mock<MegaChatCall> {
            on { chatid } doReturn thisChatId
            on { peeridParticipants } doReturn peerList
            on { callId } doReturn thisCallId
        }
        whenever(getCallUseCase.getCallsInProgressAndOnHold()).thenReturn(arrayListOf(call))
        val chatRoom = mock<ChatRoom> {
            on { isGroup } doReturn false
            on { isMeeting } doReturn isMeetingCall
        }

        chatRepository.stub {
            onBlocking { getChatRoom(any()) } doReturn chatRoom
            onBlocking { getMyUserHandle() } doReturn myHandle
        }
        whenever(chatManagement.isRequestSent(any())).thenReturn(requestSent)

        monitorChatCallUpdatesUseCase.stub {
            on { invoke() } doReturn flow { awaitCancellation() }
        }
    }

    private fun stubUpdatedCall(
        isAlone: Boolean,
        isMeetingCall: Boolean,
        requestSent: Boolean,
        currentStatus: ChatCallStatus = ChatCallStatus.Joining,
    ) {
        val thisChatId = 321L
        val myHandle = 42L
        val thisCallId = 123L

        getCallUseCase.stub {
            on { getCallsInProgressAndOnHold() } doReturn arrayListOf()
        }

        val peerList =  if (isAlone) listOf(myHandle) else listOf(myHandle, 1L)

        val call = mock<ChatCall>{
            on { chatId } doReturn thisChatId
            on {changes} doReturn listOf()
            on { status } doReturn currentStatus
            on { peerIdParticipants } doReturn peerList
            on { callId } doReturn thisCallId
        }

        monitorChatCallUpdatesUseCase.stub {
            on{ invoke() } doReturn call.asHotFlow()
        }


        val chatRoom = mock<ChatRoom> {
            on { isGroup } doReturn false
            on { isMeeting } doReturn isMeetingCall
        }

        chatRepository.stub {
            onBlocking { getChatRoom(any()) } doReturn chatRoom
            onBlocking { getMyUserHandle() } doReturn myHandle
        }

        whenever(chatManagement.isRequestSent(any())).thenReturn(requestSent)
    }
}