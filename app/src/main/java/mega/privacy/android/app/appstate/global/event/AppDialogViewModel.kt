package mega.privacy.android.app.appstate.global.event

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import mega.privacy.android.navigation.contract.dialog.AppDialogEvent
import mega.privacy.android.shared.original.core.ui.utils.asUiStateFlow
import javax.inject.Inject

@HiltViewModel
class AppDialogViewModel @Inject constructor(
    private val appDialogsEventQueueReceiver: AppDialogsEventQueueReceiver,
) : ViewModel() {
    private val eventHandledSignal = MutableSharedFlow<Unit>()
    private val dialogDisplayedSignal = MutableSharedFlow<StateEventWithContent<AppDialogEvent>>()

    val dialogEvents: StateFlow<StateEventWithContent<AppDialogEvent>> by lazy {
        merge(
            flow {
                for (event in appDialogsEventQueueReceiver.events) {
                    emit(triggered(event))
                    awaitHandledEvent()
                }
            },
            dialogDisplayedSignal
        ).asUiStateFlow(
            viewModelScope,
            initialValue = consumed()
        )
    }

    private suspend fun awaitHandledEvent() {
        eventHandledSignal.first()
    }

    fun dialogDisplayed() {
        viewModelScope.launch {
            dialogDisplayedSignal.emit(consumed())
        }
    }

    fun eventHandled() {
        viewModelScope.launch {
            eventHandledSignal.emit(Unit)
        }
    }

}