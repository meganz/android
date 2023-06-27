package mega.privacy.android.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import mega.privacy.android.domain.entity.transfer.TransferType

/**
 * Entity to save the currently active transfers
 *
 *
 * @param tag An integer that identifies this transfer.
 * @param transferType [TransferType] of this transfer.
 * @param totalBytes the total amount of bytes that will be transferred
 * @param transferredBytes the current amount of bytes already transferred
 */
@Entity
internal data class ActiveTransferEntity(
    @PrimaryKey
    val tag: Int,
    val transferType: TransferType,
    val totalBytes: Long,
    val transferredBytes: Long,
)