package mega.privacy.android.app.appstate.global.call

import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.entity.call.ChatCall
import java.time.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlin.time.toDuration

/**
 * UI state for the global return-to-call banner.
 *
 * @property currentCall The ongoing call, or null when there is no call in progress.
 * @property themeMode The current theme mode used to render the banner.
 */
data class OngoingCallBannerUiState(
    val currentCall: ChatCall?,
    val themeMode: ThemeMode,
) {
    /**
     * Duration elapsed since the call started, derived from its initial timestamp.
     *
     * @return the elapsed [Duration], or null when there is no call or no valid timestamp.
     */
    fun getDurationFromInitialTimestamp(): Duration? =
        currentCall?.initialTimestamp?.takeIf { it != 0L }?.let { initialTimestamp ->
            val currentDuration = Instant.now().epochSecond.toDuration(DurationUnit.SECONDS)
            currentDuration.minus(initialTimestamp.seconds)
        }
}
