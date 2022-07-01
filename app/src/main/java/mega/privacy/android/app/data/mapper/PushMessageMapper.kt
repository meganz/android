package mega.privacy.android.app.data.mapper

import androidx.work.Data
import mega.privacy.android.app.domain.entity.pushes.PushMessage

/**
 * Mapper to convert [Data] to [PushMessage].
 */
typealias PushMessageMapper = (@JvmSuppressWildcards Data) -> @JvmSuppressWildcards PushMessage

internal fun toPushMessage(data: Data): PushMessage =
    PushMessage(
        type = data.getString(PushMessage.KEY_TYPE),
        email = data.getString(PushMessage.KEY_EMAIL),
        silent = data.getString(PushMessage.KEY_SILENT)
    )