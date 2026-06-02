package mega.privacy.android.shared.ads.advertisements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.libraries.ads.mobile.sdk.banner.AdSize
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRequest
import com.google.android.ump.ConsentInformation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
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
import mega.privacy.android.shared.ads.BuildConfig
import timber.log.Timber
import javax.inject.Inject

/**
 * View model of [mega.privacy.android.shared.ads.NewAdsContainer]
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

    /**
     * Whether ads are allowed for the current screen. For link screens this is the per-link
     * `QueryAdsUseCase` result; a link created by a Pro user is not eligible, so no request is made.
     * Defaults to `true` for callers with no extra constraint (e.g. the home screen).
     */
    private var isAdsAllowedForScreen = true

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
            if (isAdsAllowedForScreen && isAdsEnabled() && consentInformation.canRequestAds()) {
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

    /**
     * Update whether ads are allowed for the current screen. When set to `false` any pending
     * request is cleared and no new request is made; when set back to `true` refreshing resumes.
     *
     * @param allowed whether ads are allowed for the current screen
     */
    fun setAdsAllowedForScreen(allowed: Boolean) {
        if (isAdsAllowedForScreen == allowed) return
        isAdsAllowedForScreen = allowed
        cancelRefreshAds()
        if (allowed) {
            scheduleRefreshAds()
        } else {
            _uiState.update { it.copy(request = null) }
        }
    }

    private fun createNewAdRequestIfNeeded() {
        Timber.d("Checking if a new AdRequest is needed")
        if (_uiState.value.request == null || System.currentTimeMillis() - lastFetchTime > MINIMUM_AD_REFRESH_INTERVAL) {
            Timber.d("Creating new AdRequest")
            _uiState.update {
                it.copy(
                    request = BannerAdRequest.Builder(
                        BuildConfig.AD_UNIT_ID,
                        AdSize(320, 50)
                    ).build()
                )
            }
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
            getFeatureFlagValueUseCase(ApiFeatures.GoogleAdsFeatureFlag)
        }.onSuccess { isEnabled ->
            _uiState.update { it.copy(isAdsFeatureEnabled = isEnabled) }
            Timber.d("Ads feature enabled: $isEnabled")
        }.onFailure { e ->
            if (e is CancellationException) {
                // Job was cancelled (e.g. screen paused/disposed); leave the flag unresolved so it
                // is re-checked next time instead of being cached as disabled.
                throw e
            }

            _uiState.update { it.copy(isAdsFeatureEnabled = false) }
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