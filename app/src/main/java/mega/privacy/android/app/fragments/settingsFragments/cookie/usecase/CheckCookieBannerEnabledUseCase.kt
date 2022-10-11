package mega.privacy.android.app.fragments.settingsFragments.cookie.usecase

import io.reactivex.rxjava3.core.Single
import mega.privacy.android.data.qualifier.MegaApi
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import mega.privacy.android.app.utils.ErrorUtils.toThrowable
import nz.mega.sdk.*
import javax.inject.Inject

/**
 * Use Case to check if Cookie Banner is enabled on SDK
 */
class CheckCookieBannerEnabledUseCase @Inject constructor(
    @MegaApi private val megaApi: MegaApiAndroid
) {

    /**
     * Check if the app can start showing the cookie banner
     */
    fun check(): Single<Boolean> =
        Single.create { emitter ->
            megaApi.getMiscFlags(OptionalMegaRequestListenerInterface(
                onRequestFinish = { _, error ->
                    if (emitter.isDisposed) return@OptionalMegaRequestListenerInterface

                    when (error.errorCode) {
                        MegaError.API_OK, MegaError.API_EACCESS ->
                            emitter.onSuccess(megaApi.isCookieBannerEnabled)
                        else ->
                            emitter.onError(error.toThrowable())
                    }
                }
            ))
        }
}
