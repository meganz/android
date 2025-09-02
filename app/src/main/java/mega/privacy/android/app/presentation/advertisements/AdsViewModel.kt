package mega.privacy.android.app.presentation.advertisements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.ads.admanager.AdManagerAdRequest
import com.google.android.ump.ConsentInformation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * View model of [AdsBannerView]
 */
@HiltViewModel
class AdsViewModel @Inject constructor(
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
    private val consentInformation: ConsentInformation,
) : ViewModel() {
    private val _request = MutableStateFlow<AdManagerAdRequest?>(null)
    private val _isAdsFeatureEnabled = MutableStateFlow<Boolean?>(null)

    /**
     * Flow to provide the AdRequest to be used in the AdManager.
     */
    val request = _request.asStateFlow()

    private var refreshAdsJob: Job? = null
    private var lastFetchTime = -1L
    private val mutex = Mutex()

    /**
     * Schedule periodic refresh of ads if ads are enabled and user consent is given.
     */
    fun scheduleRefreshAds() {
        if (refreshAdsJob?.isActive == true) return
        refreshAdsJob?.cancel()
        refreshAdsJob = viewModelScope.launch {
            if (isAdsEnabled() && consentInformation.canRequestAds()) {
                createNewAdRequestIfNeeded()
                while (isActive) {
                    delay(MINIMUM_AD_REFRESH_INTERVAL)
                    Timber.d("Refreshing AdRequest")
                    createNewAdRequestIfNeeded()
                }
            }
        }
    }

    fun cancelRefreshAds() {
        refreshAdsJob?.cancel()
    }

    private fun createNewAdRequestIfNeeded() {
        Timber.d("Checking if a new AdRequest is needed")
        if (_request.value == null || System.currentTimeMillis() - lastFetchTime > MINIMUM_AD_REFRESH_INTERVAL) {
            Timber.d("Creating new AdRequest")
            _request.update { AdManagerAdRequest.Builder().build() }
            lastFetchTime = System.currentTimeMillis()
        }
    }

    private suspend fun isAdsEnabled(): Boolean = mutex.withLock {
        if (_isAdsFeatureEnabled.value == null) {
            checkForAdsAvailability()
        }
        return _isAdsFeatureEnabled.value ?: false
    }

    /**
     * Check if the ads feature is enabled.
     */
    private suspend fun checkForAdsAvailability() {
        runCatching {
            _isAdsFeatureEnabled.update {
                getFeatureFlagValueUseCase(ApiFeatures.GoogleAdsFeatureFlag)
            }
            Timber.d("Ads feature enabled: ${_isAdsFeatureEnabled.value}")
        }.onFailure {
            _isAdsFeatureEnabled.update { false }
            Timber.e(it, "Error getting feature flag value")
        }
    }

    companion object {
        /**
         * Minimum interval for ad refresh in milliseconds.
         *
         * https://support.google.com/admanager/answer/6022114?hl=en
         */
        const val MINIMUM_AD_REFRESH_INTERVAL = 30_000L // 30 seconds
    }
}