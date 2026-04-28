package mega.privacy.android.navigation.contract.suppression

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.NavEntryDecorator

/**
 * Returns an [OverlaySuppressionNavEntryDecorator] that is remembered across
 * recompositions.
 *
 * This decorator reads overlay suppression metadata from each visible entry
 * and writes to the shared [OverlaySuppressionState]. When an entry that
 * declares [withOverlaySuppression] enters composition, suppression is
 * activated. When it leaves, suppression is cleared.
 *
 * @param suppressionState The shared state holder read by the event
 *   processing layer to decide whether to defer suppressable events.
 */
@Composable
fun <T : Any> rememberOverlaySuppressionNavEntryDecorator(
    suppressionState: OverlaySuppressionState,
): OverlaySuppressionNavEntryDecorator<T> {
    return remember(suppressionState) {
        OverlaySuppressionNavEntryDecorator(suppressionState)
    }
}

/**
 * Decorator that bridges NavEntry metadata to the event processing layer.
 *
 * For each visible entry, it checks whether [suppressesOverlays] is `true`.
 * If so, it activates suppression via [OverlaySuppressionState] and clears
 * it when the entry leaves composition.
 *
 * Overlay entries (dialogs, bottom sheets) do not declare this metadata,
 * so they have no effect on the suppression state.
 *
 * @param suppressionState The shared state holder read by the event
 */
class OverlaySuppressionNavEntryDecorator<T : Any>(
    private val suppressionState: OverlaySuppressionState,
) : NavEntryDecorator<T>(
    onPop = { _ -> },
    decorate = { entry ->
        val suppressionType = entry.suppressesOverlays()
        if (suppressionType != SuppressionType.None) {
            DisposableEffect(Unit) {
                suppressionState.setSuppressing(suppressionType)
                onDispose { suppressionState.setSuppressing(SuppressionType.None) }
            }
        }
        entry.Content()
    },
)
