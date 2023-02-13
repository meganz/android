package mega.privacy.android.app.myAccount.usecase

import io.reactivex.rxjava3.core.Single
import mega.privacy.android.app.R
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import mega.privacy.android.app.utils.StringResourcesUtils.getString
import mega.privacy.android.data.qualifier.MegaApi
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaError.API_EEXIST
import nz.mega.sdk.MegaError.API_ENOENT
import nz.mega.sdk.MegaError.API_OK
import javax.inject.Inject

class ConfirmChangeEmailUseCase @Inject constructor(
    @MegaApi private val megaApi: MegaApiAndroid
) {

    /**
     * Launches a request to confirm an email change.
     *
     * @param link     The email change link.
     * @param password The password of the account to change its email.
     * @return The old email if success, the corresponding error if not.
     */
    fun confirm(link: String, password: String): Single<String> =
        Single.create { emitter ->
            megaApi.confirmChangeEmail(
                link,
                password,
                OptionalMegaRequestListenerInterface(onRequestFinish = { request, error ->
                    emitter.onSuccess(
                        when (error.errorCode) {
                            API_OK -> request.email
                            API_EEXIST -> getString(R.string.mail_already_used)
                            API_ENOENT -> getString(R.string.old_password_provided_incorrect)
                            else -> getString(R.string.general_text_error)
                        }
                    )
                })
            )
        }
}