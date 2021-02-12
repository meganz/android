package mega.privacy.android.app.fragments.settingsFragments.cookie.usecase

import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.Disposable
import mega.privacy.android.app.utils.ErrorUtils.toThrowable
import mega.privacy.android.app.utils.LogUtil.logError
import nz.mega.sdk.*
import javax.inject.Inject

/**
 * Use Case to check if Cookie Banner is enabled on SDK
 */
class CheckCookieBannerEnabledUseCase @Inject constructor(
    private val megaApi: MegaApiAndroid
) {

    /**
     * Check if the app can start showing the cookie banner
     */
    fun check(): Single<Boolean> =
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
                        MegaError.API_OK, MegaError.API_EACCESS -> emitter.onSuccess(api.isCookieBannerEnabled)
                        else -> emitter.onError(error.toThrowable())
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

            megaApi.getMiscFlags(listener)

            emitter.setDisposable(Disposable.fromAction {
                megaApi.removeRequestListener(listener)
            })
        }
}
