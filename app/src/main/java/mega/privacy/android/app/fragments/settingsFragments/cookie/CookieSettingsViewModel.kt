package mega.privacy.android.app.fragments.settingsFragments.cookie

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import mega.privacy.android.app.arch.BaseRxViewModel
import mega.privacy.android.app.utils.notifyObserver
import mega.privacy.android.domain.entity.settings.cookie.CookieType
import mega.privacy.android.domain.usecase.setting.GetCookieSettingsUseCase
import mega.privacy.android.domain.usecase.setting.UpdateCookieSettingsUseCase
import mega.privacy.android.domain.usecase.setting.UpdateCrashAndPerformanceReportersUseCase
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class CookieSettingsViewModel @Inject constructor(
    private val getCookieSettingsUseCase: GetCookieSettingsUseCase,
    private val updateCookieSettingsUseCase: UpdateCookieSettingsUseCase,
    private val updateCrashAndPerformanceReportersUseCase: UpdateCrashAndPerformanceReportersUseCase,
) : BaseRxViewModel() {

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

    fun onEnabledCookies(): LiveData<MutableSet<CookieType>> = enabledCookies
    fun onUpdateResult(): LiveData<Boolean> = updateResult

    /**
     * On enable back pressed handler
     *
     * @return livedata to enable/disable back pressed handler
     */
    fun onEnableBackPressedHandler(): LiveData<Boolean> = enableBackPressedHandler

    init {
        getCookieSettings()
    }

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
            enabledCookies.value?.addAll(CookieType.values())
            enabledCookies.notifyObserver()
        } else {
            resetCookies()
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
        addAdsCheckCookieIfNeeded()
        viewModelScope.launch {
            enabledCookies.value?.let {
                runCatching {
                    updateCookieSettingsUseCase(it.toSet())
                    updateCrashAndPerformanceReportersUseCase()
                    updateResult.value = true
                }.onFailure {
                    Timber.e(it)
                    updateResult.value = false
                    getCookieSettings()
                }
            }
        }
    }

    /**
     * Retrieve current cookie settings from SDK
     */
    private fun getCookieSettings() {
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
