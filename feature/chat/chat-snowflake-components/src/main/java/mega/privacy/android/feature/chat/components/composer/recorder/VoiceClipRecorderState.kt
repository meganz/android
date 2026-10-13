package mega.privacy.android.feature.chat.components.composer.recorder

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/**
 * State of the voice clip recorder.
 *
 * The record button changes this state while the user holds, slides and releases it. The
 * recorder overlay reads it to show the timer, the slide to cancel hint and the lock control.
 */
@Stable
class VoiceClipRecorderState internal constructor(
    private val lockThresholdPx: Float,
    private val cancelThresholdPx: Float,
    private val timeSource: TimeSource = TimeSource.Monotonic,
) {

    /**
     * True from the [VoiceClipRecordEvent.Start] event until the recording is cancelled or
     * finished.
     */
    var isRecording by mutableStateOf(false)
        private set

    /**
     * True when the user locked the recording. A locked recording continues after the user
     * releases the record button.
     */
    var isLocked by mutableStateOf(false)
        private set

    /**
     * Distance that the finger moved from the press position, in px. Only one axis is not zero:
     * x is zero or less (towards the start), y is zero or less (up).
     */
    internal var dragOffset by mutableStateOf(Offset.Zero)
        private set

    /**
     * True when the user released the record button before the minimum recording duration.
     */
    internal var showTapAndHoldHint by mutableStateOf(false)

    private var startMark: TimeMark? = null

    /**
     * Time since the recording started, or zero when there is no recording.
     */
    internal val elapsed: Duration
        get() = startMark?.elapsedNow() ?: Duration.ZERO

    internal fun start(): VoiceClipRecordEvent? {
        if (isRecording) return null
        isRecording = true
        isLocked = false
        dragOffset = Offset.Zero
        showTapAndHoldHint = false
        startMark = timeSource.markNow()
        return VoiceClipRecordEvent.Start
    }

    /**
     * @param offset Distance from the press position, in px, with positive x towards the end of
     * the layout direction.
     */
    internal fun drag(offset: Offset): VoiceClipRecordEvent? {
        if (!isRecording || isLocked) return null
        val towardsStart = (-offset.x).coerceAtLeast(0f)
        val up = (-offset.y).coerceAtLeast(0f)
        dragOffset = when {
            towardsStart == 0f && up == 0f -> Offset.Zero
            towardsStart >= up -> Offset(-towardsStart, 0f)
            else -> Offset(0f, -up)
        }
        return when {
            towardsStart > cancelThresholdPx -> {
                reset()
                VoiceClipRecordEvent.Cancel
            }

            up > lockThresholdPx -> {
                isLocked = true
                dragOffset = Offset.Zero
                VoiceClipRecordEvent.Lock
            }

            else -> null
        }
    }

    internal fun release(): VoiceClipRecordEvent? {
        if (!isRecording || isLocked) return null
        val isTooShort = elapsed < MinimumRecordingDuration
        reset()
        showTapAndHoldHint = isTooShort
        return if (isTooShort) VoiceClipRecordEvent.Cancel else VoiceClipRecordEvent.Finish
    }

    internal fun cancel(): VoiceClipRecordEvent? {
        if (!isRecording) return null
        reset()
        return VoiceClipRecordEvent.Cancel
    }

    internal fun finish(): VoiceClipRecordEvent? {
        if (!isRecording) return null
        reset()
        return VoiceClipRecordEvent.Finish
    }

    private fun reset() {
        isRecording = false
        isLocked = false
        dragOffset = Offset.Zero
        startMark = null
    }
}

/**
 * Creates and remembers a [VoiceClipRecorderState].
 */
@Composable
fun rememberVoiceClipRecorderState(): VoiceClipRecorderState {
    val density = LocalDensity.current
    return remember(density) {
        with(density) {
            VoiceClipRecorderState(
                lockThresholdPx = VoiceClipRecorderDefaults.LockThreshold.toPx(),
                cancelThresholdPx = VoiceClipRecorderDefaults.CancelThreshold.toPx(),
            )
        }
    }
}

/**
 * A shorter recording is not sent, because the user most likely tapped the record button by
 * mistake.
 */
internal val MinimumRecordingDuration = 1.seconds
