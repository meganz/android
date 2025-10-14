package mega.privacy.android.app.appstate.content.transfer

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import javax.inject.Inject

@HiltViewModel
class AppTransferViewModel @Inject constructor(

) : ViewModel() {
    private val _state = MutableStateFlow(AppTransferUiState())
    val state = _state.asStateFlow()

    fun setTransferEvent(event: TransferTriggerEvent) {
        _state.update {
            it.copy(transferEvent = triggered(event))
        }
    }

    fun consumedTransferEvent() {
        _state.update {
            it.copy(transferEvent = consumed())
        }
    }
}