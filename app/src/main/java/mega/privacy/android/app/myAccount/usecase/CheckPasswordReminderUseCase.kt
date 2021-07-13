package mega.privacy.android.app.myAccount.usecase

import io.reactivex.rxjava3.core.Single
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import mega.privacy.android.app.utils.ErrorUtils.toThrowable
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaError.API_ENOENT
import nz.mega.sdk.MegaError.API_OK
import javax.inject.Inject

class CheckPasswordReminderUseCase @Inject constructor(
    @MegaApi private val megaApi: MegaApiAndroid
) {

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