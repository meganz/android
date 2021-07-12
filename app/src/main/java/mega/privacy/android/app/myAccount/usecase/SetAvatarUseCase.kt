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

    fun set(avatarPath: String): Single<Pair<Boolean, Boolean>> = setAvatar(avatarPath)

    fun remove(): Single<Pair<Boolean, Boolean>> = setAvatar(null)

    private fun setAvatar(avatarPath: String?): Single<Pair<Boolean, Boolean>> =
        Single.create { emitter ->
            megaApi.setAvatar(avatarPath, OptionalMegaRequestListenerInterface(
                onRequestFinish = { request, error ->
                    emitter.onSuccess(Pair(request.file != null, error.errorCode == API_OK))
                }
            ))
        }
}