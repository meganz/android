package mega.privacy.android.app.myAccount.usecase

import com.jeremyliao.liveeventbus.LiveEventBus
import io.reactivex.rxjava3.core.Single
import mega.privacy.android.app.R
import mega.privacy.android.app.constants.EventConstants.EVENT_USER_EMAIL_UPDATED
import mega.privacy.android.data.qualifier.MegaApi
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import mega.privacy.android.app.utils.StringResourcesUtils.getString
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaError.*
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
                            API_OK -> {
                                LiveEventBus
                                    .get(EVENT_USER_EMAIL_UPDATED, Boolean::class.java)
                                    .post(true)

                                request.email
                            }
                            API_EEXIST -> getString(R.string.mail_already_used)
                            API_ENOENT -> getString(R.string.old_password_provided_incorrect)
                            else -> getString(R.string.general_text_error)
                        }
                    )
                })
            )
        }
}