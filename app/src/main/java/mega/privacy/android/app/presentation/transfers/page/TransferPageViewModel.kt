package mega.privacy.android.app.presentation.transfers.page

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import mega.privacy.android.app.presentation.manager.model.TransfersTab
import javax.inject.Inject

@HiltViewModel
class TransferPageViewModel @Inject constructor(

) : ViewModel() {
    private val _state = MutableStateFlow(TransferPageUiState())

    /**
     * State
     */
    val state = _state.asStateFlow()

    /**
     * Set the current transfers tab to the UI state
     *
     * @param tab transfer tab to set
     */
    fun setTransfersTab(tab: TransfersTab) {
        _state.update { it.copy(transfersTab = tab) }
    }

    /**
     * Transfer tab
     */
    val transferTab: TransfersTab
        get() = state.value.transfersTab
}

/**
 * Transfer page ui state
 *
 * @property transfersTab
 */
data class TransferPageUiState(
    val transfersTab: TransfersTab = TransfersTab.PENDING_TAB,
)