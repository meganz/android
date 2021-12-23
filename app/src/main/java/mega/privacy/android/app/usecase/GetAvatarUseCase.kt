package mega.privacy.android.app.usecase

import android.graphics.Bitmap
import io.reactivex.rxjava3.core.Single
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import mega.privacy.android.app.utils.AvatarUtil.getAvatarBitmap
import mega.privacy.android.app.utils.ErrorUtils.toThrowable
import mega.privacy.android.app.utils.StringUtils.toThrowable
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaError
import java.io.File
import javax.inject.Inject

/**
 * Use case for getting users' avatars
 *
 * @property megaApi MegaApiAndroid instance to use.
 */
class GetAvatarUseCase @Inject constructor(
    @MegaApi private val megaApi: MegaApiAndroid
) {

    /**
     * Gets the avatar of a MegaUser.
     *
     * @param email     MegaUser's email.
     * @param fileDest  Absolute path of the file in which the avatar will be stored.
     * @return The Bitmap of the avatar.
     */
    fun get(email: String, fileDest: String): Single<Bitmap> =
        Single.create { emitter ->
            megaApi.getUserAvatar(email, fileDest, OptionalMegaRequestListenerInterface(
                onRequestFinish = { _, error ->
                    if (error.errorCode == MegaError.API_OK) {
                        val bitmap = getAvatarBitmap(File(fileDest))

                        if (bitmap != null) {
                            emitter.onSuccess(bitmap)
                            return@OptionalMegaRequestListenerInterface
                        }

                        emitter.onError("Cannot get avatar".toThrowable())
                    } else {
                        emitter.onError(error.toThrowable())
                    }
                }
            ))
        }
}