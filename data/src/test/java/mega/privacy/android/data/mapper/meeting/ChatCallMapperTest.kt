package mega.privacy.android.data.mapper.meeting

import com.google.common.truth.Truth
import mega.privacy.android.data.mapper.handles.HandleListMapper
import mega.privacy.android.domain.entity.call.CallCompositionChanges
import mega.privacy.android.domain.entity.call.ChatCallChanges
import mega.privacy.android.domain.entity.call.ChatCallStatus
import mega.privacy.android.domain.entity.call.EndCallReason
import mega.privacy.android.domain.entity.meeting.NetworkQualityType
import mega.privacy.android.domain.entity.call.ChatCallTermCodeType
import nz.mega.sdk.MegaChatCall
import nz.mega.sdk.MegaChatWaitingRoom
import nz.mega.sdk.MegaHandleList
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class ChatRequestMapperTest {
    private lateinit var underTest: ChatCallMapper

    private val expectedCallId = 123L
    private val expectedChatId = 345L
    private val expectedPeerId1 = 456L
    private val expectedPeerId2 = 789L
    private val expectedMsg = "message to test"
    private val expectedCallLimit = 3
    private val expectedCallWillEndTs = 789L
    private val expectedFlag = true

    @Before
    fun setUp() {
        underTest =
            ChatCallMapper(
                handleListMapper = HandleListMapper(),
                chatCallChangesMapper = ChatCallChangesMapper(),
                chatCallStatusMapper = ChatCallStatusMapper(),
                endCallReasonMapper = EndCallReasonMapper(),
                chatCallTermCodeMapper = ChatCallTermCodeMapper(),
                callCompositionChangesMapper = CallCompositionChangesMapper(),
                networkQualityMapper = NetworkQualityMapper(),
                chatWaitingRoomMapper = ChatWaitingRoomMapper(
                    WaitingRoomStatusMapper(),
                    HandleListMapper()
                ),
                waitingRoomStatusMapper = WaitingRoomStatusMapper(),
                chatSessionMapper = ChatSessionMapper(
                    ChatSessionChangesMapper(),
                    ChatSessionStatusMapper(),
                    ChatSessionTermCodeMapper()
                ),
                callNotificationMapper = CallNotificationMapper(),
            )
    }

    @Test
    fun `test mapping chat call received contains a valid call Id, chat Id, caller Id, aux Handle, handle, peer Id composition change`() {
        val chatCall = getMockChatCall()
        val actual = underTest(chatCall)
        Truth.assertThat(actual.callId).isEqualTo(expectedCallId)
        Truth.assertThat(actual.chatId).isEqualTo(expectedChatId)
        Truth.assertThat(actual.caller).isEqualTo(expectedPeerId1)
        Truth.assertThat(actual.auxHandle).isEqualTo(expectedPeerId1)
        Truth.assertThat(actual.handle).isEqualTo(expectedPeerId1)
        Truth.assertThat(actual.peerIdCallCompositionChange)
            .isEqualTo(expectedPeerId1)
    }

    @Test
    fun `test mapping chat call generic Message`() {
        val chatCall = getMockChatCall(genericMessage = expectedMsg)
        val actual = underTest(chatCall)
        Truth.assertThat(actual.genericMessage).isEqualTo(expectedMsg)
    }

    @Test
    fun `test mapping chat call will end Ts`() {
        val chatCall = getMockChatCall(callWillEndTs = expectedCallWillEndTs)
        val actual = underTest(chatCall)
        Truth.assertThat(actual.callWillEndTs).isEqualTo(expectedCallWillEndTs)
    }

    @Test
    fun `test mapping chat call flag`() {
        val chatCall = getMockChatCall(flag = expectedFlag)
        val actual = underTest(chatCall)
        Truth.assertThat(actual.flag).isEqualTo(expectedFlag)
    }

    @Test
    fun `test mapping call duration limit`() {
        val chatCall = getMockChatCall()
        val actual = underTest(chatCall)
        Truth.assertThat(actual.callDurationLimit).isEqualTo(expectedCallLimit)
    }

    @Test
    fun `test mapping call clients limit`() {
        val chatCall = getMockChatCall()
        val actual = underTest(chatCall)
        Truth.assertThat(actual.callClientsLimit).isEqualTo(expectedCallLimit)
    }

    @Test
    fun `test mapping call users limit`() {
        val chatCall = getMockChatCall()
        val actual = underTest(chatCall)
        Truth.assertThat(actual.callUsersLimit).isEqualTo(expectedCallLimit)
    }

    @Test
    fun `test mapping call clients per user limit`() {
        val chatCall = getMockChatCall()
        val actual = underTest(chatCall)
        Truth.assertThat(actual.callClientsPerUserLimit).isEqualTo(expectedCallLimit)
    }

    @Test
    fun `test mapping chat call status in progress`() {
        val chatCall = getMockChatCall(status = MegaChatCall.CALL_STATUS_IN_PROGRESS)
        val actual = underTest(chatCall)
        Truth.assertThat(actual.status).isEqualTo(ChatCallStatus.InProgress)
    }

    @Test
    fun `test mapping chat call changes type status`() {
        val chatCall = getMockChatCall(changes = MegaChatCall.CHANGE_TYPE_STATUS)
        val actual = underTest(chatCall)
        Truth.assertThat(actual.changes).contains(ChatCallChanges.Status)
    }

    @Test
    fun `test mapping chat call end call reason end by moderator`() {
        val chatCall = getMockChatCall(endCallReason = MegaChatCall.END_CALL_REASON_BY_MODERATOR)
        val actual = underTest(chatCall)
        Truth.assertThat(actual.endCallReason).isEqualTo(EndCallReason.ByModerator)
    }

    @Test
    fun `test mapping chat call when call composition change is peer added`() {
        val chatCall = getMockChatCall(callCompositionChange = MegaChatCall.PEER_ADDED)
        val actual = underTest(chatCall)
        Truth.assertThat(actual.callCompositionChange).isEqualTo(CallCompositionChanges.Added)
    }

    @Test
    fun `test mapping chat call received contains a valid peer id participants list`() {
        val peerIdParticipantsListTest = mock<MegaHandleList>()
        whenever(peerIdParticipantsListTest.size()).thenReturn(3)
        whenever(peerIdParticipantsListTest[0]).thenReturn(expectedPeerId1)
        whenever(peerIdParticipantsListTest[1]).thenReturn(expectedPeerId2)

        val chatCall = getMockChatCall(peerIdParticipants = peerIdParticipantsListTest)
        val actual = underTest(chatCall)
        Truth.assertThat(actual.peerIdParticipants).isNotNull()
        Truth.assertThat(actual.peerIdParticipants?.get(0)).isEqualTo(expectedPeerId1)
        Truth.assertThat(actual.peerIdParticipants?.get(1)).isEqualTo(expectedPeerId2)
    }

    @Test
    fun `test mapping chat call received contains a valid moderators list`() {
        val moderatorsListTest = mock<MegaHandleList>()
        whenever(moderatorsListTest.size()).thenReturn(3)
        whenever(moderatorsListTest[0]).thenReturn(expectedPeerId1)
        whenever(moderatorsListTest[1]).thenReturn(expectedPeerId2)

        val chatCall = getMockChatCall(moderators = moderatorsListTest)
        val actual = underTest(chatCall)
        Truth.assertThat(actual.moderators).isNotNull()
        Truth.assertThat(actual.moderators?.get(0)).isEqualTo(expectedPeerId1)
        Truth.assertThat(actual.moderators?.get(1)).isEqualTo(expectedPeerId2)
    }

    @Test
    fun `test mapping chat call received contains a valid raise hands list`() {
        val raisedHandsListTest = mock<MegaHandleList>()
        whenever(raisedHandsListTest.size()).thenReturn(3)
        whenever(raisedHandsListTest[0]).thenReturn(expectedPeerId1)
        whenever(raisedHandsListTest[1]).thenReturn(expectedPeerId2)

        val chatCall = getMockChatCall(raisedHandsList = raisedHandsListTest)
        val actual = underTest(chatCall)
        Truth.assertThat(actual.raisedHandsList).isNotNull()
        Truth.assertThat(actual.raisedHandsList?.get(0)).isEqualTo(expectedPeerId1)
        Truth.assertThat(actual.raisedHandsList?.get(1)).isEqualTo(expectedPeerId2)
    }

    @Test
    fun `test mapping chat call received contains a valid sessions client Id list`() {
        val sessionsClientIdListTest = mock<MegaHandleList>()
        whenever(sessionsClientIdListTest.size()).thenReturn(3)
        whenever(sessionsClientIdListTest[0]).thenReturn(expectedPeerId1)
        whenever(sessionsClientIdListTest[1]).thenReturn(expectedPeerId2)

        val chatCall = getMockChatCall(sessionsClientId = sessionsClientIdListTest)
        val actual = underTest(chatCall)
        Truth.assertThat(actual.sessionsClientId).isNotNull()
        Truth.assertThat(actual.sessionsClientId?.get(0)).isEqualTo(expectedPeerId1)
        Truth.assertThat(actual.sessionsClientId?.get(1)).isEqualTo(expectedPeerId2)
    }

    @Test
    fun `test mapping chat call when network quality is bad`() {
        val chatCall = getMockChatCall(networkQuality = MegaChatCall.NETWORK_QUALITY_BAD)
        val actual = underTest(chatCall)
        Truth.assertThat(actual.networkQuality).isEqualTo(NetworkQualityType.Bad)
    }

    @Test
    fun `test mapping chat call when term code is hang up`() {
        val chatCall = getMockChatCall(termCode = MegaChatCall.TERM_CODE_HANGUP)
        val actual = underTest(chatCall)
        Truth.assertThat(actual.termCode).isEqualTo(ChatCallTermCodeType.Hangup)
    }

    @Test
    fun `test mapping chat call when isAudioDetected is false `() {
        val chatCall = getMockChatCall(isAudioDetected = false)
        val actual = underTest(chatCall)
        Truth.assertThat(actual.isAudioDetected).isEqualTo(false)
    }

    @Test
    fun `test mapping chat call when isIgnored is true `() {
        val chatCall = getMockChatCall(isIgnored = true)
        val actual = underTest(chatCall)
        Truth.assertThat(actual.isIgnored).isEqualTo(true)
    }

    @Test
    fun `test mapping chat call when isIncoming is true `() {
        val chatCall = getMockChatCall(isIncoming = true)
        val actual = underTest(chatCall)
        Truth.assertThat(actual.isIncoming).isEqualTo(true)
    }

    @Test
    fun `test mapping chat call when isOnHold is true `() {
        val chatCall = getMockChatCall(isOnHold = true)
        val actual = underTest(chatCall)
        Truth.assertThat(actual.isOnHold).isEqualTo(true)
    }

    @Test
    fun `test mapping chat call when isOutgoing is true `() {
        val chatCall = getMockChatCall(isOutgoing = true)
        val actual = underTest(chatCall)
        Truth.assertThat(actual.isOutgoing).isEqualTo(true)
    }

    @Test
    fun `test mapping chat call when isOwnClientCaller is true `() {
        val chatCall = getMockChatCall(isOwnClientCaller = true)
        val actual = underTest(chatCall)
        Truth.assertThat(actual.isOwnClientCaller).isEqualTo(true)
    }

    @Test
    fun `test mapping chat call when isOwnModerator is true `() {
        val chatCall = getMockChatCall(isOwnModerator = true)
        val actual = underTest(chatCall)
        Truth.assertThat(actual.isOwnModerator).isEqualTo(true)
    }

    @Test
    fun `test mapping chat call when isRinging is true `() {
        val chatCall = getMockChatCall(isRinging = true)
        val actual = underTest(chatCall)
        Truth.assertThat(actual.isRinging).isEqualTo(true)
    }

    @Test
    fun `test mapping chat call when hasLocalAudio is true `() {
        val chatCall = getMockChatCall(hasLocalAudio = true)
        val actual = underTest(chatCall)
        Truth.assertThat(actual.hasLocalAudio).isEqualTo(true)
    }

    @Test
    fun `test mapping chat call when hasLocalVideo is false `() {
        val chatCall = getMockChatCall(hasLocalVideo = false)
        val actual = underTest(chatCall)
        Truth.assertThat(actual.hasLocalVideo).isEqualTo(false)
    }

    @Test
    fun `test mapping chat call when isSpeakRequestEnabled is false `() {
        val chatCall = getMockChatCall(isSpeakRequestEnabled = false)
        val actual = underTest(chatCall)
        Truth.assertThat(actual.isSpeakRequestEnabled).isEqualTo(false)
    }

    private fun getMockChatCall(
        chatId: Long = expectedChatId,
        callId: Long = expectedCallId,
        status: Int = -1,
        hasLocalAudio: Boolean = false,
        hasLocalVideo: Boolean = false,
        changes: Int = -1,
        isAudioDetected: Boolean = false,
        duration: Duration = 100.seconds,
        initialTimeStamp: Long = 1L,
        finalTimeStamp: Long = 1L,
        termCode: Int = -1,
        endCallReason: Int = -1,
        isSpeakRequestEnabled: Boolean = false,
        notificationType: Int = -1,
        auxHandle: Long = expectedPeerId1,
        isRinging: Boolean = false,
        isOwnModerator: Boolean = false,
        sessionsClientId: MegaHandleList = mock(),
        peerIdCallCompositionChange: Long = expectedPeerId1,
        callCompositionChange: Int = -1,
        peerIdParticipants: MegaHandleList = mock(),
        handle: Long = expectedPeerId1,
        flag: Boolean = false,
        moderators: MegaHandleList = mock(),
        raisedHandsList: MegaHandleList = mock(),
        numParticipants: Int = 2,
        isIgnored: Boolean = false,
        isIncoming: Boolean = false,
        isOutgoing: Boolean = false,
        isOwnClientCaller: Boolean = false,
        caller: Long = expectedPeerId1,
        isOnHold: Boolean = false,
        genericMessage: String = "",
        callDurationLimit: Int = expectedCallLimit,
        callUsersLimit: Int = expectedCallLimit,
        callClientsLimit: Int = expectedCallLimit,
        callClientsPerUserLimit: Int = expectedCallLimit,
        callWillEndTs: Long = expectedCallWillEndTs,
        networkQuality: Int = -1,
        waitingRoomStatus: Int? = 0,
        waitingRoom: MegaChatWaitingRoom? = null,
        handleList: MegaHandleList = mock(),
        speakersList: MegaHandleList = mock(),
        speakRequestsList: MegaHandleList = mock(),
    ): MegaChatCall {
        val call = mock<MegaChatCall> {
            on { this.chatid }.thenReturn(chatId)
            on { this.callId }.thenReturn(callId)
            on { this.status }.thenReturn(status)
            on { this.hasLocalAudio() }.thenReturn(hasLocalAudio)
            on { this.hasLocalVideo() }.thenReturn(hasLocalVideo)
            on { this.changes }.thenReturn(changes)
            on { this.isAudioDetected }.thenReturn(isAudioDetected)
            on { this.duration }.thenReturn(duration.inWholeSeconds)
            on { this.initialTimeStamp }.thenReturn(initialTimeStamp)
            on { this.finalTimeStamp }.thenReturn(finalTimeStamp)
            on { this.termCode }.thenReturn(termCode)
            on { this.callWillEndTs }.thenReturn(callWillEndTs)
            on { this.callDurationLimit }.thenReturn(callDurationLimit)
            on { this.callUsersLimit }.thenReturn(callUsersLimit)
            on { this.callClientsLimit }.thenReturn(callClientsLimit)
            on { this.callClientsPerUserLimit }.thenReturn(callClientsPerUserLimit)
            on { this.endCallReason }.thenReturn(endCallReason)
            on { this.isSpeakRequestEnabled }.thenReturn(isSpeakRequestEnabled)
            on { this.notificationType }.thenReturn(notificationType)
            on { this.auxHandle }.thenReturn(auxHandle)
            on { this.isRinging }.thenReturn(isRinging)
            on { this.isOwnModerator }.thenReturn(isOwnModerator)
            on { this.sessionsClientid }.thenReturn(sessionsClientId)
            on { this.peeridCallCompositionChange }.thenReturn(peerIdCallCompositionChange)
            on { this.callCompositionChange }.thenReturn(callCompositionChange)
            on { this.peeridParticipants }.thenReturn(peerIdParticipants)
            on { this.handle }.thenReturn(handle)
            on { this.flag }.thenReturn(flag)
            on { this.moderators }.thenReturn(moderators)
            on { this.raiseHandsList }.thenReturn(raisedHandsList)
            on { this.numParticipants }.thenReturn(numParticipants)
            on { this.isIgnored }.thenReturn(isIgnored)
            on { this.isIncoming }.thenReturn(isIncoming)
            on { this.isOutgoing }.thenReturn(isOutgoing)
            on { this.isOwnClientCaller }.thenReturn(isOwnClientCaller)
            on { this.caller }.thenReturn(caller)
            on { this.isOnHold }.thenReturn(isOnHold)
            on { this.genericMessage }.thenReturn(genericMessage)
            on { this.networkQuality }.thenReturn(networkQuality)
            on { this.wrJoiningState }.thenReturn(waitingRoomStatus)
            on { this.waitingRoom }.thenReturn(waitingRoom)
            on { this.handleList }.thenReturn(handleList)
            on { this.speakersList }.thenReturn(speakersList)
            on { this.speakRequestsList }.thenReturn(speakRequestsList)
        }

        return call
    }
}