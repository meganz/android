package mega.privacy.android.app.presentation.meeting.chat.mapper

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.app.R
import javax.inject.Inject

/**
 * Map full name according to isMe
 *
 * @property context
 */
class ParticipantNameMapper @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    /**
     * get the full name according to whether the user is me.
     *
     * @param isMe
     * @param fullName
     * @return full name
     */
    operator fun invoke(isMe: Boolean, fullName: String): String =
        if (isMe) {
            context.getString(R.string.chat_me_text_bracket, fullName)
        } else {
            fullName
        }
}