package mega.privacy.android.core.sharedcomponents.coroutine

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

@Composable
fun LaunchedOnceEffect(
    key: Any = Unit,
    block: suspend () -> Unit
) {
    var hasLaunched by rememberSaveable(key) { mutableStateOf(false) }

    LaunchedEffect(hasLaunched) {
        if (!hasLaunched) {
            hasLaunched = true
            block()
        }
    }
}