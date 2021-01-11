package mega.privacy.android.app.fragments.settingsFragments.cookie.usecase

import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.disposables.Disposable
import mega.privacy.android.app.fragments.settingsFragments.cookie.data.CookieType
import nz.mega.sdk.*
import java.util.*
import javax.inject.Inject

class UpdateCookieSettingsUsecase @Inject constructor(
    private val megaApi: MegaApiAndroid
) {

    companion object {
        private const val TAG = "UpdateCookieSettingsUsecase"
    }

    fun run(cookies: Set<CookieType>?): Completable =
        Completable.create { emitter ->
            if (cookies.isNullOrEmpty()) {
                emitter.onError(IllegalArgumentException("Cookies are null or empty"))
                return@create
            }

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
                        emitter.onComplete()
                    } else {
                        emitter.onError(RuntimeException("$TAG: ${error.errorString}"))
                    }
                }

                override fun onRequestTemporaryError(
                    api: MegaApiJava,
                    request: MegaRequest,
                    error: MegaError
                ) {
                    megaApi.removeRequestListener(this)
                    emitter.onError(RuntimeException("$TAG: ${error.errorString}"))
                }
            }

            val bitSet = BitSet(CookieType.values().size).apply {
                this[0] = true // Essential cookies are always enabled
            }

            cookies.forEach { setting ->
                bitSet[setting.value] = true
            }

            val bitSetToDecimal = bitSet.toLongArray().first().toInt()
            megaApi.setCookieSettings(bitSetToDecimal, listener)

            emitter.setDisposable(Disposable.fromAction {
                megaApi.removeRequestListener(listener)
            })
        }
}
