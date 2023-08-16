package mega.privacy.android.domain.usecase.camerauploads

import kotlinx.coroutines.yield
import mega.privacy.android.domain.entity.SyncRecord
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.DeleteSyncRecordByLocalPath
import mega.privacy.android.domain.usecase.FileNameExists
import mega.privacy.android.domain.usecase.GetDeviceCurrentNanoTimeUseCase
import mega.privacy.android.domain.usecase.GetSyncRecordByFingerprint
import mega.privacy.android.domain.usecase.SaveSyncRecord
import mega.privacy.android.domain.usecase.node.GetChildNodeUseCase
import java.io.File
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/**
 * Use case to save [SaveSyncRecord] to database
 *
 *
 * @property getSyncRecordByFingerprint [GetSyncRecordByFingerprint]
 * @property deleteSyncRecordByLocalPath [DeleteSyncRecordByLocalPath]
 * @property areUploadFileNamesKeptUseCase [AreUploadFileNamesKeptUseCase]
 * @property getChildNodeUseCase [GetChildNodeUseCase]
 * @property fileNameExists [FileNameExists]
 * @property saveSyncRecord [SaveSyncRecord]
 * @property getDeviceCurrentNanoTimeUseCase [GetDeviceCurrentNanoTimeUseCase]
 */
class SaveSyncRecordsToDBUseCase @Inject constructor(
    private val getSyncRecordByFingerprint: GetSyncRecordByFingerprint,
    private val deleteSyncRecordByLocalPath: DeleteSyncRecordByLocalPath,
    private val areUploadFileNamesKeptUseCase: AreUploadFileNamesKeptUseCase,
    private val getChildNodeUseCase: GetChildNodeUseCase,
    private val fileNameExists: FileNameExists,
    private val saveSyncRecord: SaveSyncRecord,
    private val getDeviceCurrentNanoTimeUseCase: GetDeviceCurrentNanoTimeUseCase,
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
        rootPath: String?,
    ) {
        for (file in list) {
            run {
                yield()

                val exist = getSyncRecordByFingerprint(
                    file.originFingerprint,
                    file.isSecondary,
                    file.isCopyOnly
                )
                exist?.timestamp?.let { existTime ->
                    if (existTime < file.timestamp) {
                        deleteSyncRecordByLocalPath(exist.localPath, exist.isSecondary)
                    } else {
                        return@run
                    }
                }

                val isSecondary = file.isSecondary
                if (!file.isCopyOnly) {
                    val resFile = File(file.localPath)
                    if (!resFile.exists()) {
                        deleteSyncRecordByLocalPath(file.localPath, isSecondary)
                        return@run
                    }
                }

                var fileName: String?
                var inCloud: Boolean
                var inDatabase = false
                var photoIndex = 0
                if (areUploadFileNamesKeptUseCase()) {
                    //Keep the file names as device but need to handle same file name in different location
                    val tempFileName = file.fileName
                    do {
                        yield()
                        fileName = getNoneDuplicatedDeviceFileName(tempFileName, photoIndex)
                        photoIndex++
                        inCloud = getChildNodeUseCase(
                            uploadNodeId,
                            fileName
                        ) != null
                        fileName?.let {
                            inDatabase = fileNameExists(it, isSecondary)
                        }
                    } while (inCloud || inDatabase)
                } else {
                    do {
                        yield()
                        fileName = getPhotoSyncNameWithIndex(
                            getLastModifiedTime(file),
                            file.localPath,
                            photoIndex
                        )
                        photoIndex++
                        inCloud = getChildNodeUseCase(
                            uploadNodeId,
                            fileName
                        ) != null
                        inDatabase = fileNameExists(fileName, isSecondary)
                    } while (inCloud || inDatabase)
                }

                var extension = ""
                fileName?.let {
                    val splitName = it.split("\\.").toTypedArray()
                    if (splitName.isNotEmpty()) {
                        extension = splitName[splitName.size - 1]
                    }
                }
                file.copy(
                    fileName = fileName ?: "",
                    newPath = "$rootPath${getDeviceCurrentNanoTimeUseCase()}.$extension"
                ).let {
                    saveSyncRecord(it)
                }
            }
        }
    }

    private fun getLastModifiedTime(file: SyncRecord): Long {
        val source = File(file.localPath)
        return source.lastModified()
    }

    private fun getNoneDuplicatedDeviceFileName(fileName: String?, index: Int): String? {
        if (index == 0) {
            return fileName
        }
        var name = ""
        var extension = ""
        val pos = fileName?.lastIndexOf(".")
        if (pos != null && pos > 0) {
            name = fileName.substring(0, pos)
            extension = fileName.substring(pos)
        }
        return "${name}_$index$extension"
    }

    private fun getPhotoSyncNameWithIndex(
        timeStamp: Long,
        fileName: String?,
        photoIndex: Int,
    ): String {
        if (photoIndex == 0) {
            return getPhotoSyncName(timeStamp, fileName)
        }
        val sdf: DateFormat = SimpleDateFormat(
            DATE_AND_TIME_PATTERN,
            Locale.getDefault()
        )
        return sdf.format(Date(timeStamp)) + "_" + photoIndex + fileName?.substring(
            fileName.lastIndexOf(
                '.'
            )
        )
    }

    private fun getPhotoSyncName(timeStamp: Long, fileName: String?): String {
        val sdf: DateFormat = SimpleDateFormat(
            DATE_AND_TIME_PATTERN,
            Locale.getDefault()
        )
        return sdf.format(Date(timeStamp)) + fileName?.substring(fileName.lastIndexOf('.'))
    }

    private companion object {
        const val DATE_AND_TIME_PATTERN = "yyyy-MM-dd HH.mm.ss"
    }

}
