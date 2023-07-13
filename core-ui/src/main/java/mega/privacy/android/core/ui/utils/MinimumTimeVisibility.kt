package mega.privacy.android.core.ui.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

/**
 * Shows the content after being shown for at least [waitDurationToShow] and for at least [minimumShowedDuration]
 * This can be used to show progress feedback for a long but unknown duration, to avoid showing it for only a fraction of time.
 */
@Composable
fun MinimumTimeVisibility(
    visible: Boolean,
    waitDurationToShow: Duration = DEFAULT_WAIT_MILLIS_TO_SHOW.toDuration(DurationUnit.MILLISECONDS),
    minimumShowedDuration: Duration = DEFAULT_MINIMUM_SHOW_MILLIS.toDuration(DurationUnit.MILLISECONDS),
    content: @Composable () -> Unit,
) {
    var show by rememberSaveable { mutableStateOf(false) } // savable because we don't want to wait on screen rotation
    var canHide by rememberSaveable { mutableStateOf(true) }
    val updatedVisible by rememberUpdatedState(visible)

    LaunchedEffect(show) {
        if (show) {
            canHide = false
            delay(minimumShowedDuration)
            canHide = true
            show = updatedVisible
        }
    }
    LaunchedEffect(visible) {
        if (visible) {
            delay(waitDurationToShow)
            show = true
        } else if (canHide) {
            show = false
        }
    }

    if (show) {
        content()
    }
}

private const val DEFAULT_WAIT_MILLIS_TO_SHOW = 800L
private const val DEFAULT_MINIMUM_SHOW_MILLIS = 1200L