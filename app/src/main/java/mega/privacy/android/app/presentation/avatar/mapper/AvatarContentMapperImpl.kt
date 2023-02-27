package mega.privacy.android.app.presentation.avatar.mapper

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.app.R
import mega.privacy.android.app.components.twemoji.wrapper.EmojiManagerWrapper
import mega.privacy.android.app.presentation.avatar.model.AvatarContent
import mega.privacy.android.app.presentation.avatar.model.EmojiAvatarContent
import mega.privacy.android.app.presentation.avatar.model.TextAvatarContent
import mega.privacy.android.data.wrapper.AvatarWrapper
import javax.inject.Inject

/**
 * Implementation of [AvatarContentMapper]
 */
class AvatarContentMapperImpl @Inject constructor(
    private val avatarWrapper: AvatarWrapper,
    private val emojiManagerWrapper: EmojiManagerWrapper,
    @ApplicationContext private val context: Context,
) : AvatarContentMapper {

    /**
     * Convert [fullName] to an [AvatarContent].
     *
     * @param fullName full name
     * @return specific types of [AvatarContent]
     */
    override suspend fun invoke(fullName: String?) =
        mapNameToAvatarText(fullName).let {
            mapAvatarTextToEmoji(it) ?: TextAvatarContent(it)
        }

    private fun mapNameToAvatarText(name: String?) = avatarWrapper.getFirstLetter(
        name
            ?: "${context.getString(R.string.first_name_text)} ${context.getString(R.string.lastname_text)}"
    )

    private suspend fun mapAvatarTextToEmoji(avatarText: String) =
        emojiManagerWrapper.getFirstEmoji(avatarText)
            ?.let { emojiDrawable -> EmojiAvatarContent(emojiDrawable) }
}
