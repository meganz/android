package mega.privacy.android.app.appstate.global.event

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import mega.privacy.android.navigation.contract.queue.NavigationQueueEvent
import mega.privacy.android.navigation.contract.queue.QueueEvent
import mega.privacy.android.navigation.contract.queue.dialog.AppDialogEvent
import mega.privacy.android.navigation.contract.viewmodel.asUiStateFlow
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class QueueEventViewModel @Inject constructor(
    private val navigationEventQueueReceiver: NavigationEventQueueReceiver,
) : ViewModel() {
    private var eventHandledSignal: CompletableDeferred<Unit>? = null
    private var eventDisplayedSignal: CompletableDeferred<Unit>? = null

    val navigationEvents: StateFlow<StateEventWithContent<QueueEvent>> by lazy {
        flow {
            for (event in navigationEventQueueReceiver.events) {
                val queueEvent = event()
                Timber.d("Collected event from queue: $queueEvent")

                if (queueEvent != null) {
                    eventDisplayedSignal = CompletableDeferred()
                }

                when (queueEvent) {
                    is NavigationQueueEvent -> {
                        emit(triggered(queueEvent))
                        eventDisplayedSignal?.await()
                        emit(consumed())
                    }

                    is AppDialogEvent -> {
                        eventHandledSignal = CompletableDeferred()
                        emit(triggered(queueEvent))
                        eventDisplayedSignal?.await()
                        emit(consumed())
                        eventHandledSignal?.await()
                    }

                    else -> {}
                }

            }
        }.asUiStateFlow(
            viewModelScope,
            initialValue = consumed()
        )
    }

    fun eventDisplayed() {
        eventDisplayedSignal?.complete(Unit)
    }

    fun eventHandled() {
        eventHandledSignal?.complete(Unit)
    }
}