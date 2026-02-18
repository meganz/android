package mega.privacy.android.core.sharedcomponents.coroutine

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import java.util.concurrent.ConcurrentHashMap

@Composable
fun LaunchedOnceEffect(
    key: Any = Unit,
    block: suspend () -> Unit,
) {
    var hasLaunched by rememberSaveable(key) { mutableStateOf(false) }

    LaunchedEffect(hasLaunched) {
        if (!hasLaunched) {
            hasLaunched = true
            block()
        }
    }
}

/**
 * Runs [block] at most once per app process for the given [key].
 *
 * Unlike [LaunchedOnceEffect], the execution is shared across the whole app: if the same [key]
 * is used in different screens, [block] runs only the first time.
 * Useful, for instance, to track analytic events that can be triggered in different screens
 * @param key Identifier for this effect. Use the same key in all composables that should share the single execution.
 * @param block
 */
@Composable
fun LaunchedOncePerAppEffect(
    key: Any,
    block: suspend () -> Unit,
) {
    LaunchedEffect(key) {
        if (executedKeysPerApp.add(key)) {
            block()
        }
    }
}

fun resetLaunchedOncePerAppEffect(key: Any) {
    executedKeysPerApp.remove(key)
}

private val executedKeysPerApp = ConcurrentHashMap.newKeySet<Any>()