package mega.privacy.android.app.myAccount.usecase

import io.reactivex.rxjava3.core.Single
import mega.privacy.android.data.qualifier.MegaApi
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import mega.privacy.android.app.utils.ErrorUtils.toThrowable
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaError.API_ENOENT
import nz.mega.sdk.MegaError.API_OK
import javax.inject.Inject

class CheckPasswordReminderUseCase @Inject constructor(
    @MegaApi private val megaApi: MegaApiAndroid
) {

    /**
     * Launches a request to check if should show Password reminder.
     *
     * @param atLogout True if the request is launched before logout action, false otherwise.
     * @return Single<Boolean> True/false if the request finished with success, error if not.
     */
    fun check(atLogout: Boolean): Single<Boolean> =
        Single.create { emitter ->
            megaApi.shouldShowPasswordReminderDialog(
                atLogout,
                OptionalMegaRequestListenerInterface(onRequestFinish = { request, error ->
                    when (error.errorCode) {
                        API_OK, API_ENOENT -> emitter.onSuccess(request.flag)
                        else -> emitter.onError(error.toThrowable())
                    }
                })
            )
        }
}