package mega.privacy.android.app.presentation.meeting.chat.saver

import androidx.compose.runtime.saveable.Saver
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import mega.privacy.android.domain.entity.chat.messages.TypedMessage
import javax.inject.Inject

/**
 * Chat savers
 *
 * @property json
 * @constructor Create empty Chat savers
 */
class ChatSavers @Inject constructor(
    private val json: Json,
) {
    /**
     * Message list saver
     */
    val messageSetSaver: Saver<Set<TypedMessage>, String> = Saver(
        save = { messages -> json.encodeToString<Set<TypedMessage>>(messages) },
        restore = { jsonString -> json.decodeFromString(jsonString) }
    )
}