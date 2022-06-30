package mega.privacy.android.app.presentation.extensions

import androidx.lifecycle.Observer
import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext


/**
 * Get a mutable state flow for a specific key.
 *
 * Uses the SavedStateHandle.getLiveData() function internally to observe changes to a value, and
 * propagate changes from the mutable state flow to the SavedStateHandle
 *
 * @param T generic type of the value being observed
 * @param scope coroutine scope for the resulting state flow
 * @param key string key used to store and retrieve the value from the SavedStateHandle
 * @param initialValue initial value for the state flow
 * @return A MutableStateFlow
 */
fun <T> SavedStateHandle.getStateFlow(
    scope: CoroutineScope,
    key: String,
    initialValue: T
): MutableStateFlow<T> {
    val liveData = getLiveData(key, initialValue)
    val stateFlow = MutableStateFlow(initialValue)

    val observer = Observer<T> { value ->
        if (value != stateFlow.value) {
            stateFlow.value = value
        }
    }
    liveData.observeForever(observer)

    stateFlow.onCompletion {
        withContext(Dispatchers.Main.immediate) {
            liveData.removeObserver(observer)
        }
    }.onEach { value ->
        withContext(Dispatchers.Main.immediate) {
            if (liveData.value != value) {
                liveData.value = value
            }
        }
    }.launchIn(scope)

    return stateFlow
}