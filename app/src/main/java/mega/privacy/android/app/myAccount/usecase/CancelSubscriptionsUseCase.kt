package mega.privacy.android.app.myAccount.usecase

import io.reactivex.rxjava3.core.Single
import mega.privacy.android.data.qualifier.MegaApi
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import mega.privacy.android.app.utils.ErrorUtils.toThrowable
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaError
import javax.inject.Inject

class CancelSubscriptionsUseCase @Inject constructor(
    @MegaApi private val megaApi: MegaApiAndroid
) {

    /**
     * Launches a request to cancel card subscriptions.
     *
     * @param feedback Message typed as reason to cancel subscriptions.
     * @return Single<Boolean> True if the request finished with success, error if not.
     */
    fun cancel(feedback: String?): Single<Boolean> =
        Single.create { emitter ->
            megaApi.creditCardCancelSubscriptions(feedback, OptionalMegaRequestListenerInterface(
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