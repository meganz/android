package mega.privacy.android.app.fragments.settingsFragments.cookie.usecase

import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.Disposable
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.fragments.settingsFragments.cookie.data.CookieType
import mega.privacy.android.app.utils.ErrorUtils.toThrowable
import mega.privacy.android.app.utils.LogUtil.logError
import nz.mega.sdk.*
import java.util.*
import javax.inject.Inject

class GetCookieSettingsUseCase @Inject constructor(
    @MegaApi private val megaApi: MegaApiAndroid
) {

    /**
     * Get current cookie settings from SDK
     *
     * @return Observable with a set of enabled cookies
     */
    fun get(): Single<Set<CookieType>> =
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
                    when (error.errorCode) {
                        MegaError.API_OK -> {
                            val result = mutableSetOf<CookieType>()

                            val bitSet = BitSet.valueOf(longArrayOf(request.numDetails.toLong()))
                            for (i in 0..bitSet.length()) {
                                if (bitSet[i]) {
                                    result.add(CookieType.valueOf(i))
                                }
                            }

                            emitter.onSuccess(result)
                        }
                        MegaError.API_ENOENT -> {
                            emitter.onSuccess(emptySet()) // Cookie Settings has not been set before
                        }
                        else -> {
                            emitter.onError(error.toThrowable())
                        }
                    }
                }

                override fun onRequestTemporaryError(
                    api: MegaApiJava,
                    request: MegaRequest,
                    error: MegaError
                ) {
                    logError(error.toThrowable().stackTraceToString())
                }
            }

            megaApi.getCookieSettings(listener)

            emitter.setDisposable(Disposable.fromAction {
                megaApi.removeRequestListener(listener)
            })
        }

    /**
     * Check if the cookie dialog should be shown
     *
     * @return Observable with the boolean flag
     */
    fun shouldShowDialog(): Single<Boolean> =
        get().map { it.isNullOrEmpty() }
}
