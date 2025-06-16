package mega.privacy.android.domain.usecase.transfers.uploads

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.transfer.TransferAppData
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.repository.CacheRepository
import mega.privacy.android.domain.repository.TransferRepository
import mega.privacy.android.domain.usecase.environment.GetCurrentTimeInMillisUseCase
import mega.privacy.android.domain.usecase.file.GetGPSCoordinatesUseCase
import mega.privacy.android.domain.usecase.file.GetLastModifiedTimeUseCase
import java.io.File
import javax.inject.Inject
import kotlin.time.ExperimentalTime

/**
 * Uploads a list of files to the specified destination folder and returns a Flow to monitor the progress
 */
class UploadFileUseCase @Inject constructor(
    private val transferRepository: TransferRepository,
    private val cacheRepository: CacheRepository,
    private val getGPSCoordinatesUseCase: GetGPSCoordinatesUseCase,
    private val getLastModifiedTimeUseCase: GetLastModifiedTimeUseCase,
    private val getCurrentTimeInMillisUseCase: GetCurrentTimeInMillisUseCase,
) {

    /**
     * Invoke
     *
     * @param uriPath the [UriPath] of the file of folder to be uploaded
     * @param fileName the name of the file if it should be renamed, if null the original name will be kept
     * @param appData the list of [TransferAppData] that will be added to this transfer
     * @param parentFolderId destination folder id where [uriPath] will be uploaded
     * @param isHighPriority Whether the file or folder should be placed on top of the upload queue or not, chat uploads are always priority regardless of this parameter
     *
     * @return a flow of [TransferEvent]s to monitor the download state and progress
     */
    @OptIn(ExperimentalTime::class)
    operator fun invoke(
        uriPath: UriPath,
        fileName: String?,
        appData: List<TransferAppData>?,
        parentFolderId: NodeId,
        isHighPriority: Boolean,
    ): Flow<TransferEvent> = flow {
        val isSourceTemporary =
            cacheRepository.isFileInCacheDirectory(File(uriPath.value))
        val finalAppData = buildList {
            appData?.let { addAll(it) }
            if (isSourceTemporary) {
                getGPSCoordinatesUseCase(uriPath)?.let {
                    add(TransferAppData.Geolocation(it.first, it.second))
                }
            }
        }.takeIf { it.isNotEmpty() }
        val shouldStartFirst = isHighPriority
                || finalAppData?.any { it is TransferAppData.ChatUploadAppData } == true
        val modificationTime = getLastModifiedTimeUseCase(uriPath)?.epochSeconds
            ?: (getCurrentTimeInMillisUseCase() / 1000L)

        emitAll(
            transferRepository.startUpload(
                localPath = uriPath.value,
                parentNodeId = parentFolderId,
                fileName = fileName,
                modificationTime = modificationTime,
                appData = finalAppData,
                isSourceTemporary = isSourceTemporary,
                shouldStartFirst = shouldStartFirst,
            )
        )
    }
}