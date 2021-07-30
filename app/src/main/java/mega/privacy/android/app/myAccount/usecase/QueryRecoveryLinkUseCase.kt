package mega.privacy.android.app.myAccount.usecase

import io.reactivex.rxjava3.core.Single
import mega.privacy.android.app.R
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import mega.privacy.android.app.utils.StringResourcesUtils.getString
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaError.*
import javax.inject.Inject

class QueryRecoveryLinkUseCase @Inject constructor(
    @MegaApi private val megaApi: MegaApiAndroid
) {

    /**
     * Launches a request to get the current account user data.
     * Launches a broadcast to update user data if finishes with success.
     *
     * @return Single<Boolean> True if the request finished with success, false if not.
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
}