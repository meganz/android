package mega.privacy.android.app.myAccount.usecase

import io.reactivex.rxjava3.core.Single
import mega.privacy.android.data.qualifier.MegaApi
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import mega.privacy.android.app.utils.ErrorUtils.toThrowable
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaError.API_OK
import javax.inject.Inject

class Check2FAUseCase @Inject constructor(
    @MegaApi private val megaApi: MegaApiAndroid
) {

    /**
     * Launches a request to check if 2FA is enabled or not.
     *
     * @return Single<Boolean> True/false if the request finished with success, error if not.
     */
    fun check(): Single<Boolean> =
        Single.create { emitter ->
            megaApi.multiFactorAuthCheck(
                megaApi.myEmail,
                OptionalMegaRequestListenerInterface(onRequestFinish = { request, error ->
                    if (error.errorCode == API_OK) emitter.onSuccess(request.flag)
                    else emitter.onError(error.toThrowable())
                })
            )
        }
}