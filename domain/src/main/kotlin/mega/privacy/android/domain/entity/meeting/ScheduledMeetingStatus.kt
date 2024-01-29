package mega.privacy.android.domain.entity.meeting

import java.time.Instant
import kotlin.time.Duration

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
    class NotJoined(callDuration: Duration?) : ScheduledMeetingStatus() {
        val callStartTimestamp = callDuration
            ?.takeIf { it.inWholeSeconds > 0 }
            ?.let { Instant.now().minusSeconds(it.inWholeSeconds).epochSecond }
    }

    /**
     * Call in progress and I am participating
     *
     * @property callStartTimestamp     Call start timestamp
     */
    class Joined(callDuration: Duration?) : ScheduledMeetingStatus() {
        val callStartTimestamp = callDuration
            ?.takeIf { it.inWholeSeconds > 0 }
            ?.let { Instant.now().minusSeconds(it.inWholeSeconds).epochSecond }
    }
}
