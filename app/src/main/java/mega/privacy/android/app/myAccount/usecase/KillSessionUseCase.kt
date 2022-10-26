package mega.privacy.android.app.myAccount.usecase

import io.reactivex.rxjava3.core.Single
import mega.privacy.android.data.qualifier.MegaApi
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import mega.privacy.android.app.utils.ErrorUtils.toThrowable
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError
import javax.inject.Inject

class KillSessionUseCase @Inject constructor(
    @MegaApi private val megaApi: MegaApiAndroid
) {

    /**
     * Launches a request to kill other sessions.
     *
     * @return Single<Boolean> True if the request finished with success, error if not.
     */
    fun kill(): Single<Boolean> =
        Single.create { emitter ->
            megaApi.killSession(MegaApiJava.INVALID_HANDLE, OptionalMegaRequestListenerInterface(
                onRequestFinish = { _, error ->
                    if (error.errorCode == MegaError.API_OK) {
                        emitter.onSuccess(true)
                    } else {
                        emitter.onError(error.toThrowable())
                    }
                }
            ))
        }
}