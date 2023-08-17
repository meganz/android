package mega.privacy.android.domain.usecase.camerauploads

import kotlinx.coroutines.yield
import mega.privacy.android.domain.entity.SyncRecord
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.FileSystemRepository
import mega.privacy.android.domain.usecase.DeleteSyncRecordByLocalPath
import mega.privacy.android.domain.usecase.FileNameExists
import mega.privacy.android.domain.usecase.GetDeviceCurrentNanoTimeUseCase
import mega.privacy.android.domain.usecase.GetSyncRecordByFingerprint
import mega.privacy.android.domain.usecase.node.GetChildNodeUseCase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/**
 * Use case to save [SyncRecord] list to database
 *
 *
 * @property getSyncRecordByFingerprint [GetSyncRecordByFingerprint]
 * @property deleteSyncRecordByLocalPath [DeleteSyncRecordByLocalPath]
 * @property areUploadFileNamesKeptUseCase [AreUploadFileNamesKeptUseCase]
 * @property getChildNodeUseCase [GetChildNodeUseCase]
 * @property fileNameExists [FileNameExists]
 * @property getDeviceCurrentNanoTimeUseCase [GetDeviceCurrentNanoTimeUseCase]
 */
class SaveSyncRecordsToDBUseCase @Inject constructor(
    private val getSyncRecordByFingerprint: GetSyncRecordByFingerprint,
    private val deleteSyncRecordByLocalPath: DeleteSyncRecordByLocalPath,
    private val areUploadFileNamesKeptUseCase: AreUploadFileNamesKeptUseCase,
    private val getChildNodeUseCase: GetChildNodeUseCase,
    private val fileNameExists: FileNameExists,
    private val saveSyncRecordsUseCase: SaveSyncRecordsUseCase,
    private val getDeviceCurrentNanoTimeUseCase: GetDeviceCurrentNanoTimeUseCase,
    private val fileSystemRepository: FileSystemRepository,
) {


    /**
     * invoke
     * @param list of [SyncRecord]
     * @param uploadNodeId [NodeId]
     * @param rootPath
     */
    suspend operator fun invoke(
        list: List<SyncRecord>,
        uploadNodeId: NodeId,
        rootPath: String,
    ) {
        val finalList = arrayListOf<SyncRecord>()
        val keepName = areUploadFileNamesKeptUseCase()

        list
            .filter { record ->
                // Delete the syncRecord from DB if files associated does not exist and syncRecord needs to be uploaded
                // Skip the file in that case
                record.localPath.let {
                    if (!record.isCopyOnly && !fileSystemRepository.doesFileExist(it)) {
                        deleteSyncRecordByLocalPath(it, record.isSecondary)
                        return@filter false
                    }
                }

                // Delete sync record in DB if syncRecord with same fingerprint is more recent than stored syncRecord
                // Skip the file if syncRecord already exists in DB with same timestamp
                getSyncRecordByFingerprint(
                    record.originFingerprint,
                    record.isSecondary,
                    record.isCopyOnly
                )?.let { exist ->
                    if (exist.timestamp < record.timestamp) {
                        deleteSyncRecordByLocalPath(exist.localPath, exist.isSecondary)
                    } else {
                        return@filter false
                    }
                }
                return@filter true
            }
            .forEach { record ->
                yield()
                val isSecondary = record.isSecondary
                var photoIndex = 0

                val initFileName = getFileName(record, keepName)
                var fileName = initFileName
                while (fileNameAlreadyExists(fileName, uploadNodeId, isSecondary, finalList)) {
                    fileName = getFileNameWithIndex(initFileName, photoIndex++)
                }

                val extension = fileName.substringAfterLast('.', "")

                record.copy(
                    fileName = fileName,
                    newPath = "$rootPath${getDeviceCurrentNanoTimeUseCase()}.$extension"
                ).let {
                    finalList.add(it)
                }
            }
        saveSyncRecordsUseCase(finalList.toList())
    }

    /**
     * Get the file name depending of the user setting to keep the original name
     *
     * @param record
     * @param keepName true if the user setting is to keep original name
     * @return the file name, null if cannot be generated
     *         If the name is kept, the name will be the same as the original name
     *         If the name is not kept, the name will be equal of `yyyy-MM-dd HH.mm.ss`,
     *         corresponding to the time the file was last modified
     */
    private fun getFileName(record: SyncRecord, keepName: Boolean): String {
        return if (keepName) {
            record.fileName
        } else {
            val sdf = SimpleDateFormat(DATE_AND_TIME_PATTERN, Locale.getDefault())
            val newFileName = sdf.format(Date(record.timestamp))
            val extension = record.fileName.substringAfterLast(".", "")
            "$newFileName.$extension"
        }
    }

    /**
     * Get the file name with an index in suffix, in case the name already exists in the syncRecords
     *
     * @param fileName
     * @param index to append to the fileName
     * @return the file name with the [index] in suffix.
     *         Result format will be fileName_index.extension
     */
    private fun getFileNameWithIndex(fileName: String, index: Int): String {
        if (index == 0)
            return fileName

        val name = fileName.substringBeforeLast(".", "")
        val extension = fileName.substringAfterLast(".", "")
        return "${name}_$index.$extension"
    }

    /**
     * Check if the [fileName] is already used or will used for one of the node
     *
     * @param fileName
     * @param parentNodeId
     * @param isSecondary
     * @param syncRecordList
     * @return true if the [fileName] is already used
     */
    private suspend fun fileNameAlreadyExists(
        fileName: String,
        parentNodeId: NodeId,
        isSecondary: Boolean,
        syncRecordList: List<SyncRecord>
    ): Boolean =
        fileNameExistsInCloud(fileName, parentNodeId)
                || fileNameExistsInDatabase(fileName, isSecondary)
                || fileNameExistsInList(fileName, syncRecordList, isSecondary)

    /**
     * Check if the [fileName] is already used for one of the node in the target folder on the Cloud drive
     *
     * @param fileName
     * @param parentNodeId
     * @return true if the [fileName] is already used
     */
    private suspend fun fileNameExistsInCloud(fileName: String, parentNodeId: NodeId?): Boolean =
        getChildNodeUseCase(parentNodeId, fileName) != null

    /**
     * Check if the [fileName] is already used for one of the future node in the database
     *
     * @param fileName
     * @param isSecondary
     * @return true if the [fileName] is already used
     */
    private suspend fun fileNameExistsInDatabase(fileName: String, isSecondary: Boolean): Boolean =
        fileNameExists(fileName, isSecondary)

    /**
     * Check if the [fileName] is already used for one of the future node in the list that is being processed
     *
     * @param fileName
     * @param syncRecordList
     * @return true if the [fileName] is already used
     */
    private fun fileNameExistsInList(
        fileName: String,
        syncRecordList: List<SyncRecord>,
        secondary: Boolean
    ): Boolean =
        syncRecordList.any { it.isSecondary == secondary && it.fileName == fileName }

    private companion object {
        const val DATE_AND_TIME_PATTERN = "yyyy-MM-dd HH.mm.ss"
    }

}
