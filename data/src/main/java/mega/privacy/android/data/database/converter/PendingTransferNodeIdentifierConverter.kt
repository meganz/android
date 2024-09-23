package mega.privacy.android.data.database.converter

import androidx.room.TypeConverter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import mega.privacy.android.domain.entity.transfer.pending.PendingTransferNodeIdentifier

/**
 * Converter for [PendingTransferNodeIdentifier]
 */
internal class PendingTransferNodeIdentifierConverter {

    @TypeConverter
    fun fromNodeIdentifier(nodeIdentifier: PendingTransferNodeIdentifier): String =
        Json.encodeToString(nodeIdentifier)

    @TypeConverter
    fun toNodeIdentifier(serializedData: String): PendingTransferNodeIdentifier =
        Json.decodeFromString(serializedData)
}