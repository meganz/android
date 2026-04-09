package mega.privacy.android.shared.ads.rewarded

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/**
 * ViewModel for the Rewarded Ad Gate.
 *
 * Manages dialog state only. The pending action lambda is held by [RewardedAdGateHandler]
 * in the Compose scope, not here, to avoid stale references after config changes.
 */
@HiltViewModel
class RewardedAdGateViewModel @Inject constructor() : ViewModel() {

    val uiState: StateFlow<RewardedAdGateUiState>
        field = MutableStateFlow(RewardedAdGateUiState())

    fun showDialog() {
        uiState.update { it.copy(showDialog = true, errorMessage = null) }
    }

    fun dismiss() {
        uiState.update { RewardedAdGateUiState() }
    }

    fun setLoading() {
        uiState.update { it.copy(isLoading = true, errorMessage = null) }
    }

    fun setLoadingComplete() {
        uiState.update { it.copy(isLoading = false) }
    }

    fun setError(message: String) {
        uiState.update { it.copy(isLoading = false, errorMessage = message) }
    }
}
