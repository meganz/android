package mega.privacy.android.app.usecase

import io.reactivex.rxjava3.core.Single
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaError
import javax.inject.Inject

/**
 * Use case for querying signup links.
 *
 * @property megaApi    Required for querying the link.
 */
class QuerySignupLinkUseCase @Inject constructor(
    @MegaApi private val megaApi: MegaApiAndroid
) {

    /**
     * Queries a signup link.
     *
     * @param link The link to query.
     * @return The email related to the signup link if success, an exception if not.
     */
    fun query(link: String): Single<String> =
        Single.create { emitter ->
            megaApi.querySignupLink(
                link,
                OptionalMegaRequestListenerInterface(onRequestFinish = { request, error ->
                    when {
                        emitter.isDisposed -> return@OptionalMegaRequestListenerInterface
                        error.errorCode == MegaError.API_OK -> emitter.onSuccess(request.email)
                        else -> emitter.onError(error.toMegaException())
                    }
                })
            )
        }
}