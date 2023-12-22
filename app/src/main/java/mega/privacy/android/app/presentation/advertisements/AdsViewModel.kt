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
import mega.privacy.android.app.presentation.advertisements.model.AdsSlotIDs
import mega.privacy.android.app.presentation.advertisements.model.AdsUIState
import mega.privacy.android.domain.entity.advertisements.FetchAdDetailRequest
import mega.privacy.android.domain.entity.preference.StartScreen
import mega.privacy.android.domain.usecase.MonitorStartScreenPreference
import mega.privacy.android.domain.usecase.advertisements.FetchAdDetailUseCase
import mega.privacy.android.domain.usecase.advertisements.IsAccountNewUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * View model of [AdsBannerView]
 */
@HiltViewModel
class AdsViewModel @Inject constructor(
    private val fetchAdDetailUseCase: FetchAdDetailUseCase,
    private val isAccountNewUseCase: IsAccountNewUseCase,
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


    /**
     * Enable the ads feature
     */
    fun enableAdsFeature() {
        isAdsFeatureEnabled = true
        checkIsNewAccount()
    }

    /**
     * Disable the ads feature
     */
    fun disableAdsFeature() {
        cancelFetchingAds()
        isAdsFeatureEnabled = false
        _uiState.update { state ->
            state.copy(showAdsView = false, slotId = "", adsBannerUrl = "")
        }
    }

    /**
     * Mark the ad slot as consumed
     */
    fun onAdConsumed() {
        val consumedSlots = _uiState.value.consumedAdSlots
        consumedSlots.add(_uiState.value.slotId)
        _uiState.update { state ->
            state.copy(consumedAdSlots = consumedSlots, showAdsView = false)
        }
        if (canConsumeAdSlot(_uiState.value.slotId))
            fetchNewAd()
    }

    /**
     * Checks if the ad slot can be consumed
     * @param adSlot  The ad slot id to check
     * @return true if the ad slot can be consumed, false otherwise
     */
    fun canConsumeAdSlot(adSlot: String): Boolean {
        return if (isAccountNew.not()) {
            true
        } else {
            _uiState.value.consumedAdSlots.contains(adSlot).not()
        }
    }

    /**
     * Update the ads visibility when configuration changes and cancels any ongoing fetching ad's job
     */
    fun onScreenOrientationChanged(isPortrait: Boolean) {
        this.isPortrait = isPortrait
        if (isPortrait.not())
            cancelFetchingAds()
        _uiState.update { state ->
            state.copy(
                showAdsView = isPortrait
                        && canConsumeAdSlot(_uiState.value.slotId)
                        && isAdsFeatureEnabled
            )
        }
    }


    /**
     * Check for the status of the account if new or not
     */
    private fun checkIsNewAccount() {
        viewModelScope.launch {
            runCatching {
                isAccountNew = isAccountNewUseCase()
            }.onFailure {
                Timber.e("Failed to fetch isNewAccount with error: ${it.message}")
            }
        }
    }

    /**
     * gets the default start screen and fetch the Ad based on the assigned slot
     */
    fun getDefaultStartScreen() {
        viewModelScope.launch {
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
        slotId: String = uiState.value.slotId,
        linkHandle: Long? = null,
    ) {
        if (isAdsFeatureEnabled && canConsumeAdSlot(slotId)) {
            val fetchAdDetailRequest = FetchAdDetailRequest(slotId, linkHandle)
            fetchAdUrlJob?.cancel()
            fetchAdUrlJob = viewModelScope.launch {
                runCatching {
                    val url = fetchAdDetailUseCase(fetchAdDetailRequest)?.url
                    _uiState.update { state ->
                        state.copy(
                            showAdsView = url != null && isPortrait && canConsumeAdSlot(slotId),
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