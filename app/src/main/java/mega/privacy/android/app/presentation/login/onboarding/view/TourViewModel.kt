package mega.privacy.android.app.presentation.login.onboarding.view

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import mega.privacy.android.app.main.TourFragment
import mega.privacy.android.domain.usecase.login.SetLogoutInProgressFlagUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * The [ViewModel] class for [TourFragment].
 *
 * @property setLogoutInProgressFlagUseCase A use case class to set the logout progress status.
 */
@HiltViewModel
class TourViewModel @Inject constructor(
    private val setLogoutInProgressFlagUseCase: SetLogoutInProgressFlagUseCase,
) : ViewModel() {

    internal fun clearLogoutProgressFlag() {
        viewModelScope.launch {
            Timber.d("Clearing the logout progress status")
            runCatching { setLogoutInProgressFlagUseCase(false) }
                .onFailure { Timber.e("Failed to set the logout progress status", it) }
        }
    }
}
