package mega.privacy.android.app.presentation.avatar.mapper

import android.graphics.Bitmap
import androidx.annotation.ColorInt

/**
 * Mapper to create avatar bitmap
 */
interface AvatarMapper {

    /**
     * Retrieve the default avatar bitmap.
     *
     * @param color The color of the avatar's background.
     * @param text  The letter to be painted on the avatar.
     * @param isList Grid or list indicator.
     * @return Bitmap with the default avatar built in.
     */
    suspend fun getDefaultAvatar(
        @ColorInt color: Int,
        text: String,
        isList: Boolean,
    ): Bitmap

}