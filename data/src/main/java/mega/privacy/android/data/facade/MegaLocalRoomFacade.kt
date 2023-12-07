package mega.privacy.android.data.facade

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import mega.privacy.android.data.cryptography.DecryptData
import mega.privacy.android.data.cryptography.EncryptData
import mega.privacy.android.data.database.dao.ActiveTransferDao
import mega.privacy.android.data.database.dao.BackupDao
import mega.privacy.android.data.database.dao.CameraUploadsRecordDao
import mega.privacy.android.data.database.dao.CompletedTransferDao
import mega.privacy.android.data.database.dao.ContactDao
import mega.privacy.android.data.database.dao.OfflineDao
import mega.privacy.android.data.database.dao.SdTransferDao
import mega.privacy.android.data.database.dao.SyncRecordDao
import mega.privacy.android.data.gateway.MegaLocalRoomGateway
import mega.privacy.android.data.mapper.SyncStatusIntMapper
import mega.privacy.android.data.mapper.backup.BackupEntityMapper
import mega.privacy.android.data.mapper.backup.BackupInfoTypeIntMapper
import mega.privacy.android.data.mapper.backup.BackupModelMapper
import mega.privacy.android.data.mapper.camerauploads.CameraUploadsRecordEntityMapper
import mega.privacy.android.data.mapper.camerauploads.CameraUploadsRecordModelMapper
import mega.privacy.android.data.mapper.camerauploads.SyncRecordEntityMapper
import mega.privacy.android.data.mapper.camerauploads.SyncRecordModelMapper
import mega.privacy.android.data.mapper.camerauploads.SyncRecordTypeIntMapper
import mega.privacy.android.data.mapper.contact.ContactEntityMapper
import mega.privacy.android.data.mapper.contact.ContactModelMapper
import mega.privacy.android.data.mapper.offline.OfflineEntityMapper
import mega.privacy.android.data.mapper.offline.OfflineModelMapper
import mega.privacy.android.data.mapper.transfer.active.ActiveTransferEntityMapper
import mega.privacy.android.data.mapper.transfer.completed.CompletedTransferEntityMapper
import mega.privacy.android.data.mapper.transfer.completed.CompletedTransferModelMapper
import mega.privacy.android.data.mapper.transfer.sd.SdTransferEntityMapper
import mega.privacy.android.data.mapper.transfer.sd.SdTransferModelMapper
import mega.privacy.android.domain.entity.Contact
import mega.privacy.android.domain.entity.Offline
import mega.privacy.android.domain.entity.SdTransfer
import mega.privacy.android.domain.entity.SyncRecord
import mega.privacy.android.domain.entity.SyncRecordType
import mega.privacy.android.domain.entity.SyncStatus
import mega.privacy.android.domain.entity.backup.Backup
import mega.privacy.android.domain.entity.backup.BackupInfoType
import mega.privacy.android.domain.entity.camerauploads.CameraUploadFolderType
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsRecord
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsRecordUploadStatus
import mega.privacy.android.domain.entity.transfer.ActiveTransfer
import mega.privacy.android.domain.entity.transfer.CompletedTransfer
import mega.privacy.android.domain.entity.transfer.TransferType
import javax.inject.Inject

