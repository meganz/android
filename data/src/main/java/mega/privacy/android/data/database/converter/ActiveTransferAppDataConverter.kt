package mega.privacy.android.data.database.converter

import androidx.room.TypeConverter
import mega.privacy.android.data.mapper.transfer.TransferAppDataMapper
import mega.privacy.android.data.mapper.transfer.TransferAppDataStringMapper
import mega.privacy.android.domain.entity.transfer.TransferAppData
import mega.privacy.android.domain.entity.transfer.pending.PendingTransferNodeIdentifier
import javax.inject.Inject

/**
 * Converter for [PendingTransferNodeIdentifier]
 */
class ActiveTransferAppDataConverter @Inject constructor() {

    /**
     * Convert a list of [TransferAppData] to a string.
     */
    @TypeConverter
    fun fromAppData(appData: List<TransferAppData>): String =
        TransferAppDataStringMapper().invoke(appData) ?: ""

    /**
     * Convert a string to a list of [TransferAppData].
     */
    @TypeConverter
    fun toAppData(serializedData: String): List<TransferAppData> =
        TransferAppDataMapper().invoke(serializedData)
}