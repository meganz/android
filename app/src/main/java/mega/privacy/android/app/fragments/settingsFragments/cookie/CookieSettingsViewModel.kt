package mega.privacy.android.app.fragments.settingsFragments.cookie

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.featuretoggle.ApiFeatures
import mega.privacy.android.app.fragments.settingsFragments.cookie.model.CookieSettingsUIState
import mega.privacy.android.app.utils.notifyObserver
import mega.privacy.android.domain.entity.settings.cookie.CookieType
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.setting.GetCookieSettingsUseCase
import mega.privacy.android.domain.usecase.setting.UpdateCookieSettingsUseCase
import mega.privacy.android.domain.usecase.setting.UpdateCrashAndPerformanceReportersUseCase
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class CookieSettingsViewModel @Inject constructor(
    private val getCookieSettingsUseCase: GetCookieSettingsUseCase,
    private val updateCookieSettingsUseCase: UpdateCookieSettingsUseCase,
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
    private val updateCrashAndPerformanceReportersUseCase: UpdateCrashAndPerformanceReportersUseCase,
    @ApplicationScope private val applicationScope: CoroutineScope,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CookieSettingsUIState())

    /**
     * CookieSettings state
     */
    val uiState = _uiState.asStateFlow()

    private val enabledCookies = MutableLiveData(mutableSetOf(CookieType.ESSENTIAL))
    private val updateResult = MutableStateFlow<Boolean?>(null)
    private val savedCookiesSize = MutableLiveData(1)

    init {
        loadCookieSettings()
        checkForInAppAdvertisement()
    }

    fun onEnabledCookies(): LiveData<MutableSet<CookieType>> = enabledCookies
    fun onUpdateResult(): StateFlow<Boolean?> = updateResult.asStateFlow()

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
        saveCookieSettings()
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
        saveCookieSettings()
    }

    /**
     * Check if showAdsCookiePreference is enabled
     */
    private fun checkForInAppAdvertisement() {
        viewModelScope.launch {
            runCatching {
                val showAdsCookiePreference =
                    getFeatureFlagValueUseCase(ApiFeatures.GoogleAdsFeatureFlag)
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

    private var saveCookieJob: Job? = null

    /**
     * Save cookie settings to SDK
     */
    fun saveCookieSettings() {
        saveCookieJob?.cancel()
        saveCookieJob = applicationScope.launch {
            updateResult.value = null
            if (uiState.value.showAdsCookiePreference) {
                addAdsCheckCookieIfNeeded()
            }
            enabledCookies.value?.let {
                runCatching {
                    updateCookieSettingsUseCase(it.toSet())
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
