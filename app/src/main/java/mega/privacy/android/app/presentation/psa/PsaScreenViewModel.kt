package mega.privacy.android.app.presentation.psa

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import mega.privacy.android.domain.usecase.psa.DismissPsaUseCase
import mega.privacy.android.domain.usecase.psa.SetDisplayedPsaUseCase
import javax.inject.Inject

/**
 * Psa view model
 *
 * legacyState - We are using this instead of the actual implementation to unify the experience while legacy screens still exist
 */
@HiltViewModel
class PsaScreenViewModel @Inject constructor(
    private val dismissPsaUseCase: DismissPsaUseCase,
    private val setDisplayedPsaUseCase: SetDisplayedPsaUseCase,
) : ViewModel() {

    suspend fun setDisplayed(psaId: Int) {
        setDisplayedPsaUseCase(psaId)
    }


    /**
     * Mark as seen
     *
     * @param psaId
     */
    fun markAsSeen(psaId: Int) = viewModelScope.launch {
        dismissPsaUseCase(psaId)
    }
}
