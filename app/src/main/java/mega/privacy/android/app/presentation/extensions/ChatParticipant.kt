package mega.privacy.android.app.presentation.extensions

import mega.privacy.android.domain.entity.chat.ChatParticipant

/**
 * Retrieve the avatar first letter of a [ChatParticipant].
 *
 * @return The first letter of the string to be painted in the default avatar.
 */
fun ChatParticipant.getAvatarFirstLetter(): String =
    getAvatarFirstLetter(data.alias ?: data.fullName ?: email)