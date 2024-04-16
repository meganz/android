package mega.privacy.android.app.presentation.hidenode

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import mega.privacy.android.domain.usecase.SetHiddenNodesOnboardedUseCase
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
internal class HiddenNodesOnboardingViewModel @Inject constructor(
    private val setHiddenNodesOnboardedUseCase: SetHiddenNodesOnboardedUseCase,
) : ViewModel() {
    fun setHiddenNodesOnboarded() = viewModelScope.launch {
        runCatching {
            setHiddenNodesOnboardedUseCase()
        }.onFailure {
            Timber.e(it)
        }
    }
}
