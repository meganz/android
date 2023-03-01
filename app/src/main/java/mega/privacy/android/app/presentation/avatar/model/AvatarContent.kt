package mega.privacy.android.app.presentation.avatar.model

import androidx.annotation.DrawableRes

/**
 * Avatar content
 *
 */
sealed interface AvatarContent

/**
 * A default avatar that shows a letter
 * @property avatarText First letter of user's full name
 */
data class TextAvatarContent(val avatarText: String) : AvatarContent

/**
 * A default avatar that shows an emoji
 * @property emojiContent Drawable resource ID of the emoji if user's full name starts with
 * emoji.
 */
data class EmojiAvatarContent(@DrawableRes val emojiContent: Int) : AvatarContent


/**
 * A photo avatar
 * @property path Path of a photo avatar
 */
data class PhotoAvatarContent(val path: String) : AvatarContent
