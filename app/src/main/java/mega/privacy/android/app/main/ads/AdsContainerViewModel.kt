package mega.privacy.android.app.main.ads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
    private val _isAdsLoaded = MutableStateFlow<Boolean?>(null)
    val isAdsLoaded: StateFlow<Boolean?> = _isAdsLoaded

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

    fun setAdsLoaded(isLoaded: Boolean) {
        if (isLoaded) {
            _isAdsLoaded.value = true
        } else if (_isAdsLoaded.value == null) {
            // in case of ad failed to load, we only set the value to false if no ads loaded before
            // otherwise we keep the value to true because the old ad is still showing
            _isAdsLoaded.value = false
        }
    }

    fun isAdsLoaded() = _isAdsLoaded.value == true
}