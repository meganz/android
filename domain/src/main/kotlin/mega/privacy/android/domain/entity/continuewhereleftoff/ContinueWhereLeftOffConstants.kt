package mega.privacy.android.domain.entity.continuewhereleftoff

import java.util.concurrent.TimeUnit

/**
 * Audio/video items within this many milliseconds of completion are considered effectively
 * watched-through and are excluded from the Continue Where Left Off carousel.
 */
val CWLO_NEAR_COMPLETION_THRESHOLD_MS: Long = TimeUnit.SECONDS.toMillis(2)
