package mega.privacy.android.app.myAccount.usecase

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.rxjava3.core.Single
import mega.privacy.android.app.R
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import mega.privacy.android.data.qualifier.MegaApi
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaError.API_EEXIST
import nz.mega.sdk.MegaError.API_ENOENT
import nz.mega.sdk.MegaError.API_OK
import javax.inject.Inject

/**
 * Confirm change email use case
 *
 * @property megaApi
 * @property context
 * @constructor Create empty Confirm change email use case
 */
class ConfirmChangeEmailUseCase @Inject constructor(
    @MegaApi private val megaApi: MegaApiAndroid,
    @ApplicationContext private val context: Context,
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
                            API_EEXIST -> context.getString(R.string.mail_already_used)
                            API_ENOENT -> context.getString(R.string.old_password_provided_incorrect)
                            else -> context.getString(R.string.general_text_error)
                        }
                    )
                })
            )
        }
}