package mega.privacy.android.app.fragments.settingsFragments.cookie

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import mega.privacy.android.app.arch.BaseRxViewModel
import mega.privacy.android.app.fragments.settingsFragments.cookie.data.CookieType
import mega.privacy.android.app.fragments.settingsFragments.cookie.usecase.GetCookieSettingsUseCase
import mega.privacy.android.app.fragments.settingsFragments.cookie.usecase.UpdateCookieSettingsUseCase
import mega.privacy.android.app.utils.LogUtil.logError
import mega.privacy.android.app.utils.notifyObserver

class CookieSettingsViewModel @ViewModelInject constructor(
    private val getCookieSettingsUseCase: GetCookieSettingsUseCase,
    private val updateCookieSettingsUseCase: UpdateCookieSettingsUseCase
) : BaseRxViewModel() {

    private val enabledCookies = MutableLiveData<MutableSet<CookieType>>()
    private val updateResult = MutableLiveData<Boolean>()

    fun onEnabledCookies(): LiveData<MutableSet<CookieType>> = enabledCookies
    fun onUpdateResult(): LiveData<Boolean> = updateResult

    init {
        getCookieSettings()
    }

    /**
     * Change specific cookie state
     *
     * @param cookie Cookie to be changed
     * @param enable Flag to enable/disable specified cookie
     */
    fun changeCookie(cookie: CookieType, enable: Boolean) {
        if (enable) {
            enabledCookies.value?.add(cookie)
        } else {
            enabledCookies.value?.remove(cookie)
        }

        enabledCookies.notifyObserver()
        updateCookieSettings()
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

        updateCookieSettings()
    }

    /**
     * Retrieve current cookie settings from SDK
     */
    private fun getCookieSettings() {
        getCookieSettingsUseCase.get()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onSuccess = { configuration ->
                    enabledCookies.postValue(configuration.toMutableSet())
                },
                onError = { error ->
                    logError(error.stackTraceToString())
                    resetCookies()
                }
            )
            .addTo(composite)
    }

    /**
     * Save cookie settings to SDK
     */
    private fun updateCookieSettings() {
        updateCookieSettingsUseCase.update(enabledCookies.value)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onComplete = {
                    updateResult.postValue(true)
                },
                onError = { error ->
                    logError(error.stackTraceToString())
                    updateResult.postValue(false)
                }
            )
            .addTo(composite)
    }

    /**
     * Reset cookies to essentials ones
     */
    private fun resetCookies() {
        enabledCookies.value = mutableSetOf(CookieType.ESSENTIAL)
    }
}
