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
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.domain.usecase.account.MonitorUpdateUserDataUseCase
import mega.privacy.android.domain.usecase.advertisements.MonitorGoogleConsentLoadedUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * View model of [mega.privacy.android.app.main.ads.NewAdsContainer]
 */
@HiltViewModel
class AdsViewModel @Inject constructor(
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
    private val consentInformation: ConsentInformation,
    private val monitorGoogleConsentLoadedUseCase: MonitorGoogleConsentLoadedUseCase,
    private val monitorUpdateUserDataUseCase: MonitorUpdateUserDataUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AdsUiState())

    /**
     * Flow to provide the AdRequest to be used in the AdManager.
     */
    val uiState = _uiState.asStateFlow()

    private var refreshAdsJob: Job? = null
    private var lastFetchTime = -1L
    private val mutex = Mutex()

    init {
        viewModelScope.launch {
            monitorGoogleConsentLoadedUseCase().collect { isLoaded ->
                if (isLoaded) {
                    Timber.d("User consent is loaded")
                    scheduleRefreshAds()
                }
            }
        }
        viewModelScope.launch {
            monitorUpdateUserDataUseCase()
                .drop(1) // drop the initial value when app starts
                .collectLatest {
                    Timber.d("Account updated, resetting ads feature flag")
                    _uiState.update { it.copy(isAdsFeatureEnabled = null) }
                    cancelRefreshAds()
                    scheduleRefreshAds()
                }
        }
    }

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
            } else {
                _uiState.update { it.copy(request = null) }
            }
        }
    }

    fun cancelRefreshAds() {
        refreshAdsJob?.cancel()
    }

    private fun createNewAdRequestIfNeeded() {
        Timber.d("Checking if a new AdRequest is needed")
        if (_uiState.value.request == null || System.currentTimeMillis() - lastFetchTime > MINIMUM_AD_REFRESH_INTERVAL) {
            Timber.d("Creating new AdRequest")
            _uiState.update { it.copy(request = AdManagerAdRequest.Builder().build()) }
            lastFetchTime = System.currentTimeMillis()
        }
    }

    private suspend fun isAdsEnabled(): Boolean = mutex.withLock {
        if (_uiState.value.isAdsFeatureEnabled == null) {
            checkForAdsAvailability()
        }
        return _uiState.value.isAdsFeatureEnabled ?: false
    }

    /**
     * Check if the ads feature is enabled.
     */
    private suspend fun checkForAdsAvailability() {
        runCatching {
            _uiState.update {
                it.copy(isAdsFeatureEnabled = getFeatureFlagValueUseCase(ApiFeatures.GoogleAdsFeatureFlag))
            }
            Timber.d("Ads feature enabled: ${_uiState.value.isAdsFeatureEnabled}")
        }.onFailure { e ->
            _uiState.update {
                it.copy(isAdsFeatureEnabled = false)
            }
            Timber.e(e, "Error getting feature flag value")
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