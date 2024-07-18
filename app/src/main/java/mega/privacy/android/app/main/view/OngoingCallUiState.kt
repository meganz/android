package mega.privacy.android.app.main.view

import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.entity.call.ChatCall
import java.time.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlin.time.toDuration

/**
 * Call in progress UI state
 *
 * @property currentCall Current call
 * @property isShown Is show
 * @property themeMode Theme mode
 */
data class OngoingCallUiState(
    val currentCall: ChatCall? = null,
    val isShown: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.System
) {
    /**
     * Get duration based in the initial timestamp
     *
     * @return [Duration]
     */
    fun getDurationFromInitialTimestamp(): Duration? {
        return currentCall?.initialTimestamp?.takeIf { it != 0L }?.let {
            val currentDuration = Instant.now().epochSecond.toDuration(DurationUnit.SECONDS)
            val initialDuration = it.seconds
            currentDuration.minus(initialDuration)
        }
    }

}