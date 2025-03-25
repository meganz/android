package mega.privacy.android.app.main.ads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import mega.privacy.android.domain.usecase.advertisements.SetAdsClosingTimestampUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel to handle the ads container.
 */
@HiltViewModel
class AdsContainerViewModel @Inject constructor(
    private val setAdsClosingTimestampUseCase: SetAdsClosingTimestampUseCase,
) : ViewModel() {

    /**
     * Callback to be called when the ads are closed.
     */
    fun handleAdsClosed() {
        viewModelScope.launch {
            runCatching {
                setAdsClosingTimestampUseCase(System.currentTimeMillis())
            }.onFailure {
                Timber.e(it, "Error setting ads closing timestamp")
            }
        }
    }
}