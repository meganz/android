package mega.privacy.android.app.fragments.settingsFragments.cookie.usecase

import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.Disposable
import mega.privacy.android.app.fragments.settingsFragments.cookie.data.CookieType
import nz.mega.sdk.*
import java.util.*
import javax.inject.Inject

class GetCookieSettingsUsecase @Inject constructor(
    private val megaApi: MegaApiAndroid
) {

    fun run(): Single<Set<CookieType>> =
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
                        val result = mutableSetOf<CookieType>()

                        val bitSet = BitSet.valueOf(longArrayOf(request.numDetails.toLong()))
                        for (i in 0..bitSet.length()) {
                            if (bitSet[i]) {
                                result.add(CookieType.valueOf(i))
                            }
                        }

                        emitter.onSuccess(result)
                    } else {
                        emitter.onError(RuntimeException("${error.errorCode}: ${error.errorString}"))
                    }
                }

                override fun onRequestTemporaryError(
                    api: MegaApiJava,
                    request: MegaRequest,
                    error: MegaError
                ) {
                    megaApi.removeRequestListener(this)
                    emitter.onError(RuntimeException("${error.errorCode}: ${error.errorString}"))
                }
            }

            megaApi.getCookieSettings(listener)

            emitter.setDisposable(Disposable.fromAction {
                megaApi.removeRequestListener(listener)
            })
        }

    fun shouldShowDialog(): Single<Boolean> =
        run().map { it.isNullOrEmpty() }
            .onErrorReturn { true }
}
