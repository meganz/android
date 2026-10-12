package mega.privacy.android.feature.photos.presentation.timeline.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

/**
 * Calls [onReveal] each time [revealRequest] increases, which happens when the user taps the
 * Camera Uploads warning icon in the toolbar. The banner that explains the warning is the first
 * item of the timeline, so [onReveal] must scroll the timeline to the top.
 *
 * The first recorded request is the current value of [revealRequest] and not zero, because a
 * [LaunchedEffect] runs again every time it re-enters composition. A baseline of zero therefore
 * scrolls the timeline to the top after each rotation or tab switch that follows a tap, and the
 * user loses the scroll position.
 *
 * The recorded request also decreases with [revealRequest]. The baseline survives process death
 * because it is saved state, but [revealRequest] comes from a ViewModel and restarts at zero, so
 * a baseline that only increases makes the next taps do nothing.
 *
 * @param revealRequest the number of taps on the warning icon so far
 * @param onReveal scrolls the timeline so that the banner is visible
 */
@Composable
internal fun RevealWarningBannerEffect(
    revealRequest: Int,
    onReveal: suspend () -> Unit,
) {
    var revealedRequest by rememberSaveable { mutableIntStateOf(revealRequest) }
    LaunchedEffect(revealRequest) {
        when {
            revealRequest < revealedRequest -> revealedRequest = revealRequest

            revealRequest > revealedRequest -> {
                revealedRequest = revealRequest
                onReveal()
            }
        }
    }
}
