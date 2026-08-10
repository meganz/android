package mega.privacy.android.feature.sync.domain.usecase.sync

import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.usecase.backup.GetLocalSyncOrBackupUriPathUseCase
import javax.inject.Inject

/**
 * Implementation of [GetLocalSyncOrBackupUriPathUseCase] that retrieves local URI paths
 * for all configured sync and backup folder pairs.
 *
 * This use case extracts the local folder paths from all existing folder pairs
 * and converts them into [UriPath] objects for use in backup and sync operations.
 *
 * @property getFolderPairsUseCase Use case to retrieve all configured folder pairs
 */
internal class GetLocalSyncOrBackupUriPathUseCaseImpl @Inject constructor(
    private val getFolderPairsUseCase: GetFolderPairsUseCase,
) : GetLocalSyncOrBackupUriPathUseCase {

    /**
     * Retrieves all local URI paths from configured sync and backup folder pairs
     *
     * @return List of [UriPath] representing local folder paths from all folder pairs
     */
    override suspend fun invoke(): List<UriPath> {
        return getFolderPairsUseCase().map {
            UriPath(it.localFolderPath)
        }
    }
}
