package mega.privacy.android.app.myAccount.usecase

import android.content.Intent
import io.reactivex.rxjava3.core.Completable
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.data.qualifier.MegaApi
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import mega.privacy.android.app.utils.Constants.BROADCAST_ACTION_INTENT_UPDATE_USER_DATA
import mega.privacy.android.app.utils.ErrorUtils.toThrowable
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaError.API_OK
import javax.inject.Inject

class GetUserDataUseCase @Inject constructor(
    @MegaApi private val megaApi: MegaApiAndroid
) {

    /**
     * Launches a request to get the current account user data.
     * Launches a broadcast to update user data if finishes with success.
     *
     * @return Completable onComplete() if the request finished with success, error if not.
     */
    fun get(): Completable =
        Completable.create { emitter ->
            megaApi.getUserData(
                OptionalMegaRequestListenerInterface(onRequestFinish = { _, error ->
                    when (error.errorCode) {
                        API_OK -> {
                            MegaApplication.getInstance()
                                .sendBroadcast(Intent(BROADCAST_ACTION_INTENT_UPDATE_USER_DATA))

                            emitter.onComplete()
                        }
                        else -> emitter.onError(error.toThrowable())
                    }
                })
            )
        }
}