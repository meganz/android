package mega.privacy.android.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import mega.privacy.android.data.database.MegaDatabaseConstant
import mega.privacy.android.domain.entity.transfer.TransferType

/**
 * Entity to save the currently active transfers
 *
 *
 * @param tag An integer that identifies this transfer.
 * @param transferType [TransferType] of this transfer.
 * @param totalBytes the total amount of bytes that will be transferred
 * @param transferredBytes the current amount of bytes already transferred
 * @param isFinished true if the transfer has already finished but it's still part of the current
 */
@Entity(MegaDatabaseConstant.TABLE_ACTIVE_TRANSFERS)
internal data class ActiveTransferEntity(
    @PrimaryKey
    @ColumnInfo(name = "tag") val tag: Int,
    @ColumnInfo(name = "transfer_type") val transferType: TransferType,
    @ColumnInfo(name = "total_bytes") val totalBytes: Long,
    @ColumnInfo(name = "transferred_bytes") val transferredBytes: Long,
    @ColumnInfo(name = "is_finished") val isFinished: Boolean,
)