internal class MegaLocalRoomFacade @Inject constructor(
    private val contactDao: ContactDao,
    private val contactEntityMapper: ContactEntityMapper,
    private val contactModelMapper: ContactModelMapper,
    private val completedTransferDao: CompletedTransferDao,
    private val activeTransferDao: ActiveTransferDao,
    private val completedTransferModelMapper: CompletedTransferModelMapper,
    private val completedTransferEntityMapper: CompletedTransferEntityMapper,
    private val activeTransferEntityMapper: ActiveTransferEntityMapper,
    private val syncRecordDao: SyncRecordDao,
    private val syncRecordModelMapper: SyncRecordModelMapper,
    private val syncRecordEntityMapper: SyncRecordEntityMapper,
    private val sdTransferDao: SdTransferDao,
    private val sdTransferModelMapper: SdTransferModelMapper,
    private val sdTransferEntityMapper: SdTransferEntityMapper,
    private val syncStatusIntMapper: SyncStatusIntMapper,
    private val syncRecordTypeIntMapper: SyncRecordTypeIntMapper,
    private val backupDao: BackupDao,
    private val backupEntityMapper: BackupEntityMapper,
    private val backupModelMapper: BackupModelMapper,
    private val backupInfoTypeIntMapper: BackupInfoTypeIntMapper,
    private val cameraUploadsRecordDao: CameraUploadsRecordDao,
    private val cameraUploadsRecordEntityMapper: CameraUploadsRecordEntityMapper,
    private val cameraUploadsRecordModelMapper: CameraUploadsRecordModelMapper,
    private val encryptData: EncryptData,
    private val decryptData: DecryptData,
    private val offlineDao: OfflineDao,
    private val offlineModelMapper: OfflineModelMapper,
    private val offlineEntityMapper: OfflineEntityMapper,
) : MegaLocalRoomGateway {
    override suspend fun insertContact(contact: Contact) {
        contactDao.insertOrUpdateContact(contactEntityMapper(contact))
    }

    override suspend fun updateContactNameByEmail(firstName: String?, email: String?) {
        if (email.isNullOrBlank()) return
        contactDao.getContactByEmail(encryptData(email))?.let { entity ->
            contactDao.insertOrUpdateContact(entity.copy(firstName = encryptData(firstName)))
        }
    }

    override suspend fun updateContactLastNameByEmail(lastName: String?, email: String?) {
        if (email.isNullOrBlank()) return
        contactDao.getContactByEmail(encryptData(email))?.let { entity ->
            contactDao.insertOrUpdateContact(entity.copy(lastName = encryptData(lastName)))
        }
    }

    override suspend fun updateContactMailByHandle(handle: Long, email: String?) {
        contactDao.getContactByHandle(encryptData(handle.toString()))?.let { entity ->
            contactDao.insertOrUpdateContact(entity.copy(mail = encryptData(email)))
        }
    }

    override suspend fun updateContactFistNameByHandle(handle: Long, firstName: String?) {
        contactDao.getContactByHandle(encryptData(handle.toString()))?.let { entity ->
            contactDao.insertOrUpdateContact(entity.copy(firstName = encryptData(firstName)))
        }
    }

    override suspend fun updateContactLastNameByHandle(handle: Long, lastName: String?) {
        contactDao.getContactByHandle(encryptData(handle.toString()))?.let { entity ->
            contactDao.insertOrUpdateContact(entity.copy(lastName = encryptData(lastName)))
        }
    }

    override suspend fun updateContactNicknameByHandle(handle: Long, nickname: String?) {
        contactDao.getContactByHandle(encryptData(handle.toString()))?.let { entity ->
            contactDao.insertOrUpdateContact(entity.copy(nickName = encryptData(nickname)))
        }
    }

    override suspend fun getContactByHandle(handle: Long): Contact? =
        contactDao.getContactByHandle(encryptData(handle.toString()))
            ?.let { contactModelMapper(it) }

    override suspend fun getContactByEmail(email: String?): Contact? =
        contactDao.getContactByEmail(encryptData(email))?.let { contactModelMapper(it) }

    override suspend fun deleteAllContacts() = contactDao.deleteAllContact()

    override suspend fun getContactCount() = contactDao.getContactCount()

    override suspend fun getAllContacts(): List<Contact> {
        val entities = contactDao.getAllContact().first()
        return entities.map { contactModelMapper(it) }
    }

    override fun getAllCompletedTransfers(size: Int?) =
        completedTransferDao.getAllCompletedTransfers()
            .map { list ->
                list.map { completedTransferModelMapper(it) }
                    .toMutableList()
                    .apply { sortWith(compareByDescending { it.timestamp }) }
                    .let { if (size != null) it.take(size) else it }
            }

    override suspend fun addCompletedTransfer(transfer: CompletedTransfer) {
        completedTransferDao.insertOrUpdateCompletedTransfer(completedTransferEntityMapper(transfer))
    }

    override suspend fun getCompletedTransfersCount() =
        completedTransferDao.getCompletedTransfersCount()

    override suspend fun deleteAllCompletedTransfers() =
        completedTransferDao.deleteAllCompletedTransfers()

    override suspend fun getCompletedTransfersByState(states: List<Int>): List<CompletedTransfer> {
        val encryptedStates = states.mapNotNull { encryptData(it.toString()) }
        return completedTransferDao.getCompletedTransfersByState(encryptedStates)
            .map { entity -> completedTransferModelMapper(entity) }
    }

    override suspend fun deleteCompletedTransfersByState(states: List<Int>): List<CompletedTransfer> {
        val encryptedStates = states.mapNotNull { encryptData(it.toString()) }
        val entities = completedTransferDao.getCompletedTransfersByState(encryptedStates)
        deleteCompletedTransferBatch(entities.mapNotNull { it.id })
        return entities.map { entity -> completedTransferModelMapper(entity) }
    }

    override suspend fun deleteCompletedTransfer(completedTransfer: CompletedTransfer) {
        completedTransferDao.deleteCompletedTransferByIds(
            listOf(completedTransfer.id ?: return)
        )
    }

    override suspend fun deleteOldestCompletedTransfers() {
        val count = completedTransferDao.getCompletedTransfersCount()
        if (count > MAX_COMPLETED_TRANSFER_ROWS) {
            val transfers = completedTransferDao.getAllCompletedTransfers().first()
                .map { completedTransferModelMapper(it) }
            val deletedTransfers =
                transfers.sortedWith(compareByDescending { it.timestamp })
                    .drop(MAX_COMPLETED_TRANSFER_ROWS)
                    .mapNotNull { it.id }

            if (deletedTransfers.isNotEmpty()) {
                deleteCompletedTransferBatch(deletedTransfers)
            }
        }
    }

    override suspend fun getActiveTransferByTag(tag: Int) =
        activeTransferDao.getActiveTransferByTag(tag)

    override fun getActiveTransfersByType(transferType: TransferType) =
        activeTransferDao.getActiveTransfersByType(transferType).map { activeTransferEntities ->
            activeTransferEntities.map { it }
        }

    override suspend fun getCurrentActiveTransfersByType(transferType: TransferType) =
        activeTransferDao.getCurrentActiveTransfersByType(transferType).map { it }

    override suspend fun insertOrUpdateActiveTransfer(activeTransfer: ActiveTransfer) =
        activeTransferDao.insertOrUpdateActiveTransfer(activeTransferEntityMapper(activeTransfer))

    override suspend fun deleteAllActiveTransfersByType(transferType: TransferType) =
        activeTransferDao.deleteAllActiveTransfersByType(transferType)

    override suspend fun setActiveTransferAsFinishedByTag(tags: List<Int>) =
        activeTransferDao.setActiveTransferAsFinishedByTag(tags)

    override suspend fun saveSyncRecord(record: SyncRecord) =
        syncRecordDao.insertOrUpdateSyncRecord(syncRecordEntityMapper(record))

    override suspend fun saveSyncRecords(records: List<SyncRecord>) {
        syncRecordDao.insertOrUpdateSyncRecords(
            records.map { record ->
                syncRecordEntityMapper(record)
            })
    }

    override suspend fun setUploadVideoSyncStatus(syncStatus: Int) =
        syncRecordDao.updateVideoState(syncStatus)

    override suspend fun doesFileNameExist(
        fileName: String,
        isSecondary: Boolean,
    ) = syncRecordDao.getSyncRecordCountByFileName(
        encryptData(fileName),
        encryptData(isSecondary.toString()).toString(),
    ) == 1

    override suspend fun doesLocalPathExist(
        fileName: String,
        isSecondary: Boolean,
    ) = syncRecordDao.getSyncRecordCountByOriginalPath(
        encryptData(fileName),
        encryptData(isSecondary.toString()).toString(),
    ) == 1

    override suspend fun getSyncRecordByFingerprint(
        fingerprint: String?,
        isSecondary: Boolean,
        isCopy: Boolean,
    ) = syncRecordDao.getSyncRecordByOriginalFingerprint(
        encryptData(fingerprint).toString(),
        encryptData(isSecondary.toString()).toString(),
        encryptData(isCopy.toString()).toString(),
    )?.let { syncRecordModelMapper(it) }

    override suspend fun getPendingSyncRecords() =
        syncRecordDao.getSyncRecordsBySyncState(syncStatusIntMapper(SyncStatus.STATUS_PENDING))
            .map { syncRecordModelMapper(it) }

    override suspend fun getVideoSyncRecordsByStatus(syncStatusType: Int) =
        syncRecordDao.getSyncRecordsBySyncStateAndType(
            syncState = syncStatusType,
            syncType = syncRecordTypeIntMapper(SyncRecordType.TYPE_VIDEO)
        ).map { syncRecordModelMapper(it) }

    override suspend fun deleteAllSyncRecords(syncRecordType: Int) =
        syncRecordDao.deleteSyncRecordsByType(syncRecordType)

    override suspend fun deleteAllSyncRecordsTypeAny() =
        syncRecordDao.deleteSyncRecordsByType(syncRecordTypeIntMapper(SyncRecordType.TYPE_ANY))

    override suspend fun deleteAllSecondarySyncRecords() =
        syncRecordDao.deleteSyncRecordsByIsSecondary(encryptData("true").toString())

    override suspend fun deleteAllPrimarySyncRecords() =
        syncRecordDao.deleteSyncRecordsByIsSecondary(encryptData("false").toString())

    override suspend fun getSyncRecordByLocalPath(path: String, isSecondary: Boolean) =
        syncRecordDao.getSyncRecordByOriginalPathAndIsSecondary(
            encryptData(path).toString(),
            encryptData(isSecondary.toString()).toString()
        )?.let { syncRecordModelMapper(it) }

    override suspend fun deleteSyncRecordByPath(path: String?, isSecondary: Boolean) {
        path?.let {
            syncRecordDao.deleteSyncRecordByOriginalPathOrNewPathAndIsSecondary(
                encryptData(it).toString(),
                encryptData(isSecondary.toString()).toString()
            )
        }
    }

    override suspend fun deleteSyncRecordByLocalPath(localPath: String?, isSecondary: Boolean) =
        syncRecordDao.deleteSyncRecordByOriginalPathAndIsSecondary(
            encryptData(localPath).toString(),
            encryptData(isSecondary.toString()).toString()
        )

    override suspend fun deleteSyncRecordByFingerPrint(
        originalPrint: String,
        newPrint: String,
        isSecondary: Boolean,
    ) = syncRecordDao.deleteSyncRecordByFingerprintsAndIsSecondary(
        encryptData(originalPrint),
        encryptData(newPrint),
        encryptData(isSecondary.toString()).toString()
    )

    override suspend fun updateSyncRecordStatusByLocalPath(
        syncStatusType: Int,
        localPath: String?,
        isSecondary: Boolean,
    ) = syncRecordDao.updateSyncRecordStateByOriginalPathAndIsSecondary(
        syncStatusType,
        encryptData(localPath).toString(),
        encryptData(isSecondary.toString()).toString()
    )

    override suspend fun getSyncRecordByNewPath(path: String) =
        syncRecordDao.getSyncRecordByNewPath(encryptData(path).toString())
            ?.let { syncRecordModelMapper(it) }

    override suspend fun getAllTimestampsOfSyncRecord(
        isSecondary: Boolean,
        syncRecordType: Int,
    ): List<Long> =
        syncRecordDao.getAllTimestampsByIsSecondaryAndSyncType(
            encryptData(isSecondary.toString()).toString(),
            syncRecordType
        ).mapNotNull { decryptData(it)?.toLongOrNull() }

    override suspend fun getAllSdTransfers(): List<SdTransfer> {
        val entities = sdTransferDao.getAllSdTransfers().first()
        return entities.map { sdTransferModelMapper(it) }
    }

    override suspend fun insertSdTransfer(transfer: SdTransfer) =
        sdTransferDao.insertSdTransfer(sdTransferEntityMapper(transfer))

    override suspend fun deleteSdTransferByTag(tag: Int) {
        sdTransferDao.deleteSdTransferByTag(tag)
    }

    override suspend fun getCompletedTransferById(id: Int) = completedTransferDao
        .getCompletedTransferById(id)?.let { completedTransferModelMapper(it) }

    override suspend fun insertOrUpdateCameraUploadsRecords(records: List<CameraUploadsRecord>) =
        cameraUploadsRecordDao.insertOrUpdateCameraUploadsRecords(
            records.map { cameraUploadsRecordEntityMapper(it) }
        )

    override suspend fun getAllCameraUploadsRecords(): List<CameraUploadsRecord> =
        cameraUploadsRecordDao.getAllCameraUploadsRecords().map {
            cameraUploadsRecordModelMapper(it)
        }

    override suspend fun getCameraUploadsRecordsBy(
        uploadStatus: List<CameraUploadsRecordUploadStatus>,
        types: List<SyncRecordType>,
        folderTypes: List<CameraUploadFolderType>,
    ): List<CameraUploadsRecord> =
        cameraUploadsRecordDao.getCameraUploadsRecordsBy(
            uploadStatus,
            types,
            folderTypes,
        ).map {
            cameraUploadsRecordModelMapper(it)
        }

    override suspend fun updateCameraUploadsRecordUploadStatus(
        mediaId: Long,
        timestamp: Long,
        folderType: CameraUploadFolderType,
        uploadStatus: CameraUploadsRecordUploadStatus,
    ) {
        val encryptedMediaId = encryptData(mediaId.toString())
        requireNotNull(encryptedMediaId)
        val encryptedTimestamp = encryptData(timestamp.toString())
        requireNotNull(encryptedTimestamp)

        cameraUploadsRecordDao.updateCameraUploadsRecordUploadStatus(
            encryptedMediaId,
            encryptedTimestamp,
            folderType,
            uploadStatus
        )
    }

    override suspend fun setCameraUploadsRecordGeneratedFingerprint(
        mediaId: Long,
        timestamp: Long,
        folderType: CameraUploadFolderType,
        generatedFingerprint: String,
    ) {
        val encryptedMediaId = encryptData(mediaId.toString())
        requireNotNull(encryptedMediaId)
        val encryptedTimestamp = encryptData(timestamp.toString())
        requireNotNull(encryptedTimestamp)
        val encryptedGeneratedFingerprint = encryptData(generatedFingerprint)
        requireNotNull(encryptedGeneratedFingerprint)

        cameraUploadsRecordDao.updateCameraUploadsRecordGeneratedFingerprint(
            encryptedMediaId,
            encryptedTimestamp,
            folderType,
            encryptedGeneratedFingerprint
        )
    }

    override suspend fun deleteCameraUploadsRecords(folderTypes: List<CameraUploadFolderType>) =
        cameraUploadsRecordDao.deleteCameraUploadsRecordsByFolderType(folderTypes)

    override suspend fun deleteBackupById(backupId: Long) {
        encryptData(backupId.toString())?.let {
            backupDao.deleteBackupByBackupId(it)
        }
    }

    override suspend fun setBackupAsOutdated(backupId: Long) {
        encryptData(backupId.toString())?.let { encryptedBackupId ->
            encryptData("true")?.let { encryptedTrue ->
                backupDao.updateBackupAsOutdated(
                    encryptedBackupId = encryptedBackupId,
                    encryptedIsOutdated = encryptedTrue
                )
            }
        }
    }

    override suspend fun saveBackup(backup: Backup) {
        backupEntityMapper(backup)?.let {
            backupDao.insertOrUpdateBackup(it)
        }
    }

    override suspend fun getCuBackUp(): Backup? {
        return encryptData("false")?.let { encryptedFalse ->
            backupDao.getBackupByType(
                backupType = backupInfoTypeIntMapper(BackupInfoType.CAMERA_UPLOADS),
                encryptedIsOutdated = encryptedFalse
            ).lastOrNull()
        }?.let { backupModelMapper(it) }
    }

    override suspend fun getMuBackUp(): Backup? {
        return encryptData("false")?.let { encryptedFalse ->
            backupDao.getBackupByType(
                backupInfoTypeIntMapper(BackupInfoType.MEDIA_UPLOADS),
                encryptedFalse
            ).lastOrNull()
        }?.let { backupModelMapper(it) }
    }

    override suspend fun getCuBackUpId(): Long? {
        return encryptData("false")?.let { encryptedFalse ->
            backupDao.getBackupIdByType(
                backupInfoTypeIntMapper(BackupInfoType.CAMERA_UPLOADS),
                encryptedFalse
            ).lastOrNull()
        }?.let { decryptData(it) }?.toLong()
    }

    override suspend fun getMuBackUpId(): Long? {
        return encryptData("false")?.let { encryptedFalse ->
            backupDao.getBackupIdByType(
                backupInfoTypeIntMapper(BackupInfoType.MEDIA_UPLOADS),
                encryptedFalse
            ).lastOrNull()
        }?.let { decryptData(it) }?.toLong()
    }

    override suspend fun getBackupById(id: Long): Backup? {
        return encryptData(id.toString())?.let { encryptedBackupId ->
            backupDao.getBackupById(encryptedBackupId)
        }?.let { backupModelMapper(it) }
    }

    override suspend fun updateBackup(backup: Backup) {
        backupEntityMapper(backup)?.let {
            backupDao.insertOrUpdateBackup(it)
        }
    }

    override suspend fun deleteAllBackups() {
        backupDao.deleteAllBackups()
    }

    override suspend fun isOfflineInformationAvailable(nodeHandle: Long) =
        offlineDao.getOfflineByHandle("${encryptData("$nodeHandle")}") != null

    override suspend fun getOfflineInformation(nodeHandle: Long) =
        offlineDao.getOfflineByHandle("${encryptData("$nodeHandle")}")?.let {
            offlineModelMapper(it)
        }

    override suspend fun saveOfflineInformation(offline: Offline) =
        offlineEntityMapper(offline).let {
            offlineDao.insertOrUpdateOffline(it)
        }

    override suspend fun clearOffline() = offlineDao.deleteAllOffline()

    override fun monitorOfflineUpdates() = offlineDao.monitorOffline()
        .map { it.map { offlineEntity -> offlineModelMapper(offlineEntity) } }


    override suspend fun getAllOfflineInfo() =
        offlineDao.getOfflineFiles()?.map { offlineModelMapper(it) }

    override suspend fun removeOfflineInformation(nodeId: String) {
        encryptData(nodeId)?.let {
            offlineDao.deleteOfflineByHandle(it)
        }
    }

    override suspend fun getOfflineInfoByParentId(parentId: Int): List<Offline>? =
        offlineDao.getOfflineByParentId(parentId)?.map {
            offlineModelMapper(it)
        }

    override suspend fun getOfflineLineById(id: Int): Offline? =
        offlineDao.getOfflineById(id)?.let {
            offlineModelMapper(it)
        }

    override suspend fun removeOfflineInformationById(id: Int) {
        offlineDao.deleteOfflineById(id)
    }

    private suspend fun deleteCompletedTransferBatch(ids: List<Int>) {
        ids.chunked(50).forEach {
            completedTransferDao.deleteCompletedTransferByIds(it)
        }
    }

    companion object {
        private const val MAX_COMPLETED_TRANSFER_ROWS = 100
    }
}
