package mega.privacy.android.app.myAccount.usecase

import io.reactivex.rxjava3.core.Completable
import mega.privacy.android.data.qualifier.MegaApi
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import mega.privacy.android.app.utils.ErrorUtils.toThrowable
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaError.API_OK
import javax.inject.Inject

class SetAvatarUseCase @Inject constructor(
    @MegaApi private val megaApi: MegaApiAndroid
) {

    /**
     * Launches a request to set a new avatar for the current account.
     *
     * @return Completable onComplete() if the request finished with success, error if not.
     */
    fun set(avatarPath: String): Completable = setAvatar(avatarPath)

    /**
     * Launches a request to set a remove the avatar of the current account.
     *
     * @return Completable onComplete() if the request finished with success, error if not.
     */
    fun remove(): Completable = setAvatar(null)

    /**
     * Launches a request to set or remove the avatar of the current account.
     *
     * @return Completable onComplete() if the request finished with success, error if not.
     */
    private fun setAvatar(avatarPath: String?): Completable =
        Completable.create { emitter ->
            megaApi.setAvatar(avatarPath, OptionalMegaRequestListenerInterface(
                onRequestFinish = { _, error ->
                    when (error.errorCode) {
                        API_OK -> emitter.onComplete()
                        else -> emitter.onError(error.toThrowable())
                    }
                }
            ))
        }
}