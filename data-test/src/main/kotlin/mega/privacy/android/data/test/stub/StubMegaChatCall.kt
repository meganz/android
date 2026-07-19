package mega.privacy.android.data.test.stub

import nz.mega.sdk.MegaChatCall
import nz.mega.sdk.MegaChatSession
import nz.mega.sdk.MegaChatWaitingRoom
import nz.mega.sdk.MegaHandleList

/**
 * In-memory stub of [MegaChatCall] that is safe to use in unit tests.
 *
 * Every public instance method is overridden, so no call ever reaches the native SDK
 * (which would crash the JVM given the null native pointer). Data getters are backed by
 * constructor parameters with sensible defaults; all other methods return benign constants.
 */
class StubMegaChatCall(
    private val callId: Long = -1L,
    private val chatId: Long = -1L,
    private val status: Int = MegaChatCall.CALL_STATUS_INITIAL,
    private val duration: Long = 0L,
    private val initialTimeStamp: Long = 0L,
    private val finalTimeStamp: Long = 0L,
    private val termCode: Int = MegaChatCall.TERM_CODE_INVALID,
    private val endCallReason: Int = MegaChatCall.END_CALL_REASON_INVALID,
    private val hasLocalAudio: Boolean = false,
    private val hasLocalVideo: Boolean = false,
    private val hasLocalScreenShare: Boolean = false,
    private val isRinging: Boolean = false,
    private val isIgnored: Boolean = false,
    private val isIncoming: Boolean = false,
    private val isOutgoing: Boolean = false,
    private val isOwnModerator: Boolean = false,
    private val isOnHold: Boolean = false,
    private val isOwnClientCaller: Boolean = false,
    private val isAudioDetected: Boolean = false,
    private val isSpeakRequestEnabled: Boolean = false,
    private val caller: Long = -1L,
    private val changes: Int = 0,
    private val numParticipants: Int = 0,
    private val networkQuality: Int = MegaChatCall.NETWORK_QUALITY_GOOD,
    private val notificationType: Int = MegaChatCall.NOTIFICATION_TYPE_INVALID,
    private val genericMessage: String? = null,
    private val auxHandle: Long = -1L,
    private val handle: Long = -1L,
    private val flag: Boolean = false,
    private val callWillEndTs: Long = 0L,
    private val peeridParticipants: MegaHandleList = StubMegaHandleList(),
    private val moderators: MegaHandleList = StubMegaHandleList(),
    private val sessionsClientid: MegaHandleList = StubMegaHandleList(),
) : MegaChatCall(0, false) {

    override fun delete() = Unit

    override fun getStatus(): Int = status
    override fun getChatid(): Long = chatId
    override fun getCallId(): Long = callId
    override fun hasLocalAudio(): Boolean = hasLocalAudio
    override fun hasLocalVideo(): Boolean = hasLocalVideo
    override fun hasLocalScreenShare(): Boolean = hasLocalScreenShare
    override fun getChanges(): Int = changes
    override fun hasChanged(p0: Int): Boolean = (changes and p0) != 0
    override fun isAudioDetected(): Boolean = isAudioDetected
    override fun hasUserSpeakPermission(p0: Long): Boolean = false
    override fun hasUserHandRaised(p0: Long): Boolean = false
    override fun getDuration(): Long = duration
    override fun getInitialTimeStamp(): Long = initialTimeStamp
    override fun getFinalTimeStamp(): Long = finalTimeStamp
    override fun getTermCode(): Int = termCode
    override fun getCallWillEndTs(): Long = callWillEndTs
    override fun getCallDurationLimit(): Int = 0
    override fun getCallUsersLimit(): Int = 0
    override fun getCallClientsLimit(): Int = 0
    override fun getCallClientsPerUserLimit(): Int = 0
    override fun getEndCallReason(): Int = endCallReason
    override fun isSpeakRequestEnabled(): Boolean = isSpeakRequestEnabled
    override fun getNotificationType(): Int = notificationType
    override fun getAuxHandle(): Long = auxHandle
    override fun isRinging(): Boolean = isRinging
    override fun isOwnModerator(): Boolean = isOwnModerator
    override fun getSessionsClientidByUserHandle(p0: Long): MegaHandleList = StubMegaHandleList()
    override fun getSessionsClientid(): MegaHandleList = sessionsClientid
    override fun getMegaChatSession(p0: Long): MegaChatSession? = null
    override fun getPeeridCallCompositionChange(): Long = -1L
    override fun getCallCompositionChange(): Int = 0
    override fun getPeeridParticipants(): MegaHandleList = peeridParticipants
    override fun getHandle(): Long = handle
    override fun getFlag(): Boolean = flag
    override fun getModerators(): MegaHandleList = moderators
    override fun getRaiseHandsList(): MegaHandleList = StubMegaHandleList()
    override fun getNumParticipants(): Int = numParticipants
    override fun isIgnored(): Boolean = isIgnored
    override fun isIncoming(): Boolean = isIncoming
    override fun isOutgoing(): Boolean = isOutgoing
    override fun isOwnClientCaller(): Boolean = isOwnClientCaller
    override fun getCaller(): Long = caller
    override fun isOnHold(): Boolean = isOnHold
    override fun getGenericMessage(): String? = genericMessage
    override fun getNetworkQuality(): Int = networkQuality
    override fun hasUserPendingSpeakRequest(p0: Long): Boolean = false
    override fun getWrJoiningState(): Int = 0
    override fun getWaitingRoom(): MegaChatWaitingRoom? = null
    override fun getHandleList(): MegaHandleList = StubMegaHandleList()
    override fun getSpeakersList(): MegaHandleList = StubMegaHandleList()
    override fun getSpeakRequestsList(): MegaHandleList = StubMegaHandleList()
}
