package mega.privacy.android.core.sharedcomponents.button

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Window in which a repeat invocation of a debounced click callback is dropped. Long enough to
 * cover the gap between a tap and the opened sheet/chooser taking over the input (AND-24231).
 */
val DEFAULT_DEBOUNCE_DURATION = 800.milliseconds

/**
 * Returns a stable callback that drops repeat events with the same [key] within
 * [debounceDuration], while events with a different key pass through independently.
 *
 * Use it when the clickable elements are rendered inside a pre-built component that only accepts
 * a shared callback, so each of them cannot be wrapped individually.
 *
 * @param key groups events into independent debounce windows (e.g. a menu action's test tag)
 * @param debounceDuration duration during which repeat events with the same key are dropped
 * @param onEvent the debounced callback
 */
@Composable
fun <T> rememberDebouncedCallback(
    key: (T) -> Any,
    debounceDuration: Duration = DEFAULT_DEBOUNCE_DURATION,
    onEvent: (T) -> Unit,
): (T) -> Unit {
    val scope = rememberCoroutineScope()
    val currentKey by rememberUpdatedState(key)
    val currentOnEvent by rememberUpdatedState(onEvent)
    val cooldowns = remember { mutableMapOf<Any, Job>() }
    return remember(debounceDuration) {
        { value: T ->
            val eventKey = currentKey(value)
            if (cooldowns[eventKey]?.isActive != true) {
                cooldowns[eventKey] = scope.launch {
                    delay(debounceDuration)
                    cooldowns.remove(eventKey)
                }
                currentOnEvent(value)
            }
        }
    }
}
