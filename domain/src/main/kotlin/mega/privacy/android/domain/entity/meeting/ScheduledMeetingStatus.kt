package mega.privacy.android.domain.entity.meeting

import java.time.Instant

/**
 * Scheduled meeting status.
 */
sealed class ScheduledMeetingStatus {

    /**
     *  Call not started
     */
    object NotStarted : ScheduledMeetingStatus()

    /**
     * Call in progress and I am not participating
     *
     * @property callStartTimestamp     Call start timestamp
     */
    class NotJoined(callDuration: Long?) : ScheduledMeetingStatus() {
        val callStartTimestamp = callDuration
            ?.takeIf { it > 0 }
            ?.let { Instant.now().minusSeconds(it).epochSecond }
    }

    /**
     * Call in progress and I am participating
     *
     * @property callStartTimestamp     Call start timestamp
     */
    class Joined(callDuration: Long?) : ScheduledMeetingStatus() {
        val callStartTimestamp = callDuration
            ?.takeIf { it > 0 }
            ?.let { Instant.now().minusSeconds(it).epochSecond }
    }
}
