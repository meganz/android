package mega.privacy.android.core.sharedcomponents.coroutine

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Launches a new coroutine and repeats `collectBlock` every time the Fragment's viewLifecycleOwner
 * is in and out of `minActiveState` lifecycle state.
 */
inline fun <T> LifecycleOwner.collectFlow(
    targetFlow: Flow<T>,
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
    crossinline collectBlock: (T) -> Unit
) {
    this.lifecycleScope.launch {
        targetFlow.flowWithLifecycle(this@collectFlow.lifecycle, minActiveState)
            .catch {
                Timber.e(it)
            }
            .collect {
                collectBlock(it)
            }
    }
}
