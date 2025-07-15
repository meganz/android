package mega.privacy.android.domain.usecase.transfers.downloads

import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.repository.FileSystemRepository
import mega.privacy.android.domain.repository.SettingsRepository
import mega.privacy.android.domain.usecase.GetStorageDownloadDefaultPathUseCase
import javax.inject.Inject

/**
 * Use case to get if download location needs to be asked to the user or it's already defined in settings.
 * If the location is set, it ensures that sill have persisted permission and tries to update it so it's more unlikely that is lost in near future
 */
class ShouldAskDownloadDestinationUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val fileSystemRepository: FileSystemRepository,
    private val getStorageDownloadDefaultPathUseCase: GetStorageDownloadDefaultPathUseCase,
) {
    /**
     * Invoke
     */
    suspend operator fun invoke(): Boolean {
        if (settingsRepository.isAskForDownloadLocation()) return true
        else {
            val uriPath = settingsRepository.getDownloadLocation()
                ?.let { UriPath(it) } ?: return true

            val hasPermission = fileSystemRepository.hasPersistedPermission(uriPath, true)
            val pathExists = fileSystemRepository.doesUriPathExist(uriPath)
            val canUseIt = (hasPermission && pathExists)
                    || uriPath.value == getStorageDownloadDefaultPathUseCase() //default path can always be used

            if (canUseIt) {
                // Take again the permission to ensure it's updated in persistedUriPermissions list so it's more unlikely that permission is lost
                fileSystemRepository.takePersistablePermission(uriPath, true)
                return false
            } else {
                // if saved path does not exist anymore or permission is lost, we'll ask the user again if they want to save it as default or not
                settingsRepository.setShouldPromptToSaveDestination(true)
                // in the meanwhile we'll ask a new destination
                settingsRepository.setAskForDownloadLocation(true)
                settingsRepository.setDownloadLocation(null)
                return true
            }
        }
    }
}