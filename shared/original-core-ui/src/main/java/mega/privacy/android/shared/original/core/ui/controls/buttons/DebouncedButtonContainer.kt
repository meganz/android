package mega.privacy.android.shared.original.core.ui.controls.buttons

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * A container that manages debounce state for an externally provided Button (or any clickable composable).
 *
 * @param onClick the action to perform when the button is clicked (debounced).
 * @param debounceDuration duration to disable clicks after each invocation
 * @param scope the coroutine scope where the debounce will be done
 * @param content a composable that takes [isClickAllowed] and [debouncedOnClick] parameters to render the actual Button.
 */
@Composable
fun DebouncedButtonContainer(
    onClick: () -> Unit,
    debounceDuration: Duration = 800.milliseconds,
    scope: CoroutineScope = rememberCoroutineScope(),
    content: @Composable (
        isClickAllowed: Boolean,
        debouncedOnClick: () -> Unit,
    ) -> Unit,
) {
    var isClickAllowed by remember { mutableStateOf(true) }

    val debouncedClick: () -> Unit = {
        if (isClickAllowed) {
            isClickAllowed = false
            onClick()
            scope.launch {
                delay(debounceDuration)
                isClickAllowed = true
            }
        }
    }

    content(isClickAllowed, debouncedClick)
}