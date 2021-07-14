package mega.privacy.android.app.myAccount.usecase

import io.reactivex.rxjava3.core.Single
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaError.API_OK
import javax.inject.Inject

class SetAvatarUseCase @Inject constructor(
    @MegaApi private val megaApi: MegaApiAndroid
) {

    /**
     * Launches a request to set a new avatar for the current account.
     *
     * @return Single<Pair<Boolean, Boolean>>
     *          - First: True, indicates the set action.
     *          - Second: True if the request finished with success, error if not.
     */
    fun set(avatarPath: String): Single<Pair<Boolean, Boolean>> = setAvatar(avatarPath)

    /**
     * Launches a request to set a remove the avatar of the current account.
     *
     * @return Single<Pair<Boolean, Boolean>>
     *          - First: False, indicates the remove action.
     *          - Second: True if the request finished with success, error if not.
     */
    fun remove(): Single<Pair<Boolean, Boolean>> = setAvatar(null)

    /**
     * Launches a request to set or remove the avatar of the current account.
     *
     * @return Single<Pair<Boolean, Boolean>>
     *          - First: True if is set action, false if is remove action.
     *          - Second: True if the request finished with success, error if not.
     */
    private fun setAvatar(avatarPath: String?): Single<Pair<Boolean, Boolean>> =
        Single.create { emitter ->
            megaApi.setAvatar(avatarPath, OptionalMegaRequestListenerInterface(
                onRequestFinish = { request, error ->
                    emitter.onSuccess(Pair(request.file != null, error.errorCode == API_OK))
                }
            ))
        }
}