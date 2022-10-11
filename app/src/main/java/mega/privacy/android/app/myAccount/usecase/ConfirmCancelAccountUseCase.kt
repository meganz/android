package mega.privacy.android.app.myAccount.usecase

import io.reactivex.rxjava3.core.Completable
import mega.privacy.android.app.R
import mega.privacy.android.data.qualifier.MegaApi
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import mega.privacy.android.app.utils.StringResourcesUtils.getString
import mega.privacy.android.app.utils.StringUtils.toThrowable
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaError.*
import javax.inject.Inject

class ConfirmCancelAccountUseCase @Inject constructor(
    @MegaApi private val megaApi: MegaApiAndroid
) {

    /**
     * Launches a request to confirm an account cancellation.
     *
     * @param link     The account cancellation link.
     * @param password The password of the account to cancel.
     * @return onComplete if success, the corresponding error if not.
     */
    fun confirm(link: String, password: String): Completable =
        Completable.create { emitter ->
            megaApi.confirmCancelAccount(
                link,
                password,
                OptionalMegaRequestListenerInterface(onRequestFinish = { _, error ->
                    when (error.errorCode) {
                        API_OK -> emitter.onComplete()
                        API_ENOENT ->
                            emitter.onError(getString(R.string.old_password_provided_incorrect).toThrowable())
                        else ->
                            emitter.onError(getString(R.string.general_text_error).toThrowable())
                    }
                })
            )
        }
}