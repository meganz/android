package mega.privacy.android.domain.entity.continuewhereleftoff

import java.util.concurrent.TimeUnit

/**
 * Audio/video items within this many milliseconds of completion are considered effectively
 * watched-through and are excluded from the Continue Where Left Off carousel.
 */
val CWLO_NEAR_COMPLETION_THRESHOLD_MS: Long = TimeUnit.SECONDS.toMillis(3)

/**
 * An audio/video item must be played past this position before it is added to the Continue
 * Where Left Off carousel. Items that were only briefly opened (<= 15 seconds) are not
 * considered resumable and are not surfaced back to the user.
 */
val CWLO_MINIMUM_PLAYBACK_THRESHOLD_MS: Long = TimeUnit.SECONDS.toMillis(15)

/**
 * Document items (PDF, text editor) read to at least this fraction of their length are
 * considered effectively read-through and are excluded from the Continue Where Left Off
 * carousel, mirroring [CWLO_NEAR_COMPLETION_THRESHOLD_MS] for audio/video.
 */
const val CWLO_NEAR_COMPLETION_FRACTION: Float = 0.9f

/**
 * Audio items with this much (or less) playback remaining count as played through: their saved
 * position is discarded and they leave the Continue Where Left Off carousel. Wider than
 * [CWLO_NEAR_COMPLETION_THRESHOLD_MS] because long-form audio (podcasts, audiobooks) commonly
 * ends with outros the user should not have to sit through to mark the item finished.
 */
val AUDIO_RESUME_FINISHED_THRESHOLD_MS: Long = TimeUnit.SECONDS.toMillis(30)

/**
 * Interval at which the playing audio item's position is persisted to storage. Position loss
 * on a hard kill or crash is bounded by this; session-ending events (pause, stop, seek, track
 * change, app backgrounding) force an immediate save on top of the periodic one.
 */
val AUDIO_PLAYBACK_INFO_SAVE_INTERVAL_MS: Long = TimeUnit.SECONDS.toMillis(3)
