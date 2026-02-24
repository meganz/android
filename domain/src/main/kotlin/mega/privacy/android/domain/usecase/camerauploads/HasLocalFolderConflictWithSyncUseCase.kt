package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.featuretoggle.DomainFeatures
import mega.privacy.android.domain.usecase.backup.GetLocalSyncOrBackupUriPathUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.file.GetPathByDocumentContentUriUseCase
import java.io.File
import javax.inject.Inject

/**
 * UseCase that checks whether a local folder path conflicts with any existing
 * Sync/Backup local folders on the same device.
 */
class HasLocalFolderConflictWithSyncUseCase @Inject constructor(
    private val getLocalSyncOrBackupUriPathUseCase: GetLocalSyncOrBackupUriPathUseCase,
    private val getPathByDocumentContentUriUseCase: GetPathByDocumentContentUriUseCase,
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
) {

    /**
     * @param localFolderPath The local folder path to validate
     * @return true if the path conflicts with an existing sync/backup folder
     */
    suspend operator fun invoke(localFolderPath: String): Boolean {
        val isFeatureEnabled = runCatching {
            getFeatureFlagValueUseCase(DomainFeatures.DCIMSelectionAsSyncBackup)
        }.getOrElse { false }

        if (!isFeatureEnabled) {
            return false
        }

        if (localFolderPath.isEmpty() || localFolderPath == "/" || localFolderPath == File.separator) {
            return false
        }

        val normalizedLocalPath = localFolderPath.trimEnd('/', File.separatorChar)

        for (folderUriPath in getLocalSyncOrBackupUriPathUseCase()) {
            val syncLocalPath = getPathByDocumentContentUriUseCase(folderUriPath.value)
                ?: continue

            val normalizedSyncPath = syncLocalPath.trimEnd('/', File.separatorChar)

            if (pathsOverlap(normalizedLocalPath, normalizedSyncPath)) {
                return true
            }
        }

        return false
    }

    private fun pathsOverlap(path1: String, path2: String): Boolean {
        return path1 == path2 ||
                UriPath(path1).isSubPathOf(UriPath(path2)) ||
                UriPath(path2).isSubPathOf(UriPath(path1))
    }
}
