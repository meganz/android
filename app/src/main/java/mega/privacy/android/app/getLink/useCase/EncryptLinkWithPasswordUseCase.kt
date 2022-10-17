package mega.privacy.android.app.getLink.useCase

import io.reactivex.rxjava3.core.Single
import mega.privacy.android.data.qualifier.MegaApi
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import mega.privacy.android.app.utils.ErrorUtils.toThrowable
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaError
import javax.inject.Inject

/**
 * Use case for encrypt a link of a node with a password.
 *
 * @property megaApi MegaApiAndroid instance to use.
 */
class EncryptLinkWithPasswordUseCase @Inject constructor(
    @MegaApi private val megaApi: MegaApiAndroid
) {
    /**
     * Launches a request to encrypt a link with a password.
     *
     * @param link     Link to encrypt.
     * @param password Password to encrypt the link.
     * @return Single<String> The encrypted link if the request finished with success, error if not.
     */
    fun encrypt(link: String, password: String): Single<String> =
        Single.create { emitter ->
            megaApi.encryptLinkWithPassword(link, password, OptionalMegaRequestListenerInterface(
                onRequestFinish = { request, error ->
                    if (error.errorCode == MegaError.API_OK) {
                        emitter.onSuccess(request.text)
                    } else {
                        emitter.onError(error.toThrowable())
                    }
                }
            ))
        }
}