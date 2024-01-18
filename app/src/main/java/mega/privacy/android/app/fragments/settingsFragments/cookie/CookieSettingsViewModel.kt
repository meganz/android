package mega.privacy.android.app.fragments.settingsFragments.cookie

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.arch.BaseRxViewModel
import mega.privacy.android.app.featuretoggle.ABTestFeatures
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.app.fragments.settingsFragments.cookie.model.CookieSettingsUIState
import mega.privacy.android.app.utils.notifyObserver
import mega.privacy.android.domain.entity.settings.cookie.CookieType
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.setting.BroadcastCookieSettingsSavedUseCase
import mega.privacy.android.domain.usecase.setting.GetCookieSettingsUseCase
import mega.privacy.android.domain.usecase.setting.UpdateCookieSettingsUseCase
import mega.privacy.android.domain.usecase.setting.UpdateCrashAndPerformanceReportersUseCase
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class CookieSettingsViewModel @Inject constructor(
    private val getCookieSettingsUseCase: GetCookieSettingsUseCase,
    private val updateCookieSettingsUseCase: UpdateCookieSettingsUseCase,
    private val broadcastCookieSettingsSavedUseCase: BroadcastCookieSettingsSavedUseCase,
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
    private val updateCrashAndPerformanceReportersUseCase: UpdateCrashAndPerformanceReportersUseCase,
) : BaseRxViewModel() {

    private val _uiState = MutableStateFlow(CookieSettingsUIState())

    /**
     * CookieSettings state
     */
    val uiState = _uiState.asStateFlow()

    private val enabledCookies = MutableLiveData(mutableSetOf(CookieType.ESSENTIAL))
    private val updateResult = MutableLiveData<Boolean>()
    private val savedCookiesSize = MutableLiveData(1)

    private val enableBackPressedHandler = MediatorLiveData<Boolean>().also { mediator ->
        mediator.addSource(enabledCookies) { enabledCookies ->
            mediator.value = enabledCookies.size != savedCookiesSize.value
        }
        mediator.addSource(savedCookiesSize) { savedCookiesSize ->
            mediator.value = savedCookiesSize != enabledCookies.value?.size
        }
    }

    init {
        loadCookieSettings()
        checkForInAppAdvertisement()
    }

    fun onEnabledCookies(): LiveData<MutableSet<CookieType>> = enabledCookies
    fun onUpdateResult(): LiveData<Boolean> = updateResult

    /**
     * On enable back pressed handler
     *
     * @return livedata to enable/disable back pressed handler
     */
    fun onEnableBackPressedHandler(): LiveData<Boolean> = enableBackPressedHandler

    /**
     * Change cookie state
     *
     * @param cookie Cookie type to change
     * @param enable Flag to enable/disable cookie
     */
    fun changeCookie(cookie: CookieType, enable: Boolean) {
        if (enable) {
            enabledCookies.value?.add(cookie)
        } else {
            enabledCookies.value?.remove(cookie)
        }
        enabledCookies.notifyObserver()
    }

    /**
     * Change all cookies state at once
     *
     * @param enable Flag to enable/disable all cookies
     */
    fun toggleCookies(enable: Boolean) {
        if (enable) {
            enabledCookies.value?.addAll(CookieType.entries.toTypedArray())
            enabledCookies.notifyObserver()
        } else {
            resetCookies()
        }
    }


    /**
     * Check if showAdsCookiePreference is enabled
     */
    private fun checkForInAppAdvertisement() {
        viewModelScope.launch {
            runCatching {
                val showAdsCookiePreference =
                    getFeatureFlagValueUseCase(AppFeatures.InAppAdvertisement) &&
                            getFeatureFlagValueUseCase(ABTestFeatures.ads) &&
                            getFeatureFlagValueUseCase(ABTestFeatures.adse)
                _uiState.update { state ->
                    state.copy(showAdsCookiePreference = showAdsCookiePreference)
                }

            }.onFailure {
                Timber.e("Failed to fetch feature flag with error: ${it.message}")
            }
        }
    }

    /**
     * Add ads check cookie if cookies get changed
     */
    private fun addAdsCheckCookieIfNeeded() {
        if (enabledCookies.value?.size != savedCookiesSize.value) {
            enabledCookies.value?.add(CookieType.ADS_CHECK)
        }
    }

    /**
     * Save cookie settings to SDK
     */
    fun saveCookieSettings() {
        if (uiState.value.showAdsCookiePreference) {
            addAdsCheckCookieIfNeeded()
        }
        viewModelScope.launch {
            enabledCookies.value?.let {
                runCatching {
                    updateCookieSettingsUseCase(it.toSet())
                    broadcastCookieSettingsSavedUseCase((it.toSet()))
                    updateCrashAndPerformanceReportersUseCase()
                    updateResult.value = true
                }.onFailure {
                    Timber.e(it)
                    updateResult.value = false
                    loadCookieSettings()
                }
            }
        }
    }

    /**
     * Retrieve current cookie settings from SDK
     */
    private fun loadCookieSettings() {
        viewModelScope.launch {
            runCatching {
                val settings = getCookieSettingsUseCase()
                if (settings.isNotEmpty()) {
                    enabledCookies.value = settings.toMutableSet()
                    savedCookiesSize.value = settings.size
                }
                updateResult.value = true
            }.onFailure {
                Timber.e(it)
                updateResult.value = false
                resetCookies()
            }
        }
    }

    /**
     * Reset cookies to essentials ones
     */
    private fun resetCookies() {
        enabledCookies.value = mutableSetOf(CookieType.ESSENTIAL)
    }
}
