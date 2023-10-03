package mega.privacy.android.domain.usecase.camerauploads

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import mega.privacy.android.domain.entity.camerauploads.CameraUploadFolderType
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsRecord
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.usecase.node.GetChildNodeUseCase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/**
 * Rename the files if needed.
 * The file will be renamed if the user has not set the option to keep the file name.
 * A suffix will be added if the name already exists in the cloud
 */
class RenameCameraUploadsRecordsUseCase @Inject constructor(
    private val getChildNodeUseCase: GetChildNodeUseCase,
    private val areUploadFileNamesKeptUseCase: AreUploadFileNamesKeptUseCase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {

    companion object {
        private const val DATE_AND_TIME_PATTERN = "yyyy-MM-dd HH.mm.ss"
    }

    /**
     * Invoke
     *
     * @param recordList the list of files renamed
     * @param primaryUploadNodeId primary cloud folder
     * @param secondaryUploadNodeId secondary cloud folder
     */
    suspend operator fun invoke(
        recordList: List<CameraUploadsRecord>,
        primaryUploadNodeId: NodeId,
        secondaryUploadNodeId: NodeId,
    ): List<CameraUploadsRecord> = withContext(ioDispatcher) {
        val keepName = areUploadFileNamesKeptUseCase()
        val renamedRecordList = arrayListOf<CameraUploadsRecord>()
        recordList.forEach { record ->
            ensureActive()
            val parentNodeId = when (record.folderType) {
                CameraUploadFolderType.Primary -> primaryUploadNodeId
                CameraUploadFolderType.Secondary -> secondaryUploadNodeId
            }

            val originalFileName = getFileName(record, keepName)
            var fileName = originalFileName
            var photoIndex = 0
            while (
                fileNameAlreadyExists(fileName, parentNodeId, record.folderType, renamedRecordList)
            ) {
                ensureActive()
                fileName = getFileNameWithIndex(originalFileName, photoIndex++)
            }

            renamedRecordList.add(record.copy(fileName = fileName))
        }
        return@withContext renamedRecordList
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
    private fun getFileName(record: CameraUploadsRecord, keepName: Boolean): String {
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
     * Get the file name with an index in suffix, in case the name is already used
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
     * Check if the [fileName] is already used or will be used for one of the node
     *
     * @param fileName
     * @param parentNodeId
     * @param folderType
     * @param recordList
     * @return true if the [fileName] is already used
     */
    private suspend fun fileNameAlreadyExists(
        fileName: String,
        parentNodeId: NodeId,
        folderType: CameraUploadFolderType,
        recordList: List<CameraUploadsRecord>
    ): Boolean =
        fileNameExistsInCloud(fileName, parentNodeId)
                || fileNameExistsInList(fileName, recordList, folderType)

    /**
     * Check if the [fileName] is already used for one of the node in the target folder on the Cloud drive
     *
     * @param fileName
     * @param parentNodeId
     * @return true if the [fileName] is already used in the cloud folder
     */
    private suspend fun fileNameExistsInCloud(fileName: String, parentNodeId: NodeId): Boolean =
        getChildNodeUseCase(parentNodeId, fileName) != null

    /**
     * Check if the [fileName] is already used for one of the future node in the list that is being processed
     *
     * @param fileName
     * @param recordList
     * @param folderType
     * @return true if the [fileName] is already used in the current list
     */
    private fun fileNameExistsInList(
        fileName: String,
        recordList: List<CameraUploadsRecord>,
        folderType: CameraUploadFolderType
    ): Boolean =
        recordList.any { it.folderType == folderType && it.fileName == fileName }
}
