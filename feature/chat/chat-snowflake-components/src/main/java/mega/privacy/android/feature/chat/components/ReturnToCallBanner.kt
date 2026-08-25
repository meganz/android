package mega.privacy.android.feature.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.TextColor
import mega.android.core.ui.tokens.theme.DSTokens
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlin.time.toDuration

/**
 * Ticking return-to-call banner.
 *
 * Stateful wrapper that derives the elapsed call time from [callStartTimestampSeconds] and updates
 * it every second, delegating the rendering to the stateless [ReturnToCallBanner] overload.
 *
 * @param text Banner label describing the ongoing call.
 * @param callStartTimestampSeconds Unix timestamp (in seconds) when the call started, or null when
 * no elapsed time should be shown.
 * @param onClick Invoked when the banner is tapped, typically to return to the call.
 * @param modifier [Modifier]
 */
@Composable
fun ReturnToCallBanner(
    text: String,
    callStartTimestampSeconds: Long?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val elapsed by produceState<Duration?>(
        initialValue = callStartTimestampSeconds.elapsedFromNow(),
        key1 = callStartTimestampSeconds,
    ) {
        while (callStartTimestampSeconds != null) {
            value = callStartTimestampSeconds.elapsedFromNow()
            delay(1.seconds)
        }
        value = null
    }
    ReturnToCallBanner(
        text = text,
        elapsed = elapsed,
        onClick = onClick,
        modifier = modifier,
    )
}

/**
 * Stateless return-to-call banner.
 *
 * A full-width primary-coloured bar that returns the user to an ongoing call when tapped, showing
 * an optional elapsed-time chronometer when [elapsed] is provided.
 *
 * @param text Banner label describing the ongoing call.
 * @param elapsed Elapsed call time to display, or null to hide the chronometer.
 * @param onClick Invoked when the banner is tapped, typically to return to the call.
 * @param modifier [Modifier]
 */
@Composable
fun ReturnToCallBanner(
    text: String,
    elapsed: Duration?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) = Row(
    modifier = modifier
        .fillMaxWidth()
        .background(DSTokens.colors.button.primary)
        .clickable { onClick() },
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.Center,
) {
    MegaText(
        text = text,
        textColor = TextColor.Inverse,
        style = AppTheme.typography.bodyMedium,
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag(RETURN_TO_CALL_BANNER_TAG),
    )
    elapsed?.let {
        MegaText(
            text = it.formatElapsed(),
            textColor = TextColor.Inverse,
            style = AppTheme.typography.bodyMedium,
            modifier = Modifier.testTag(RETURN_TO_CALL_BANNER_CHRONOMETER_TAG),
        )
    }
}

private fun Long?.elapsedFromNow(): Duration? =
    this?.takeIf { it != 0L }?.let { start ->
        val now = System.currentTimeMillis() / 1000
        (now - start).coerceAtLeast(0).toDuration(DurationUnit.SECONDS)
    }

/**
 * Formats a [Duration] as `m:ss`, or `h:mm:ss` once it reaches an hour.
 */
internal fun Duration.formatElapsed(): String {
    val totalSeconds = inWholeSeconds
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}

/**
 * Test tag for the return-to-call banner label.
 */
const val RETURN_TO_CALL_BANNER_TAG = "return_to_call_banner:label"

/**
 * Test tag for the return-to-call banner elapsed-time chronometer.
 */
const val RETURN_TO_CALL_BANNER_CHRONOMETER_TAG = "return_to_call_banner:chronometer"
