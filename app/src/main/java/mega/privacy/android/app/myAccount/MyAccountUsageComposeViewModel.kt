package mega.privacy.android.app.myAccount

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/**
 * View Model for MyAccountUsageComposeFragment
 */
@HiltViewModel
class MyAccountUsageComposeViewModel @Inject constructor() : ViewModel() {

    /**
     * private mutable UI state
     */
    private val _uiState = MutableStateFlow(MyAccountUsageComposeUiState())

    /**
     * public immutable UI State for view
     */
    val uiState = _uiState.asStateFlow()

    init {
        // Initialize state
    }
}

