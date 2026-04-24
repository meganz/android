package mega.privacy.android.feature.contact.add

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import mega.privacy.android.feature.contact.add.model.AddContactUiState
import mega.privacy.android.navigation.contract.viewmodel.asUiStateFlow
import javax.inject.Inject

/**
 * Add contact view model
 *
 */
@HiltViewModel
class AddContactViewModel @Inject constructor() : ViewModel() {

    /**
     * Ui state
     */
    val uiState: StateFlow<AddContactUiState> by lazy(LazyThreadSafetyMode.NONE) {
        flow<AddContactUiState> { awaitCancellation() }
            .asUiStateFlow(viewModelScope, AddContactUiState.Loading)
    }
}
