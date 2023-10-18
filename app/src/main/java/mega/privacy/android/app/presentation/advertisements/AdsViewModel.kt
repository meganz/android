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
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * View model of [AdsBannerView]
 */
@HiltViewModel
class AdsViewModel @Inject constructor(
    private val fetchAdDetailUseCase: FetchAdDetailUseCase,
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
    private val monitorStartScreenPreference: MonitorStartScreenPreference,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdsUIState())

    /**
     * Ads state
     */
    val uiState = _uiState.asStateFlow()

    /**
     * Feature Flag for InAppAdvertisement
     */
    private var isAdsFeatureEnabled: Boolean = false
    private var fetchAdUrlJob: Job? = null

    init {
        getDefaultStartScreen()
    }

    /**
     * gets the default start screen and fetch the Ad based on the assigned slot
     */
    private fun getDefaultStartScreen() {
        viewModelScope.launch {
            isAdsFeatureEnabled = getFeatureFlagValueUseCase(AppFeatures.InAppAdvertisement)
            if (isAdsFeatureEnabled) {
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
                            showAdsView = url != null,
                            adsBannerUrl = url.orEmpty()
                        )
                    }
                }.onFailure { e ->
                    Timber.e(e)
                }
            }
        } else {
            _uiState.update { state ->
                state.copy(showAdsView = false, adsBannerUrl = "")
            }
        }
    }
}