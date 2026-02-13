package mega.privacy.android.feature.myaccount.presentation.mapper

import android.content.Context
import androidx.compose.ui.unit.TextUnit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import mega.privacy.android.data.wrapper.AvatarWrapper
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.feature.myaccount.presentation.model.AvatarContent
import mega.privacy.android.feature.myaccount.presentation.model.EmojiAvatarContent
import mega.privacy.android.feature.myaccount.presentation.model.PhotoAvatarContent
import mega.privacy.android.feature.myaccount.presentation.model.TextAvatarContent
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.android.thirdpartylib.twemoji.wrapper.EmojiManagerWrapper
import java.io.File
import javax.inject.Inject

/**
 * Implementation of [AvatarContentMapper]
 */
class AvatarContentMapperImpl @Inject constructor(
    private val avatarWrapper: AvatarWrapper,
    private val emojiManagerWrapper: EmojiManagerWrapper,
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher, // file execution need to move to background work
) : AvatarContentMapper {

    override suspend fun invoke(
        fullName: String?,
        localFile: File?,
        showBorder: Boolean,
        textSize: TextUnit,
        backgroundColor: Int,
    ): AvatarContent = withContext(ioDispatcher) {
        mapAvatarPathToPhoto(localFile, showBorder)
            ?: mapNameToAvatarText(fullName).let { name ->
                mapAvatarTextToEmoji(name, backgroundColor, showBorder)
                    ?: TextAvatarContent(name, backgroundColor, showBorder, textSize)
            }
    }

    private fun mapAvatarPathToPhoto(localFile: File?, showBorder: Boolean): PhotoAvatarContent? =
        localFile?.takeIf { it.exists() && it.length() > 0 }?.let {
            PhotoAvatarContent(localFile.toURI().toString(), localFile.length(), showBorder)
        }

    private fun mapNameToAvatarText(name: String?) = avatarWrapper.getFirstLetter(
        name
            ?: "${context.getString(sharedR.string.general_first_name)} ${context.getString(sharedR.string.general_last_name)}"
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
