package mega.privacy.android.data.mapper.meeting

import com.google.common.truth.Truth
import mega.privacy.android.data.mapper.handles.HandleListMapper
import mega.privacy.android.domain.entity.meeting.CallCompositionChanges
import mega.privacy.android.domain.entity.meeting.ChatCallChanges
import mega.privacy.android.domain.entity.meeting.ChatCallStatus
import mega.privacy.android.domain.entity.meeting.ChatSession
import mega.privacy.android.domain.entity.meeting.EndCallReason
import mega.privacy.android.domain.entity.meeting.NetworkQualityType
import mega.privacy.android.domain.entity.meeting.TermCodeType
import nz.mega.sdk.MegaChatCall
import nz.mega.sdk.MegaChatSession
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

    @Before
    fun setUp() {
        underTest =
            ChatCallMapper(
                handleListMapper = HandleListMapper(),
                chatCallChangesMapper = ChatCallChangesMapper(),
                chatCallStatusMapper = ChatCallStatusMapper(),
                endCallReasonMapper = EndCallReasonMapper(),
                callTermCodeMapper = CallTermCodeMapper(),
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
            )
    }

    @Test
    fun `test mapping chat call received contains a valid call Id, chat Id, caller Id, peer Id composition change`() {
        val chatCall = getMockChatCall()
        val actual = underTest(chatCall)
        Truth.assertThat(actual.callId).isEqualTo(expectedCallId)
        Truth.assertThat(actual.chatId).isEqualTo(expectedChatId)
        Truth.assertThat(actual.caller).isEqualTo(expectedPeerId1)
        Truth.assertThat(actual.peerIdCallCompositionChange)
            .isEqualTo(expectedPeerId1)
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
        Truth.assertThat(actual.termCode).isEqualTo(TermCodeType.Hangup)
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
    fun `test mapping chat call when isSpeakAllowed is true `() {
        val chatCall = getMockChatCall(isSpeakAllowed = true)
        val actual = underTest(chatCall)
        Truth.assertThat(actual.isSpeakAllowed).isEqualTo(true)
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
    fun `test mapping chat call when hasPendingSpeakRequest is false `() {
        val chatCall = getMockChatCall(hasPendingSpeakRequest = false)
        val actual = underTest(chatCall)
        Truth.assertThat(actual.hasPendingSpeakRequest).isEqualTo(false)
    }

    private fun getMockChatSession(peerId:Long, clientId:Long): MegaChatSession{
        val session = mock<MegaChatSession> {
            on { peerid }.thenReturn(peerId)
            on { clientid }.thenReturn(clientId)
            on { status }.thenReturn(MegaChatSession.SESSION_STATUS_IN_PROGRESS)
            on { isSpeakAllowed }.thenReturn(true)
            on { hasAudio() }.thenReturn(true)
            on { hasVideo() }.thenReturn(true)
            on { isHiResVideo }.thenReturn(true)
            on { isLowResVideo }.thenReturn(false)
            on { hasCamera() }.thenReturn(true)
            on { isLowResCamera }.thenReturn(false)
            on { isHiResCamera }.thenReturn(true)
            on { hasScreenShare() }.thenReturn(false)
            on { isHiResScreenShare }.thenReturn(false)
            on { isLowResScreenShare }.thenReturn(false)
            on { isOnHold }.thenReturn(false)
            on { termCode }.thenReturn(MegaChatSession.SESS_TERM_CODE_RECOVERABLE)
            on { hasPendingSpeakRequest() }.thenReturn(false)
            on { isAudioDetected }.thenReturn(true)
            on { canRecvVideoHiRes() }.thenReturn(true)
            on { canRecvVideoLowRes() }.thenReturn(false)
            on { isModerator }.thenReturn(true)
            on { isRecording }.thenReturn(true)
            on { hasSpeakPermission() }.thenReturn(false)
        }

        return session
    }

    private fun getMockChatCall(
        status: Int = -1,
        chatId: Long = expectedChatId,
        callId: Long = expectedCallId,
        hasLocalAudio: Boolean = false,
        hasLocalVideo: Boolean = false,
        changes: Int = -1,
        hasSpeakerPermission: Boolean = false,
        isAudioDetected: Boolean = false,
        duration: Duration = 100.seconds,
        initialTimeStamp: Long = 1L,
        finalTimeStamp: Long = 1L,
        termCode: Int = -1,
        endCallReason: Int = -1,
        isSpeakRequestEnabled: Boolean = false,
        isRinging: Boolean = false,
        isOwnModerator: Boolean = false,
        sessionsClientId: MegaHandleList = mock(),
        peerIdCallCompositionChange: Long = expectedPeerId1,
        callCompositionChange: Int = -1,
        peerIdParticipants: MegaHandleList = mock(),
        moderators: MegaHandleList = mock(),
        numParticipants: Int = 2,
        isIgnored: Boolean = false,
        isIncoming: Boolean = false,
        isOutgoing: Boolean = false,
        isOwnClientCaller: Boolean = false,
        caller: Long = expectedPeerId1,
        isOnHold: Boolean = false,
        isSpeakAllowed: Boolean = false,
        networkQuality: Int = -1,
        hasPendingSpeakRequest: Boolean = false,
        waitingRoomStatus: Int? = 0,
        sessionByClientId: Map<Long, ChatSession> = emptyMap()
    ): MegaChatCall {
        val call = mock<MegaChatCall> {
            on { this.callId }.thenReturn(callId)
            on { this.chatid }.thenReturn(chatId)
            on { this.status }.thenReturn(status)
            on { this.caller }.thenReturn(caller)
            on { this.isSpeakRequestEnabled }.thenReturn(isSpeakRequestEnabled)
            on { this.hasSpeakPermission() }.thenReturn(hasSpeakerPermission)
            on { this.duration }.thenReturn(duration.inWholeSeconds)
            on { this.numParticipants }.thenReturn(numParticipants)
            on { this.changes }.thenReturn(changes)
            on { this.endCallReason }.thenReturn(endCallReason)
            on { this.callCompositionChange }.thenReturn(callCompositionChange)
            on { this.peeridCallCompositionChange }.thenReturn(peerIdCallCompositionChange)
            on { this.peeridParticipants }.thenReturn(peerIdParticipants)
            on { this.moderators }.thenReturn(moderators)
            on { this.sessionsClientid }.thenReturn(sessionsClientId)
            on { this.networkQuality }.thenReturn(networkQuality)
            on { this.termCode }.thenReturn(termCode)
            on { this.initialTimeStamp }.thenReturn(initialTimeStamp)
            on { this.finalTimeStamp }.thenReturn(finalTimeStamp)
            on { this.isAudioDetected }.thenReturn(isAudioDetected)
            on { this.isIgnored }.thenReturn(isIgnored)
            on { this.isIncoming }.thenReturn(isIncoming)
            on { this.isOnHold }.thenReturn(isOnHold)
            on { this.isOutgoing }.thenReturn(isOutgoing)
            on { this.isOwnClientCaller }.thenReturn(isOwnClientCaller)
            on { this.isOwnModerator }.thenReturn(isOwnModerator)
            on { this.isRinging }.thenReturn(isRinging)
            on { this.isSpeakAllowed }.thenReturn(isSpeakAllowed)
            on { this.hasLocalAudio() }.thenReturn(hasLocalAudio)
            on { this.hasLocalVideo() }.thenReturn(hasLocalVideo)
            on { this.hasPendingSpeakRequest() }.thenReturn(hasPendingSpeakRequest)
            on { this.wrJoiningState }.thenReturn(waitingRoomStatus)
        }

        return call
    }
}