package mega.privacy.android.domain.entity.chat

import mega.privacy.android.domain.entity.meeting.ScheduledMeetingStatus
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit

/**
 * Meeting room item
 *
 * @property chatId
 * @property title
 * @property lastMessage
 * @property isLastMessageVoiceClip
 * @property isLastMessageGeolocation
 * @property unreadCount
 * @property hasPermissions
 * @property isActive
 * @property isPublic
 * @property isMuted
 * @property lastTimestamp
 * @property lastTimestampFormatted
 * @property highlight
 * @property firstUserChar
 * @property firstUserAvatar
 * @property firstUserColor
 * @property secondUserChar
 * @property secondUserAvatar
 * @property secondUserColor
 * @property schedId
 * @property isRecurringDaily
 * @property isRecurringWeekly
 * @property isRecurringMonthly
 * @property isPending
 * @property scheduledStartTimestamp
 * @property scheduledEndTimestamp
 * @property scheduledTimestampFormatted
 * @property scheduledMeetingStatus
 */
data class MeetingRoomItem constructor(
    val chatId: Long,
    val title: String,
    val lastMessage: String? = null,
    val isLastMessageVoiceClip: Boolean = false,
    val isLastMessageGeolocation: Boolean = false,
    val unreadCount: Int = 0,
    val hasPermissions: Boolean = false,
    val isActive: Boolean = false,
    val isPublic: Boolean = false,
    val isMuted: Boolean = false,
    val lastTimestamp: Long = 0L,
    val lastTimestampFormatted: String? = null,
    val highlight: Boolean = false,
    val firstUserChar: String? = null,
    val firstUserAvatar: String? = null,
    val firstUserColor: Int? = null,
    val secondUserChar: String? = null,
    val secondUserAvatar: String? = null,
    val secondUserColor: Int? = null,
    val schedId: Long? = null,
    val isRecurringDaily: Boolean = false,
    val isRecurringWeekly: Boolean = false,
    val isRecurringMonthly: Boolean = false,
    val isPending: Boolean = false,
    val scheduledStartTimestamp: Long? = null,
    val scheduledEndTimestamp: Long? = null,
    val scheduledTimestampFormatted: String? = null,
    val scheduledMeetingStatus: ScheduledMeetingStatus? = null,
) {

    fun isSingleMeeting(): Boolean =
        secondUserChar == null

    fun isScheduledMeeting(): Boolean =
        schedId != null

    fun isRecurring(): Boolean =
        isRecurringDaily || isRecurringWeekly || isRecurringMonthly

    fun hasOngoingCall(): Boolean =
        (scheduledMeetingStatus is ScheduledMeetingStatus.Joined
                && scheduledMeetingStatus.callStartTimestamp != null)
                || (scheduledMeetingStatus is ScheduledMeetingStatus.NotJoined
                && scheduledMeetingStatus.callStartTimestamp != null)

    fun getCallDuration(): String? =
        when (scheduledMeetingStatus) {
            is ScheduledMeetingStatus.Joined -> scheduledMeetingStatus.callStartTimestamp
            is ScheduledMeetingStatus.NotJoined -> scheduledMeetingStatus.callStartTimestamp
            else -> null
        }?.let { timestamp ->
            Duration.between(
                Instant.ofEpochSecond(timestamp),
                Instant.now()
            ).seconds
        }?.let { duration ->
            String.format(
                "%02d:%02d",
                TimeUnit.SECONDS.toMinutes(duration) % 60,
                TimeUnit.SECONDS.toSeconds(duration) % 60
            )
        }
}
