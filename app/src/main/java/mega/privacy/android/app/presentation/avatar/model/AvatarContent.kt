package mega.privacy.android.app.presentation.avatar.model

import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * Avatar content
 *
 */
sealed interface AvatarContent

/**
 * A default avatar that shows a letter
 * @property avatarText First letter of user's full name
 * @property backgroundColor
 * @property showBorder
 * @property textSize
 */
data class TextAvatarContent(
    val avatarText: String,
    @ColorInt val backgroundColor: Int,
    val showBorder: Boolean = true,
    val textSize: TextUnit = 38.sp,
) : AvatarContent

/**
 * A default avatar that shows an emoji
 * @property emojiContent Drawable resource ID of the emoji if user's full name starts with
 * emoji.
 * @property backgroundColor
 * @property showBorder
 */
data class EmojiAvatarContent(
    @DrawableRes val emojiContent: Int,
    @ColorInt val backgroundColor: Int,
    val showBorder: Boolean = true,
) : AvatarContent


/**
 * A photo avatar
 * @property path Path of a photo avatar
 * @property size we use the size here because StateFlow will not emit if the path the same,
 * size parameter to know the avatar file already changed
 * @property showBorder
 */
data class PhotoAvatarContent(
    val path: String,
    val size: Long,
    val showBorder: Boolean = true,
) : AvatarContent
