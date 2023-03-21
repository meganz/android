package mega.privacy.android.data.mapper.pushmessage

import androidx.work.Data
import mega.privacy.android.domain.entity.pushes.PushMessage
import javax.inject.Inject

/**
 * Mapper to convert [PushMessage] to [Data].
 */
class DataMapper @Inject constructor() {

    /**
     * Invoke.
     *
     * @param pushMessage [PushMessage]
     */
    operator fun invoke(pushMessage: PushMessage) = Data.Builder()
        .putString(PushMessage.KEY_TYPE, pushMessage.type)
        .putString(PushMessage.KEY_EMAIL, pushMessage.email)
        .putString(PushMessage.KEY_SILENT, pushMessage.silent)
        .putString(PushMessage.KEY_CHAT_ID, pushMessage.chatId)
        .build()
}