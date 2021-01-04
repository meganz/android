package mega.privacy.android.app.fragments.settingsFragments

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import mega.privacy.android.app.arch.BaseRxViewModel
import mega.privacy.android.app.utils.LogUtil.logDebug
import mega.privacy.android.app.utils.notifyObserver
import nz.mega.sdk.*
import java.util.*

class CookieSettingsViewModel @ViewModelInject constructor(
    private val megaApi: MegaApiAndroid
) : BaseRxViewModel() {

    companion object {
        private const val TAG = "CookieSettingsViewModel"
    }

    private val enabledCookies = MutableLiveData<MutableSet<Cookie>>()

    fun getEnabledCookies(): LiveData<MutableSet<Cookie>> = enabledCookies

    init {
        add(getCookieSettingsObservable()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { configuration ->
                    enabledCookies.postValue(configuration.toMutableSet())
                },
                { error ->
                    logDebug(error.stackTraceToString())
                }
            ))
    }

    fun changeCookie(cookie: Cookie, enable: Boolean) {
        if (enable) {
            enabledCookies.value?.add(cookie)
        } else {
            enabledCookies.value?.remove(cookie)
        }

        enabledCookies.notifyObserver()
        updateCookies()
    }

    fun toggleCookies(enable: Boolean) {
        if (enable) {
            enabledCookies.value?.addAll(Cookie.values())
        } else {
            enabledCookies.value?.clear()
            enabledCookies.value?.add(Cookie.ESSENTIAL)
        }

        enabledCookies.notifyObserver()
        updateCookies()
    }

    private fun getCookieSettingsObservable(): Single<Set<Cookie>> =
        Single.create { emitter ->
            val listener = object : MegaRequestListenerInterface {
                override fun onRequestStart(api: MegaApiJava, request: MegaRequest) {
                    if (emitter.isDisposed) {
                        megaApi.removeRequestListener(this)
                    }
                }

                override fun onRequestUpdate(api: MegaApiJava, request: MegaRequest) {
                    if (emitter.isDisposed) {
                        megaApi.removeRequestListener(this)
                    }
                }

                override fun onRequestFinish(
                    api: MegaApiJava,
                    request: MegaRequest,
                    error: MegaError
                ) {
                    if (error.errorCode == MegaError.API_OK) {
                        val result = mutableSetOf<Cookie>()

                        val bitSet = BitSet.valueOf(longArrayOf(request.numDetails.toLong()))
                        for (i in 0..bitSet.length()) {
                            if (bitSet[i]) {
                                result.add(Cookie.valueOf(i))
                            }
                        }

                        emitter.onSuccess(result)
                    } else {
                        emitter.onError(IllegalStateException("$TAG: ${error.errorString}"))
                    }
                }

                override fun onRequestTemporaryError(
                    api: MegaApiJava,
                    request: MegaRequest,
                    error: MegaError
                ) {
                    megaApi.removeRequestListener(this)
                    emitter.onError(IllegalStateException("$TAG: ${error.errorString}"))
                }
            }

            megaApi.getCookieSettings(listener)

            emitter.setDisposable(Disposable.fromAction {
                megaApi.removeRequestListener(listener)
            })
        }

    private fun updateCookies() {
        add(Completable.fromAction {
            val bitSet = BitSet(Cookie.values().size).apply {
                this[0] = true // Essential cookies are always enabled
            }

            enabledCookies.value?.forEach { setting ->
                bitSet[setting.value] = true
            }

            val bitSetToDecimal = bitSet.toLongArray().first().toInt()
            megaApi.setCookieSettings(bitSetToDecimal)
        }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { },
                { error ->
                    logDebug(error.stackTraceToString())
                }
            ))
    }

    enum class Cookie(val value: Int) {
        ESSENTIAL(0), PREFERENCE(1), ANALYTICS(2), ADVERTISEMENT(3), THIRDPARTY(4);

        companion object {
            fun valueOf(type: Int): Cookie =
                values().firstOrNull { it.value == type } ?: ESSENTIAL
        }
    }
}
