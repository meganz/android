package mega.privacy.android.data.facade

import dagger.Lazy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import mega.privacy.android.data.cryptography.DecryptData
import mega.privacy.android.data.cryptography.EncryptData
import mega.privacy.android.data.database.dao.ActiveTransferDao
import mega.privacy.android.data.database.dao.ActiveTransferGroupDao
import mega.privacy.android.data.database.dao.BackupDao
import mega.privacy.android.data.database.dao.CameraUploadsRecordDao
import mega.privacy.android.data.database.dao.ChatPendingChangesDao
import mega.privacy.android.data.database.dao.CompletedTransferDao
import mega.privacy.android.data.database.dao.ContactDao
import mega.privacy.android.data.database.dao.LastPageViewedInPdfDao
import mega.privacy.android.data.database.dao.MediaPlaybackInfoDao
import mega.privacy.android.data.database.dao.OfflineDao
import mega.privacy.android.data.database.dao.PendingTransferDao
import mega.privacy.android.data.database.dao.VideoRecentlyWatchedDao
import mega.privacy.android.data.gateway.MegaLocalRoomGateway
import mega.privacy.android.data.mapper.MediaPlaybackInfoEntityMapper
import mega.privacy.android.data.mapper.MediaPlaybackInfoMapper
import mega.privacy.android.data.mapper.backup.BackupEntityMapper
import mega.privacy.android.data.mapper.backup.BackupInfoTypeIntMapper
import mega.privacy.android.data.mapper.backup.BackupModelMapper
import mega.privacy.android.data.mapper.camerauploads.CameraUploadsRecordEntityMapper
import mega.privacy.android.data.mapper.camerauploads.CameraUploadsRecordModelMapper
import mega.privacy.android.data.mapper.chat.ChatRoomPendingChangesEntityMapper
import mega.privacy.android.data.mapper.chat.ChatRoomPendingChangesModelMapper
import mega.privacy.android.data.mapper.contact.ContactEntityMapper
import mega.privacy.android.data.mapper.contact.ContactModelMapper
import mega.privacy.android.data.mapper.offline.OfflineEntityMapper
import mega.privacy.android.data.mapper.offline.OfflineModelMapper
import mega.privacy.android.data.mapper.pdf.LastPageViewedInPdfEntityMapper
import mega.privacy.android.data.mapper.pdf.LastPageViewedInPdfModelMapper
import mega.privacy.android.data.mapper.transfer.TransferStateIntMapper
import mega.privacy.android.data.mapper.transfer.active.ActiveTransferEntityMapper
import mega.privacy.android.data.mapper.transfer.active.ActiveTransferGroupEntityMapper
import mega.privacy.android.data.mapper.transfer.completed.CompletedTransferEntityMapper
import mega.privacy.android.data.mapper.transfer.completed.CompletedTransferLegacyModelMapper
import mega.privacy.android.data.mapper.transfer.completed.CompletedTransferModelMapper
import mega.privacy.android.data.mapper.transfer.pending.InsertPendingTransferRequestMapper
import mega.privacy.android.data.mapper.transfer.pending.PendingTransferModelMapper
import mega.privacy.android.data.mapper.videosection.VideoRecentlyWatchedEntityMapper
import mega.privacy.android.data.mapper.videosection.VideoRecentlyWatchedItemMapper
import mega.privacy.android.data.model.VideoRecentlyWatchedItem
import mega.privacy.android.domain.entity.CameraUploadsRecordType
import mega.privacy.android.domain.entity.Contact
import mega.privacy.android.domain.entity.Offline
import mega.privacy.android.domain.entity.backup.Backup
import mega.privacy.android.domain.entity.backup.BackupInfoType
import mega.privacy.android.domain.entity.camerauploads.CameraUploadFolderType
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsRecord
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsRecordUploadStatus
import mega.privacy.android.domain.entity.chat.ChatPendingChanges
import mega.privacy.android.domain.entity.mediaplayer.MediaPlaybackInfo
import mega.privacy.android.domain.entity.mediaplayer.MediaType
import mega.privacy.android.domain.entity.pdf.LastPageViewedInPdf
import mega.privacy.android.domain.entity.transfer.ActiveTransfer
import mega.privacy.android.domain.entity.transfer.ActiveTransferActionGroup
import mega.privacy.android.domain.entity.transfer.CompletedTransfer
import mega.privacy.android.domain.entity.transfer.TransferState
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.entity.transfer.pending.InsertPendingTransferRequest
import mega.privacy.android.domain.entity.transfer.pending.PendingTransfer
import mega.privacy.android.domain.entity.transfer.pending.PendingTransferState
import mega.privacy.android.domain.entity.transfer.pending.UpdateAlreadyTransferredFilesCount
import mega.privacy.android.domain.entity.transfer.pending.UpdatePendingTransferRequest
import mega.privacy.android.domain.entity.transfer.pending.UpdatePendingTransferState
import mega.privacy.android.domain.entity.transfer.pending.UpdateScanningFoldersData
import javax.inject.Inject

