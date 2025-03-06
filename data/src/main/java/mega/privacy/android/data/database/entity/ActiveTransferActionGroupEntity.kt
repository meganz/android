package mega.privacy.android.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import mega.privacy.android.data.database.MegaDatabaseConstant
import mega.privacy.android.domain.entity.transfer.ActiveTransferActionGroup
import mega.privacy.android.domain.entity.transfer.TransferType

/**
 * Database implementation of active transfer action group
 */
@Entity(
    MegaDatabaseConstant.TABLE_ACTIVE_TRANSFER_ACTION_GROUPS,
)
data class ActiveTransferActionGroupEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "group_id")
    override val groupId: Int? = null,
    @ColumnInfo(name = "transfer_type")
    override val transferType: TransferType,
    @ColumnInfo(name = "destination")
    override val destination: String,
    @ColumnInfo(name = "start_time")
    override val startTime: Long? = null
) : ActiveTransferActionGroup