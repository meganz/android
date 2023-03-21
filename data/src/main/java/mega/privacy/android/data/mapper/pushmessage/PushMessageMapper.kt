package mega.privacy.android.data.mapper.pushmessage

import androidx.work.Data
import mega.privacy.android.domain.entity.pushes.PushMessage
import javax.inject.Inject

/**
 * Mapper to convert [Data] to [PushMessage].
 */
class PushMessageMapper @Inject constructor() {

    /**
     * Invoke.
     *
     * @param data [Data]
     */
    operator fun invoke(data: Data) = PushMessage(
        type = data.getString(PushMessage.KEY_TYPE),
        email = data.getString(PushMessage.KEY_EMAIL),
        silent = data.getString(PushMessage.KEY_SILENT),
        chatId = data.getString(PushMessage.KEY_CHAT_ID),
    )
}