package mega.privacy.android.app.appstate.global

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.StateEventWithContentConsumed
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import mega.android.core.ui.model.SnackbarAttributes
import mega.privacy.android.navigation.contract.queue.SnackbarEventQueue
import javax.inject.Inject

@HiltViewModel
class SnackbarEventsViewModel @Inject constructor(
    private val snackbarEventQueue: SnackbarEventQueue,
) : ViewModel() {
    private val snackbarEventConsumedSignal =
        MutableSharedFlow<StateEventWithContentConsumed>()

    val snackbarEventState: StateFlow<StateEventWithContent<SnackbarAttributes>> by lazy {
        merge(
            monitorEventQueue(),
            snackbarEventConsumedSignal
        ).stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = consumed()
        )
    }

    private fun monitorEventQueue() = flow {
        for (event in snackbarEventQueue.eventQueue) {
            emit(triggered(event))
            awaitEventConsumed()
        }
    }

    private suspend fun awaitEventConsumed() {
        snackbarEventConsumedSignal.first() == consumed()
    }

    fun consumeEvent() {
        viewModelScope.launch {
            snackbarEventConsumedSignal.emit(consumed())
        }
    }
}