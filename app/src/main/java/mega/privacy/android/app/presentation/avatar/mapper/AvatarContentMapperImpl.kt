package mega.privacy.android.app.presentation.avatar.mapper

import android.content.Context
import androidx.compose.ui.unit.TextUnit
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.app.R
import mega.privacy.android.app.components.twemoji.wrapper.EmojiManagerWrapper
import mega.privacy.android.app.presentation.avatar.model.AvatarContent
import mega.privacy.android.app.presentation.avatar.model.EmojiAvatarContent
import mega.privacy.android.app.presentation.avatar.model.PhotoAvatarContent
import mega.privacy.android.app.presentation.avatar.model.TextAvatarContent
import mega.privacy.android.data.wrapper.AvatarWrapper
import java.io.File
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
     * @param localFile local path of the avatar photo
     * @return specific types of [AvatarContent]
     */
    override suspend fun invoke(
        fullName: String?,
        localFile: File?,
        showBorder: Boolean,
        textSize: TextUnit,
        backgroundColor: suspend () -> Int,
    ): AvatarContent =
        mapAvatarPathToPhoto(localFile, showBorder)
            ?: mapNameToAvatarText(fullName).let { name ->
                val color = backgroundColor()
                mapAvatarTextToEmoji(name, color, showBorder)
                    ?: TextAvatarContent(name, color, showBorder, textSize)
            }

    private fun mapAvatarPathToPhoto(localFile: File?, showBorder: Boolean): PhotoAvatarContent? =
        localFile?.takeIf { it.exists() && it.length() > 0 }?.let {
            PhotoAvatarContent(localFile.toURI().toString(), showBorder)
        }

    private fun mapNameToAvatarText(name: String?) = avatarWrapper.getFirstLetter(
        name
            ?: "${context.getString(R.string.first_name_text)} ${context.getString(R.string.lastname_text)}"
    )

    private suspend fun mapAvatarTextToEmoji(
        avatarText: String,
        backgroundColor: Int,
        showBorder: Boolean,
    ) = emojiManagerWrapper.getFirstEmoji(avatarText)
        ?.let { emojiDrawable ->
            EmojiAvatarContent(
                emojiDrawable,
                backgroundColor,
                showBorder
            )
        }
}
