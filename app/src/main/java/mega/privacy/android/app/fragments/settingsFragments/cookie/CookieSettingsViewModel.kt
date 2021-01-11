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
import mega.privacy.android.app.fragments.settingsFragments.cookie.usecase.GetCookieSettingsUsecase
import mega.privacy.android.app.fragments.settingsFragments.cookie.usecase.UpdateCookieSettingsUsecase
import mega.privacy.android.app.utils.LogUtil.logDebug
import mega.privacy.android.app.utils.notifyObserver

class CookieSettingsViewModel @ViewModelInject constructor(
    private val getCookieSettingsUsecase: GetCookieSettingsUsecase,
    private val updateCookieSettingsUsecase: UpdateCookieSettingsUsecase
) : BaseRxViewModel() {

    companion object {
        private const val TAG = "CookieSettingsViewModel"
    }

    private val enabledCookies = MutableLiveData<MutableSet<CookieType>>()
    private val updateResult = MutableLiveData<Boolean>()

    fun onEnabledCookies(): LiveData<MutableSet<CookieType>> = enabledCookies
    fun onUpdateResult(): LiveData<Boolean> = updateResult

    init {
        getCookieSettings()
    }

    fun changeCookie(cookie: CookieType, enable: Boolean) {
        if (enable) {
            enabledCookies.value?.add(cookie)
        } else {
            enabledCookies.value?.remove(cookie)
        }

        enabledCookies.notifyObserver()
        updateCookieSettings()
    }

    fun toggleCookies(enable: Boolean) {
        if (enable) {
            enabledCookies.value?.addAll(CookieType.values())
            enabledCookies.notifyObserver()
        } else {
            resetCookies()
        }

        updateCookieSettings()
    }

    private fun getCookieSettings() {
        getCookieSettingsUsecase.run()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onSuccess = { configuration ->
                    enabledCookies.postValue(configuration.toMutableSet())
                },
                onError = { error ->
                    resetCookies()
                    logDebug(error.stackTraceToString())
                }
            )
            .addTo(composite)
    }

    private fun updateCookieSettings() {
        updateCookieSettingsUsecase.run(enabledCookies.value)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onComplete = {
                    updateResult.postValue(true)
                },
                onError = { error ->
                    logDebug(error.stackTraceToString())
                    updateResult.postValue(false)
                }
            )
            .addTo(composite)
    }

    private fun resetCookies() {
        enabledCookies.postValue(mutableSetOf(CookieType.ESSENTIAL))
    }
}
