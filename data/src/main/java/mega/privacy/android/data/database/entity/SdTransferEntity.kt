package mega.privacy.android.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import mega.privacy.android.data.database.MegaDatabaseConstant

/**
 * SD transfer entity
 *
 * @property id
 * @property tag
 * @property encryptedName
 * @property encryptedSize
 * @property encryptedHandle
 * @property encryptedAppData
 * @property encryptedPath
 * @constructor Create empty S d transfer entity
 */
@Entity(MegaDatabaseConstant.TABLE_SD_TRANSFERS)
data class SdTransferEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id") val id: Int? = null,
    @ColumnInfo(name = "sdtransfertag") val tag: Int?,
    @ColumnInfo(name = "sdtransfername") val encryptedName: String?,
    @ColumnInfo(name = "sdtransfersize") val encryptedSize: String?,
    @ColumnInfo(name = "sdtransferhandle") val encryptedHandle: String?,
    @ColumnInfo(name = "sdtransferappdata") val encryptedAppData: String?,
    @ColumnInfo(name = "sdtransferpath") val encryptedPath: String?,
)