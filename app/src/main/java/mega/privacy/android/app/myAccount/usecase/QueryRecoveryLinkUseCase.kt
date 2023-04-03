package mega.privacy.android.app.myAccount.usecase

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.rxjava3.core.Single
import mega.privacy.android.app.R
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import mega.privacy.android.data.qualifier.MegaApi
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaError.API_EACCESS
import nz.mega.sdk.MegaError.API_EEXPIRED
import nz.mega.sdk.MegaError.API_OK
import javax.inject.Inject

/**
 * Query recovery link use case
 *
 * @property megaApi
 * @property context
 * @constructor Create empty Query recovery link use case
 */
class QueryRecoveryLinkUseCase @Inject constructor(
    @MegaApi private val megaApi: MegaApiAndroid,
    @ApplicationContext private val context: Context,
) {

    /**
     * Launches a request to query an account cancellation link.
     *
     * @param link The account cancellation link.
     * @return Single<String> The link if success, the corresponding error if not.
     */
    fun queryCancelAccount(link: String): Single<String> =
        Single.create { emitter ->
            megaApi.queryCancelLink(
                link,
                OptionalMegaRequestListenerInterface(onRequestFinish = { request, error ->
                    emitter.onSuccess(
                        when (error.errorCode) {
                            API_OK -> request.link
                            API_EACCESS -> context.getString(R.string.error_not_logged_with_correct_account)
                            API_EEXPIRED -> context.getString(R.string.cancel_link_expired)
                            else -> context.getString(R.string.invalid_link)
                        }
                    )
                })
            )
        }

    /**
     * Launches a request to query an email change link.
     *
     * @param link The email change link.
     * @return The link if success, the corresponding error if not.
     */
    fun queryChangeEmail(link: String): Single<String> =
        Single.create { emitter ->
            megaApi.queryChangeEmailLink(
                link,
                OptionalMegaRequestListenerInterface(onRequestFinish = { request, error ->
                    emitter.onSuccess(
                        when (error.errorCode) {
                            API_OK -> request.link
                            API_EACCESS -> context.getString(R.string.account_change_email_error_not_logged_with_correct_account_message)
                            else -> context.getString(R.string.invalid_link)
                        }
                    )
                })
            )
        }
}