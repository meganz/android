package mega.privacy.android.app.myAccount.usecase

import io.reactivex.rxjava3.core.Single
import mega.privacy.android.app.R
import mega.privacy.android.data.qualifier.MegaApi
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import mega.privacy.android.app.utils.StringResourcesUtils.getString
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaError.*
import javax.inject.Inject

class QueryRecoveryLinkUseCase @Inject constructor(
    @MegaApi private val megaApi: MegaApiAndroid
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
                            API_EACCESS -> getString(R.string.error_not_logged_with_correct_account)
                            API_EEXPIRED -> getString(R.string.cancel_link_expired)
                            else -> getString(R.string.invalid_link)
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
                            API_EACCESS -> getString(R.string.account_change_email_error_not_logged_with_correct_account_message)
                            else -> getString(R.string.invalid_link)
                        }
                    )
                })
            )
        }
}