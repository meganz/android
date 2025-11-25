package mega.privacy.android.app.appstate.global.event

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation3.runtime.NavKey
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import mega.privacy.android.navigation.contract.queue.NavigationQueueEvent
import mega.privacy.android.navigation.contract.viewmodel.asUiStateFlow
import javax.inject.Inject

@HiltViewModel
class NavigationEventViewModel @Inject constructor(
    private val navigationEventQueueReceiver: NavigationEventQueueReceiver,
) : ViewModel() {
    private var acknowledged: CompletableDeferred<Unit>? = null

    val navigationEvents: StateFlow<StateEventWithContent<List<NavKey>>> by lazy {
        flow {
            for (event in navigationEventQueueReceiver.events) {
                val queueEvent = event() as? NavigationQueueEvent
                if (queueEvent != null) {
                    acknowledged = CompletableDeferred()
                    emit(triggered(queueEvent.keys))
                    acknowledged?.await()
                    emit(consumed())
                }
            }
        }.asUiStateFlow(
            viewModelScope,
            initialValue = consumed()
        )
    }

    fun eventHandled() {
        acknowledged?.complete(Unit)
    }
}