internal class MegaLocalRoomFacade @Inject constructor(
    private val contactDao: Lazy<ContactDao>,
    private val contactEntityMapper: ContactEntityMapper,
    private val contactModelMapper: ContactModelMapper,
    private val completedTransferDao: Lazy<CompletedTransferDao>,
    private val activeTransferDao: Lazy<ActiveTransferDao>,
    private val completedTransferModelMapper: CompletedTransferModelMapper,
    private val completedTransferEntityMapper: CompletedTransferEntityMapper,
    private val completedTransferLegacyModelMapper: CompletedTransferLegacyModelMapper,
    private val activeTransferEntityMapper: ActiveTransferEntityMapper,
    private val backupDao: Lazy<BackupDao>,
    private val backupEntityMapper: BackupEntityMapper,
    private val backupModelMapper: BackupModelMapper,
    private val backupInfoTypeIntMapper: BackupInfoTypeIntMapper,
    private val cameraUploadsRecordDao: Lazy<CameraUploadsRecordDao>,
    private val cameraUploadsRecordEntityMapper: CameraUploadsRecordEntityMapper,
    private val cameraUploadsRecordModelMapper: CameraUploadsRecordModelMapper,
    private val encryptData: EncryptData,
    private val decryptData: DecryptData,
    private val offlineDao: Lazy<OfflineDao>,
    private val offlineModelMapper: OfflineModelMapper,
    private val offlineEntityMapper: OfflineEntityMapper,
    private val chatPendingChangesDao: Lazy<ChatPendingChangesDao>,
    private val chatRoomPendingChangesEntityMapper: ChatRoomPendingChangesEntityMapper,
    private val chatRoomPendingChangesModelMapper: ChatRoomPendingChangesModelMapper,
    private val videoRecentlyWatchedDao: Lazy<VideoRecentlyWatchedDao>,
    private val videoRecentlyWatchedEntityMapper: VideoRecentlyWatchedEntityMapper,
    private val videoRecentlyWatchedItemMapper: VideoRecentlyWatchedItemMapper,
    private val pendingTransferDao: Lazy<PendingTransferDao>,
    private val pendingTransferModelMapper: PendingTransferModelMapper,
    private val insertPendingTransferRequestMapper: InsertPendingTransferRequestMapper,
    private val activeTransferGroupDao: Lazy<ActiveTransferGroupDao>,
    private val activeTransferGroupEntityMapper: ActiveTransferGroupEntityMapper,
    private val lastPageViewedInPdfDao: Lazy<LastPageViewedInPdfDao>,
    private val lastPageViewedInPdfEntityMapper: LastPageViewedInPdfEntityMapper,
    private val lastPageViewedInPdfModelMapper: LastPageViewedInPdfModelMapper,
    private val mediaPlaybackInfoDao: Lazy<MediaPlaybackInfoDao>,
    private val mediaPlaybackInfoEntityMapper: MediaPlaybackInfoEntityMapper,
    private val mediaPlaybackInfoMapper: MediaPlaybackInfoMapper,
    private val transferStateIntMapper: TransferStateIntMapper,
) : MegaLocalRoomGateway {
    override suspend fun insertContact(contact: Contact) {
        contactDao.get().insertOrUpdateContact(contactEntityMapper(contact))
    }

    override suspend fun updateContactNameByEmail(firstName: String?, email: String?) {
        if (email.isNullOrBlank()) return
        contactDao.get().getContactByEmail(encryptData(email))?.let { entity ->
            contactDao.get().insertOrUpdateContact(entity.copy(firstName = encryptData(firstName)))
        }
    }

    override suspend fun updateContactLastNameByEmail(lastName: String?, email: String?) {
        if (email.isNullOrBlank()) return
        contactDao.get().getContactByEmail(encryptData(email))?.let { entity ->
            contactDao.get().insertOrUpdateContact(entity.copy(lastName = encryptData(lastName)))
        }
    }

    override suspend fun updateContactMailByHandle(handle: Long, email: String?) {
        contactDao.get().getContactByHandle(encryptData(handle.toString()))?.let { entity ->
            contactDao.get().insertOrUpdateContact(entity.copy(mail = encryptData(email)))
        }
    }

    override suspend fun updateContactFistNameByHandle(handle: Long, firstName: String?) {
        contactDao.get().getContactByHandle(encryptData(handle.toString()))?.let { entity ->
            contactDao.get().insertOrUpdateContact(entity.copy(firstName = encryptData(firstName)))
        }
    }

    override suspend fun updateContactLastNameByHandle(handle: Long, lastName: String?) {
        contactDao.get().getContactByHandle(encryptData(handle.toString()))?.let { entity ->
            contactDao.get().insertOrUpdateContact(entity.copy(lastName = encryptData(lastName)))
        }
    }

    override suspend fun updateContactNicknameByHandle(handle: Long, nickname: String?) {
        contactDao.get().getContactByHandle(encryptData(handle.toString()))?.let { entity ->
            contactDao.get().insertOrUpdateContact(entity.copy(nickName = encryptData(nickname)))
        }
    }

    override suspend fun getContactByHandle(handle: Long): Contact? {
        val encryptedHandle = encryptData(handle.toString())
        return contactDao.get().getContactByHandle(encryptedHandle)?.let { entity ->
            contactModelMapper(entity)
        }
    }

    override fun monitorContactByHandle(handle: Long): Flow<Contact> =
        flow {
            val encryptedHandle = encryptData(handle.toString())
            emitAll(contactDao.get().monitorContactByHandle(encryptedHandle))
        }.map { contactModelMapper(it) }

    override suspend fun getContactByEmail(email: String?): Contact? =
        contactDao.get().getContactByEmail(encryptData(email))?.let { contactModelMapper(it) }


    override fun monitorContactByEmail(email: String): Flow<Contact?> =
        flow {
            val encryptedEmail = encryptData(email)
            encryptedEmail?.let {
                emitAll(
                    contactDao.get().monitorContactByEmail(it)
                )
            }
        }.map { it?.let { entity -> contactModelMapper(entity) } }

    override suspend fun deleteAllContacts() = contactDao.get().deleteAllContact()

    override suspend fun getContactCount() = contactDao.get().getContactCount()

    override suspend fun getAllContacts(): List<Contact> {
        val entities = contactDao.get().getAllContact().first()
        return entities.map { contactModelMapper(it) }
    }

    override fun getCompletedTransfers(size: Int?) =
        completedTransferDao.get().getAllCompletedTransfers()
            .map { list ->
                list.map { completedTransferModelMapper(it) }
                    .toMutableList()
                    .apply { sortWith(compareByDescending { it.timestamp }) }
                    .let { if (size != null) it.take(size) else it }
            }

    override fun getCompletedTransfersByStateWithLimit(
        limit: Int,
        vararg transferStates: TransferState,
    ) =
        completedTransferDao.get().getCompletedTransfersByStateWithLimit(
            transferStates.map(transferStateIntMapper::invoke),
            limit
        ).map { it.map(completedTransferModelMapper::invoke) }

    override suspend fun addCompletedTransfer(transfer: CompletedTransfer) {
        completedTransferDao.get()
            .insertOrUpdateCompletedTransfer(completedTransferEntityMapper(transfer))
    }

    override suspend fun addCompletedTransfers(transfers: List<CompletedTransfer>) {
        transfers.map { completedTransferEntityMapper(it) }.let { mappedTransfers ->
            completedTransferDao.get().insertOrUpdateCompletedTransfers(
                entities = mappedTransfers,
                chunkSize = MAX_INSERT_LIST_SIZE,
            )
        }
    }

    override suspend fun getCompletedTransfersCount() =
        completedTransferDao.get().getCompletedTransfersCount()

    override suspend fun deleteAllCompletedTransfers() =
        completedTransferDao.get().deleteAllCompletedTransfers()

    override suspend fun getCompletedTransfersByState(states: List<Int>): List<CompletedTransfer> {
        return completedTransferDao.get().getCompletedTransfersByState(states)
            .map { entity -> completedTransferModelMapper(entity) }
    }

    override suspend fun deleteCompletedTransfersByState(states: List<Int>): List<CompletedTransfer> {
        val entities = completedTransferDao.get().getCompletedTransfersByState(states)
        deleteCompletedTransferBatch(entities.mapNotNull { it.id })
        return entities.map { entity -> completedTransferModelMapper(entity) }
    }

    override suspend fun deleteCompletedTransfersById(ids: List<Int>) {
        deleteCompletedTransferBatch(ids)
    }

    override suspend fun deleteCompletedTransfer(completedTransfer: CompletedTransfer) {
        completedTransferDao.get().deleteCompletedTransferByIds(
            listOf(completedTransfer.id ?: return)
        )
    }

    override suspend fun deleteOldestCompletedTransfers() {
        val count = completedTransferDao.get().getCompletedTransfersCount()
        if (count > MAX_COMPLETED_TRANSFER_ROWS) {
            listOf(
                TransferState.STATE_COMPLETED,
                TransferState.STATE_CANCELLED,
                TransferState.STATE_FAILED
            ).forEach {
                completedTransferDao.get()
                    .deleteOldCompletedTransfersByState(
                        transferStateIntMapper(it),
                        MAX_COMPLETED_TRANSFER_ROWS
                    )
            }
        }
    }

    override suspend fun migrateLegacyCompletedTransfers() {
        completedTransferDao.get().getAllLegacyCompletedTransfers()
            .takeIf { it.isNotEmpty() }
            ?.let { legacyEntities ->
                val firstHundred = legacyEntities
                    .sortedWith(compareByDescending { it.timestamp })
                    .take(100)
                addCompletedTransfers(firstHundred.map { completedTransferLegacyModelMapper(it) })
                completedTransferDao.get().deleteAllLegacyCompletedTransfers()
            }
    }

    override suspend fun getActiveTransferByUniqueId(uniqueId: Long) =
        activeTransferDao.get().getActiveTransferByUniqueId(uniqueId)

    override suspend fun getActiveTransferByTag(tag: Int) =
        activeTransferDao.get().getActiveTransferByTag(tag)

    override fun getActiveTransfersByType(transferType: TransferType) =
        activeTransferDao.get().getActiveTransfersByType(transferType)


    override suspend fun getCurrentActiveTransfersByType(transferType: TransferType) =
        activeTransferDao.get().getCurrentActiveTransfersByType(transferType)

    override suspend fun getCurrentActiveTransfers(): List<ActiveTransfer> =
        activeTransferDao.get().getCurrentActiveTransfers()

    override suspend fun insertOrUpdateActiveTransfer(activeTransfer: ActiveTransfer) =
        activeTransferDao.get()
            .insertOrUpdateActiveTransfer(activeTransferEntityMapper(activeTransfer))

    override suspend fun insertOrUpdateActiveTransfers(activeTransfers: List<ActiveTransfer>) =
        activeTransfers.map { activeTransferEntityMapper(it) }.let { mappedActiveTransfers ->
            activeTransferDao.get().insertOrUpdateActiveTransfers(mappedActiveTransfers)
        }

    override suspend fun deleteAllActiveTransfersByType(transferType: TransferType) =
        activeTransferDao.get().deleteAllActiveTransfersByType(transferType)

    override suspend fun deleteAllActiveTransfers() =
        activeTransferDao.get().deleteAllActiveTransfers()

    override suspend fun setActiveTransfersAsFinishedByUniqueId(
        uniqueIds: List<Long>,
        cancelled: Boolean,
    ) = activeTransferDao.get().setActiveTransfersAsFinishedByUniqueId(uniqueIds, cancelled)

    override suspend fun insertActiveTransferGroup(activeTransferActionGroup: ActiveTransferActionGroup) =
        activeTransferGroupDao.get()
            .insertActiveTransferGroup(activeTransferGroupEntityMapper(activeTransferActionGroup))

    override suspend fun getActiveTransferGroup(groupId: Int): ActiveTransferActionGroup? =
        activeTransferGroupDao.get().getActiveTransferGroupById(groupId)

    override suspend fun deleteActiveTransferGroup(groupId: Int) {
        activeTransferGroupDao.get().deleteActiveTransfersGroupById(groupId)
    }

    override suspend fun insertOrUpdateCameraUploadsRecords(records: List<CameraUploadsRecord>) =
        cameraUploadsRecordDao.get().insertOrUpdateCameraUploadsRecords(
            records.map { cameraUploadsRecordEntityMapper(it) }
        )

    override suspend fun getAllCameraUploadsRecords(): List<CameraUploadsRecord> =
        cameraUploadsRecordDao.get().getAllCameraUploadsRecords().map {
            cameraUploadsRecordModelMapper(it)
        }

    override suspend fun getCameraUploadsRecordsBy(
        uploadStatus: List<CameraUploadsRecordUploadStatus>,
        types: List<CameraUploadsRecordType>,
        folderTypes: List<CameraUploadFolderType>,
    ): List<CameraUploadsRecord> =
        cameraUploadsRecordDao.get().getCameraUploadsRecordsBy(
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
        cameraUploadsRecordDao.get().updateCameraUploadsRecordUploadStatus(
            mediaId,
            timestamp,
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
        cameraUploadsRecordDao.get().updateCameraUploadsRecordGeneratedFingerprint(
            mediaId,
            timestamp,
            folderType,
            generatedFingerprint
        )
    }

    override suspend fun deleteCameraUploadsRecords(folderTypes: List<CameraUploadFolderType>) =
        cameraUploadsRecordDao.get().deleteCameraUploadsRecordsByFolderType(folderTypes)

    override suspend fun deleteBackupById(backupId: Long) {
        encryptData(backupId.toString())?.let {
            backupDao.get().deleteBackupByBackupId(it)
        }
    }

    override suspend fun setBackupAsOutdated(backupId: Long) {
        encryptData(backupId.toString())?.let { encryptedBackupId ->
            encryptData("true")?.let { encryptedTrue ->
                backupDao.get().updateBackupAsOutdated(
                    encryptedBackupId = encryptedBackupId,
                    encryptedIsOutdated = encryptedTrue
                )
            }
        }
    }

    override suspend fun saveBackup(backup: Backup) {
        backupEntityMapper(backup)?.let {
            backupDao.get().insertOrUpdateBackup(it)
        }
    }

    override suspend fun getCuBackUp(): Backup? {
        return encryptData("false")?.let { encryptedFalse ->
            backupDao.get().getBackupByType(
                backupType = backupInfoTypeIntMapper(BackupInfoType.CAMERA_UPLOADS),
                encryptedIsOutdated = encryptedFalse
            ).lastOrNull()
        }?.let { backupModelMapper(it) }
    }

    override suspend fun getMuBackUp(): Backup? {
        return encryptData("false")?.let { encryptedFalse ->
            backupDao.get().getBackupByType(
                backupInfoTypeIntMapper(BackupInfoType.MEDIA_UPLOADS),
                encryptedFalse
            ).lastOrNull()
        }?.let { backupModelMapper(it) }
    }

    override suspend fun getCuBackUpId(): Long? {
        return encryptData("false")?.let { encryptedFalse ->
            backupDao.get().getBackupIdByType(
                backupInfoTypeIntMapper(BackupInfoType.CAMERA_UPLOADS),
                encryptedFalse
            ).lastOrNull()
        }?.let { decryptData(it) }?.toLong()
    }

    override suspend fun getMuBackUpId(): Long? {
        return encryptData("false")?.let { encryptedFalse ->
            backupDao.get().getBackupIdByType(
                backupInfoTypeIntMapper(BackupInfoType.MEDIA_UPLOADS),
                encryptedFalse
            ).lastOrNull()
        }?.let { decryptData(it) }?.toLong()
    }

    override suspend fun getBackupById(id: Long): Backup? {
        return encryptData(id.toString())?.let { encryptedBackupId ->
            backupDao.get().getBackupById(encryptedBackupId)
        }?.let { backupModelMapper(it) }
    }

    override suspend fun updateBackup(backup: Backup) {
        backupEntityMapper(backup)?.let {
            backupDao.get().insertOrUpdateBackup(it)
        }
    }

    override suspend fun deleteAllBackups() {
        backupDao.get().deleteAllBackups()
    }

    override suspend fun isOfflineInformationAvailable(nodeHandle: Long) =
        offlineDao.get().getOfflineByHandle("${encryptData("$nodeHandle")}") != null

    override suspend fun getOfflineInformation(nodeHandle: Long) =
        offlineDao.get().getOfflineByHandle("${encryptData("$nodeHandle")}")?.let {
            offlineModelMapper(it)
        }

    override suspend fun saveOfflineInformation(offline: Offline) =
        offlineEntityMapper(offline).let {
            offlineDao.get().insertOrUpdateOffline(it)
        }

    override suspend fun clearOffline() = offlineDao.get().deleteAllOffline()

    override fun monitorOfflineUpdates() = offlineDao.get().monitorOffline()
        .map { it.map { offlineEntity -> offlineModelMapper(offlineEntity) } }


    override suspend fun getAllOfflineInfo() =
        offlineDao.get().getOfflineFiles()?.map { offlineModelMapper(it) } ?: emptyList()

    override suspend fun removeOfflineInformation(nodeId: String) {
        encryptData(nodeId)?.let {
            offlineDao.get().deleteOfflineByHandle(it)
        }
    }

    override suspend fun getOfflineInfoByParentId(parentId: Int): List<Offline> =
        offlineDao.get().getOfflineByParentId(parentId)?.map {
            offlineModelMapper(it)
        } ?: emptyList()

    override suspend fun getOfflineLineById(id: Int): Offline? =
        offlineDao.get().getOfflineById(id)?.let {
            offlineModelMapper(it)
        }

    override suspend fun removeOfflineInformationById(id: Int) {
        offlineDao.get().deleteOfflineById(id)
    }

    override suspend fun removeOfflineInformationByIds(ids: List<Int>) {
        offlineDao.get().deleteOfflineByIds(ids)
    }

    private suspend fun deleteCompletedTransferBatch(ids: List<Int>) {
        completedTransferDao.get().deleteCompletedTransferByIds(
            ids,
            MAX_INSERT_LIST_SIZE
        )
    }

    override suspend fun setChatPendingChanges(chatPendingChanges: ChatPendingChanges) {
        chatPendingChangesDao.get().upsertChatPendingChanges(
            chatRoomPendingChangesEntityMapper(chatPendingChanges)
        )
    }

    override suspend fun getAllRecentlyWatchedVideos(): Flow<List<VideoRecentlyWatchedItem>> {
        videoRecentlyWatchedDao.get().deleteExcessVideos()
        return videoRecentlyWatchedDao.get().getAllRecentlyWatchedVideos().map { entities ->
            entities.map { entity ->
                videoRecentlyWatchedItemMapper(
                    entity.videoHandle,
                    entity.watchedTimestamp,
                    entity.collectionId,
                    entity.collectionTitle
                )
            }
        }
    }

    override suspend fun removeRecentlyWatchedVideo(handle: Long) =
        videoRecentlyWatchedDao.get().removeRecentlyWatchedVideo(handle)

    override suspend fun clearRecentlyWatchedVideos() =
        videoRecentlyWatchedDao.get().clearRecentlyWatchedVideos()

    override suspend fun saveRecentlyWatchedVideo(item: VideoRecentlyWatchedItem) {
        val entity = videoRecentlyWatchedEntityMapper(item)
        videoRecentlyWatchedDao.get().insertVideo(entity)
    }

    override suspend fun saveRecentlyWatchedVideos(items: List<VideoRecentlyWatchedItem>) {
        val entities = items.map { videoRecentlyWatchedEntityMapper(it) }
        videoRecentlyWatchedDao.get().insertOrUpdateRecentlyWatchedVideos(entities)
    }

    override fun monitorChatPendingChanges(chatId: Long): Flow<ChatPendingChanges?> =
        chatPendingChangesDao.get().getChatPendingChanges(chatId)
            .map { entity -> entity?.let { chatRoomPendingChangesModelMapper(it) } }

    override suspend fun insertPendingTransfers(pendingTransfers: List<InsertPendingTransferRequest>) {
        pendingTransferDao.get()
            .insertOrUpdatePendingTransfers(
                pendingTransfers.map { insertPendingTransferRequestMapper(it) },
                MAX_INSERT_LIST_SIZE
            )
    }

    override fun monitorPendingTransfersByType(transferType: TransferType) =
        pendingTransferDao.get().monitorPendingTransfersByType(transferType)
            .map { it.map { pendingTransferModelMapper(it) } }

    override suspend fun getPendingTransfersByType(transferType: TransferType) =
        pendingTransferDao.get().getPendingTransfersByType(transferType)
            .map { pendingTransferModelMapper(it) }

    override suspend fun getPendingTransfersByState(pendingTransferState: PendingTransferState) =
        pendingTransferDao.get().getPendingTransfersByState(pendingTransferState)
            .map { pendingTransferModelMapper(it) }

    override fun monitorPendingTransfersByTypeAndState(
        transferType: TransferType,
        pendingTransferState: PendingTransferState,
    ) = pendingTransferDao.get()
        .monitorPendingTransfersByTypeAndState(transferType, pendingTransferState)
        .map { it.map { pendingTransferModelMapper(it) } }

    override suspend fun getPendingTransfersByTypeAndState(
        transferType: TransferType,
        pendingTransferState: PendingTransferState,
    ) = pendingTransferDao.get()
        .getPendingTransfersByTypeAndState(transferType, pendingTransferState)
        .map { pendingTransferModelMapper(it) }

    override suspend fun getPendingTransfersByUniqueId(uniqueId: Long): PendingTransfer? =
        pendingTransferDao.get().getPendingTransferByUniqueId(uniqueId)
            ?.let { pendingTransferModelMapper(it) }

    override suspend fun updatePendingTransfers(vararg updatePendingTransferRequests: UpdatePendingTransferRequest) {
        updatePendingTransferRequests.singleOrNull()?.let { request ->
            when (request) {
                is UpdateAlreadyTransferredFilesCount -> pendingTransferDao.get().update(request)
                is UpdatePendingTransferState -> pendingTransferDao.get().update(request)
                is UpdateScanningFoldersData -> pendingTransferDao.get().update(request)
            }
        } ?: run {
            pendingTransferDao.get().updateMultiple(updatePendingTransferRequests.toList())
        }
    }

    override suspend fun deletePendingTransferByUniqueId(uniqueId: Long) {
        pendingTransferDao.get().deletePendingTransferByUniqueId(uniqueId)
    }

    override suspend fun deleteAllPendingTransfers() {
        pendingTransferDao.get().deleteAllPendingTransfers()
    }

    override suspend fun deleteCompletedTransfersByPath(path: String) = completedTransferDao.get()
        .deleteCompletedTransfersByPath(path)

    override suspend fun insertOrUpdateLastPageViewedInPdf(lastPageViewedInPdf: LastPageViewedInPdf) =
        lastPageViewedInPdfDao.get()
            .insertOrUpdateLastPageViewedInPdf(lastPageViewedInPdfEntityMapper(lastPageViewedInPdf))

    override suspend fun getLastPageViewedInPdfByHandle(handle: Long) =
        lastPageViewedInPdfDao.get().getLastPageViewedInPdfByHandle(handle)
            ?.let { lastPageViewedInPdfModelMapper(it) }

    override suspend fun deleteLastPageViewedInPdfByHandle(handle: Long) =
        lastPageViewedInPdfDao.get().deleteLastPageViewedInPdfByHandle(handle)

    override suspend fun deleteAllLastPageViewedInPdf() =
        lastPageViewedInPdfDao.get().deleteAllLastPageViewedInPdf()

    override suspend fun deletePlaybackInfo(handle: Long) {
        mediaPlaybackInfoDao.get().removePlaybackInfo(handle)
    }

    override suspend fun clearAllPlaybackInfos() {
        mediaPlaybackInfoDao.get().clearAllPlaybackInfos()
    }

    override suspend fun clearAudioPlaybackInfos() {
        mediaPlaybackInfoDao.get().clearPlaybackInfosByType(MediaType.Audio)
    }

    override suspend fun insertOrUpdatePlaybackInfo(info: MediaPlaybackInfo) {
        mediaPlaybackInfoDao.get().insertOrUpdatePlaybackInfo(
            mediaPlaybackInfoEntityMapper(info)
        )
    }

    override suspend fun insertOrUpdatePlaybackInfos(infos: List<MediaPlaybackInfo>) {
        mediaPlaybackInfoDao.get().insertOrUpdatePlaybackInfos(
            infos.map { info -> mediaPlaybackInfoEntityMapper(info) }
        )
    }

    override suspend fun getMediaPlaybackInfo(handle: Long): MediaPlaybackInfo? =
        mediaPlaybackInfoDao.get().getMediaPlaybackInfo(handle)?.let {
            mediaPlaybackInfoMapper(it)
        }

    override suspend fun monitorAllPlaybackInfos(): Flow<List<MediaPlaybackInfo>> =
        mediaPlaybackInfoDao.get().getAllPlaybackInfos().map { entities ->
            entities.map {
                mediaPlaybackInfoMapper(it)
            }
        }

    override suspend fun monitorAudioPlaybackInfos(): Flow<List<MediaPlaybackInfo>> =
        mediaPlaybackInfoDao.get().getAllPlaybackInfosByType(MediaType.Audio).map { entities ->
            entities.map {
                mediaPlaybackInfoMapper(it)
            }
        }

    companion object {
        internal const val MAX_COMPLETED_TRANSFER_ROWS = 100
        internal const val MAX_INSERT_LIST_SIZE = 200
    }
}
