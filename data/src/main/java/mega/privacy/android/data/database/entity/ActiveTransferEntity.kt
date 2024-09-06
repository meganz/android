package mega.privacy.android.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import mega.privacy.android.data.database.MegaDatabaseConstant
import mega.privacy.android.domain.entity.transfer.ActiveTransfer
import mega.privacy.android.domain.entity.transfer.TransferState
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
 * @param isAlreadyTransferred True if the transfer finished without actually transferring bytes because it was already transferred
 * @param localPath Local path related to this transfer. For uploads, this property is the path to the source file. For downloads, it is the path of the destination file.
 * @param nodeHandle Handle related to this transfer. For downloads, this property is the handle of the source node. It's not used for ActiveTransfer uploads as once it has a handle is not active anymore.
 * @param state [TransferState]
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
    override val isAlreadyTransferred: Boolean,
    @ColumnInfo(name = "local_path", defaultValue = " ")
    override val localPath: String,
    @ColumnInfo(name = "node_handle", defaultValue = "-1")
    override val nodeHandle: Long,
    @ColumnInfo(name = "state", defaultValue = "STATE_NONE")
    override val state: TransferState,
) : ActiveTransfer