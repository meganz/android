package mega.privacy.android.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import mega.privacy.android.data.database.MegaDatabaseConstant
import mega.privacy.android.domain.entity.transfer.ActiveTransferGroup
import mega.privacy.android.domain.entity.transfer.TransferType

/**
 * Database implementation of active transfer group
 */
@Entity(
    MegaDatabaseConstant.TABLE_ACTIVE_TRANSFER_GROUPS,
)
data class ActiveTransferGroupEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "group_id")
    override val groupId: Int? = null,
    @ColumnInfo(name = "transfer_type")
    override val transferType: TransferType,
    @ColumnInfo(name = "destination")
    override val destination: String,
    @ColumnInfo(name = "fileName")
    override val singleFileName: String? = null,
) : ActiveTransferGroup