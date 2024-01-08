package mega.privacy.android.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import mega.privacy.android.data.database.MegaDatabaseConstant
import mega.privacy.android.domain.entity.transfer.ActiveTransfer
import mega.privacy.android.domain.entity.transfer.TransferType

/**
 * Entity to save the currently active transfers
 *
 *
 * @param tag An integer that identifies this transfer.
 * @param transferType [TransferType] of this transfer.
 * @param totalBytes the total amount of bytes that will be transferred
 * @param isFinished true if the transfer has already finished but it's still part of the current
 * @param isFolderTransfer True if it's a folder transfer, false otherwise (file transfer).
 * @param isPaused True if the transfer is paused, false otherwise
 * @param isAlreadyDownloaded True if the transfer finished without actually transferring bytes because it was already transferred
 */
@Entity(
    MegaDatabaseConstant.TABLE_ACTIVE_TRANSFERS,
    indices = [Index(value = ["transfer_type"])]
)
internal data class ActiveTransferEntity(
    @PrimaryKey
    @ColumnInfo(name = "tag")
    override val tag: Int,
    @ColumnInfo(name = "transfer_type")
    override val transferType: TransferType,
    @ColumnInfo(name = "total_bytes")
    override val totalBytes: Long,
    @ColumnInfo(name = "is_finished")
    override val isFinished: Boolean,
    @ColumnInfo(name = "is_folder_transfer", defaultValue = "0")
    override val isFolderTransfer: Boolean,
    @ColumnInfo(name = "is_paused", defaultValue = "0")
    override val isPaused: Boolean,
    @ColumnInfo(name = "is_already_downloaded", defaultValue = "0")
    override val isAlreadyDownloaded: Boolean,
) : ActiveTransfer