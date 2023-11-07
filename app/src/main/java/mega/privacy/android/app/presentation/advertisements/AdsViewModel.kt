package mega.privacy.android.app.presentation.advertisements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.app.presentation.advertisements.model.AdsSlotIDs
import mega.privacy.android.app.presentation.advertisements.model.AdsUIState
import mega.privacy.android.domain.entity.advertisements.FetchAdDetailRequest
import mega.privacy.android.domain.entity.preference.StartScreen
import mega.privacy.android.domain.usecase.MonitorStartScreenPreference
import mega.privacy.android.domain.usecase.advertisements.FetchAdDetailUseCase
import mega.privacy.android.domain.usecase.advertisements.IsAccountNewUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * View model of [AdsBannerView]
 */
@HiltViewModel
class AdsViewModel @Inject constructor(
    private val fetchAdDetailUseCase: FetchAdDetailUseCase,
    private val isAccountNewUseCase: IsAccountNewUseCase,
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
    private val monitorStartScreenPreference: MonitorStartScreenPreference,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdsUIState())
    private var isPortrait = true

    /**
     * Ads state
     */
    val uiState = _uiState.asStateFlow()

    /**
     * Feature Flag for InAppAdvertisement
     */
    private var isAdsFeatureEnabled: Boolean = false
    private var isAccountNew: Boolean = false
    private var fetchAdUrlJob: Job? = null

    init {
        checkForInApAdvertisementFeature()
    }

    /**
     * Checks before fetching new Ad if account is not new
     */
    fun onAdDismissed() {
        _uiState.update { state ->
            state.copy(showAdsView = false)
        }
        if (isAccountNew) return
        val currentSlotId = _uiState.value.slotId
        fetchNewAd(currentSlotId)
    }

    /**
     * Update the ads visibility when configuration changes and cancels any ongoing fetching ad's job
     */
    fun onScreenOrientationChanged(isPortrait: Boolean) {
        this.isPortrait = isPortrait
        if (isPortrait.not())
            cancelFetchingAds()
        _uiState.update { state ->
            state.copy(showAdsView = isPortrait && isAdsFeatureEnabled)
        }
    }


    /**
     * Check for the status of the account if new or not
     */
    private suspend fun checkIsNewAccount() {
        runCatching {
            isAccountNew = isAccountNewUseCase()
        }.onFailure {
            Timber.e("Failed to fetch isNewAccount with error: ${it.message}")
        }
    }

    private fun checkForInApAdvertisementFeature() {
        viewModelScope.launch {
            runCatching {
                isAdsFeatureEnabled = getFeatureFlagValueUseCase(AppFeatures.InAppAdvertisement)
                if (isAdsFeatureEnabled) {
                    getDefaultStartScreen()
                    checkIsNewAccount()
                }
            }.onFailure {
                Timber.e("Failed to fetch feature flag with error: ${it.message}")
            }
        }
    }

    /**
     * gets the default start screen and fetch the Ad based on the assigned slot
     */
    private suspend fun getDefaultStartScreen() {
        when (monitorStartScreenPreference().firstOrNull()) {
            StartScreen.CloudDrive -> {
                fetchNewAd(AdsSlotIDs.TAB_CLOUD_SLOT_ID)
            }

            StartScreen.Photos -> {
                fetchNewAd(AdsSlotIDs.TAB_PHOTOS_SLOT_ID)
            }

            StartScreen.Home -> {
                fetchNewAd(AdsSlotIDs.TAB_HOME_SLOT_ID)
            }

            else -> {}
        }
    }

    /**
     * Cancel fetching any ongoing ads job
     */
    fun cancelFetchingAds() {
        fetchAdUrlJob?.cancel()
    }

    /**
     * Update the url in AdsUIState
     * @param slotId  The ad slot id to fetch ad
     * @param linkHandle  The public handle for file/folder link if user visits Share Link screen, this parameter is optional
     */
    fun fetchNewAd(
        slotId: String,
        linkHandle: Long? = null,
    ) {
        if (isAdsFeatureEnabled) {
            val fetchAdDetailRequest = FetchAdDetailRequest(slotId, linkHandle)
            fetchAdUrlJob?.cancel()
            fetchAdUrlJob = viewModelScope.launch {
                runCatching {
                    val url = fetchAdDetailUseCase(fetchAdDetailRequest)?.url
                    _uiState.update { state ->
                        state.copy(
                            showAdsView = url != null && isPortrait,
                            slotId = slotId,
                            adsBannerUrl = url.orEmpty()
                        )
                    }
                }.onFailure { e ->
                    Timber.e(e)
                }
            }
        } else {
            _uiState.update { state ->
                state.copy(showAdsView = false, slotId = slotId, adsBannerUrl = "")
            }
        }
    }
}