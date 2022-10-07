package mega.privacy.android.app.usecase

import android.content.Context
import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.rxjava3.core.Single
import mega.privacy.android.app.R
import mega.privacy.android.data.qualifier.MegaApi
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import mega.privacy.android.app.utils.AvatarUtil
import mega.privacy.android.app.utils.AvatarUtil.getAvatarUri
import mega.privacy.android.app.utils.ErrorUtils.toThrowable
import mega.privacy.android.app.utils.StringUtils.toThrowable
import mega.privacy.android.app.utils.view.TextDrawable
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
     * Enum defining the type of avatar.
     */
    enum class AvatarType{
        MINI, GENERAL, LINK
    }

    /**
     * Gets the avatar of a MegaUser.
     *
     * @param email     MegaUser's email.
     * @param fileDest  Absolute path of the file in which the avatar will be stored.
     * @return The Uri of the avatar.
     */
    fun get(email: String, fileDest: String): Single<Uri> =
        Single.create { emitter ->
            megaApi.getUserAvatar(email, fileDest, OptionalMegaRequestListenerInterface(
                onRequestFinish = { _, error ->
                    if (error.errorCode == MegaError.API_OK) {
                        val uri = getAvatarUri(File(fileDest))

                        if (uri != null) {
                            emitter.onSuccess(uri)
                            return@OptionalMegaRequestListenerInterface
                        }

                        emitter.onError("Cannot get avatar".toThrowable())
                    } else {
                        emitter.onError(error.toThrowable())
                    }
                }
            ))
        }

    /**
     * Gets the default Avatar as drawable.
     *
     * @param context Required Context to get resources.
     * @param name  Text to get the letter to show.
     * @param color Background color.
     * @param type  Avatar type.
     * @return The default Avatar as drawable.
     */
    fun getDefaultAvatarDrawable(
        @ApplicationContext context: Context,
        name: String?,
        @ColorInt color: Int,
        type: AvatarType
    ): Drawable {
        val avatarSize = context.resources.getDimensionPixelSize(
            when (type) {
                AvatarType.MINI -> R.dimen.image_mini_avatar_size
                AvatarType.LINK -> R.dimen.image_link_avatar_size
                else -> R.dimen.image_contact_size
            }
        )

        val fontSize = context.resources.getDimensionPixelSize(
            when (type) {
                AvatarType.MINI -> R.dimen.image_mini_avatar_text_size
                AvatarType.LINK -> R.dimen.image_link_avatar_text_size
                else -> R.dimen.image_contact_text_size
            }
        )

        return TextDrawable.builder()
            .beginConfig()
            .width(avatarSize)
            .height(avatarSize)
            .fontSize(fontSize)
            .textColor(ContextCompat.getColor(context, R.color.white))
            .bold()
            .toUpperCase()
            .endConfig()
            .buildRound(AvatarUtil.getFirstLetter(name), color)
    }

}