package mega.privacy.android.domain.usecase.transfers.downloads

import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.featuretoggle.DomainFeatures
import mega.privacy.android.domain.repository.FileSystemRepository
import mega.privacy.android.domain.repository.SettingsRepository
import mega.privacy.android.domain.repository.TransferRepository
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import javax.inject.Inject

/**
 * Use case to get if download location needs to be asked to the user or it's already defined in settings.
 * If the location is set, it ensures that sill have persisted permission and tries to update it so it's more unlikely that is lost in near future
 */
class ShouldAskDownloadDestinationUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val transferRepository: TransferRepository,
    private val fileSystemRepository: FileSystemRepository,
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
) {
    /**
     * Invoke
     */
    suspend operator fun invoke(): Boolean {
        return when {
            !(getFeatureFlagValueUseCase(DomainFeatures.AllowToChooseDownloadDestination) || transferRepository.allowUserToSetDownloadDestination()) -> false
            settingsRepository.isStorageAskAlways() -> true
            else -> settingsRepository.getStorageDownloadLocation()?.let { UriPath(it) }
                ?.takeIf {
                    fileSystemRepository.hasPersistedPermission(it, true)
                            && fileSystemRepository.doesUriPathExist(it)
                }
                ?.also {
                    // Take again the permission to ensure it's updated in persistedUriPermissions list so it's more unlikely that permission is lost
                    fileSystemRepository.takePersistablePermission(it, true)
                } == null
        }
    }
}