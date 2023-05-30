package mega.privacy.android.app.fragments.settingsFragments.cookie

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.arch.BaseRxViewModel
import mega.privacy.android.app.fragments.settingsFragments.cookie.data.CookieType
import mega.privacy.android.app.fragments.settingsFragments.cookie.usecase.GetCookieSettingsUseCase
import mega.privacy.android.app.fragments.settingsFragments.cookie.usecase.UpdateCookieSettingsUseCase
import mega.privacy.android.app.utils.notifyObserver
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class CookieSettingsViewModel @Inject constructor(
    private val getCookieSettingsUseCase: GetCookieSettingsUseCase,
    private val updateCookieSettingsUseCase: UpdateCookieSettingsUseCase,
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
     * Save cookie settings to SDK
     */
    fun saveCookieSettings() {
        updateCookieSettingsUseCase.update(enabledCookies.value)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onComplete = {
                    updateResult.value = true

                    MegaApplication.getInstance().checkEnabledCookies()
                },
                onError = { error ->
                    Timber.e(error)
                    updateResult.value = false
                    getCookieSettings()
                }
            ).addTo(composite)
    }

    /**
     * Retrieve current cookie settings from SDK
     */
    private fun getCookieSettings() {
        getCookieSettingsUseCase.get()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onSuccess = { settings ->
                    if (!settings.isNullOrEmpty()) {
                        enabledCookies.value = settings.toMutableSet()
                        savedCookiesSize.value = settings.size
                    }

                    updateResult.value = true
                },
                onError = { error ->
                    Timber.e(error)
                    updateResult.value = false
                    resetCookies()
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
