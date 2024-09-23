package mega.privacy.android.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import mega.privacy.android.data.database.MegaDatabaseConstant.TABLE_PENDING_TRANSFER
import mega.privacy.android.data.database.converter.PendingTransferNodeIdentifierConverter
import mega.privacy.android.domain.entity.transfer.TransferStage
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.entity.transfer.pending.PendingTransfer
import mega.privacy.android.domain.entity.transfer.pending.PendingTransferNodeIdentifier
import mega.privacy.android.domain.entity.transfer.pending.PendingTransferState

/**
 * Pending transfer database entity for [PendingTransfer]
 * @property pendingTransferId
 * @property transferTag
 * @property transferType
 * @property nodeIdentifier
 * @property path
 * @property appData
 * @property isHighPriority
 * @property scanningFoldersData
 * @property startedFiles
 * @property alreadyTransferred
 * @property state
 */
@Entity(
    tableName = TABLE_PENDING_TRANSFER,
    indices = [Index(value = ["state", "transferTag", "transferType"])]
)
@TypeConverters(PendingTransferNodeIdentifierConverter::class)
data class PendingTransferEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo("pendingTransferId") val pendingTransferId: Long? = null,
    @ColumnInfo("transferTag") val transferTag: Int?,
    @ColumnInfo("transferType") val transferType: TransferType,
    @ColumnInfo("nodeIdentifier") val nodeIdentifier: PendingTransferNodeIdentifier,
    @ColumnInfo("path") val path: String,
    @ColumnInfo("appData") val appData: String?,
    @ColumnInfo("isHighPriority") val isHighPriority: Boolean,
    @Embedded val scanningFoldersData: ScanningFoldersDataEntity,
    @ColumnInfo("startedFiles") val startedFiles: Int = 0,
    @ColumnInfo("alreadyTransferred") val alreadyTransferred: Int,
    @ColumnInfo("state") val state: PendingTransferState,
) {

    /**
     * Pending transfer data related to scanning folders process
     * @property stage the stage of this transfer scanning process
     * @property fileCount the number of files scanned
     * @property folderCount the number of folders scanned
     * @property createdFolderCount the number of folders already created
     */
    data class ScanningFoldersDataEntity(
        @ColumnInfo("stage") val stage: TransferStage = TransferStage.STAGE_NONE,
        @ColumnInfo("fileCount") val fileCount: Int = 0,
        @ColumnInfo("folderCount") val folderCount: Int = 0,
        @ColumnInfo("createdFolderCount") val createdFolderCount: Int = 0,
    )
}
