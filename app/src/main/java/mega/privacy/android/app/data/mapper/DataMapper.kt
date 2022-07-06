package mega.privacy.android.app.data.mapper

import androidx.work.Data
import mega.privacy.android.domain.entity.pushes.PushMessage

/**
 * Mapper to convert [PushMessage] to [Data].
 */
typealias DataMapper = (@JvmSuppressWildcards PushMessage) -> @JvmSuppressWildcards Data

internal fun toData(pushMessage: PushMessage): Data = Data.Builder()
    .putString(PushMessage.KEY_TYPE, pushMessage.type)
    .putString(PushMessage.KEY_EMAIL, pushMessage.email)
    .putString(PushMessage.KEY_SILENT, pushMessage.silent)
    .build()