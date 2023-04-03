package mega.privacy.android.app.myAccount.usecase

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.rxjava3.core.Completable
import mega.privacy.android.app.R
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import mega.privacy.android.app.utils.StringUtils.toThrowable
import mega.privacy.android.data.qualifier.MegaApi
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaError.API_ENOENT
import nz.mega.sdk.MegaError.API_OK
import javax.inject.Inject

/**
 * Confirm cancel account use case
 *
 * @property megaApi
 * @property context
 */
class ConfirmCancelAccountUseCase @Inject constructor(
    @MegaApi private val megaApi: MegaApiAndroid,
    @ApplicationContext private val context: Context,
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
                            emitter.onError(
                                context.getString(R.string.old_password_provided_incorrect)
                                    .toThrowable()
                            )
                        else ->
                            emitter.onError(
                                context.getString(R.string.general_text_error).toThrowable()
                            )
                    }
                })
            )
        }
}