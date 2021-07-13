package mega.privacy.android.app.myAccount.usecase

import io.reactivex.rxjava3.core.Single
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import mega.privacy.android.app.utils.ErrorUtils.toThrowable
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaError.API_OK
import javax.inject.Inject

class GetMyAvatarUseCase @Inject constructor(
    @MegaApi private val megaApi: MegaApiAndroid
) {
    fun get(avatarPath: String): Single<Boolean> =
        Single.create { emitter ->
            megaApi.getUserAvatar(megaApi.myUser,
                avatarPath,
                OptionalMegaRequestListenerInterface(
                    onRequestFinish = { _, error ->
                        if (error.errorCode == API_OK) {
                            emitter.onSuccess(true)
                        } else {
                            emitter.onError(error.toThrowable())
                        }
                    }
                ))
        }
}