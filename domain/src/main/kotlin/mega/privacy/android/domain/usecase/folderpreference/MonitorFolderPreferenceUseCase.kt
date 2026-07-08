package mega.privacy.android.domain.usecase.folderpreference

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.preference.FolderPreference
import mega.privacy.android.domain.repository.FolderPreferenceRepository
import javax.inject.Inject

/**
 * Monitor the per-folder preferences stored for a folder.
 */
class MonitorFolderPreferenceUseCase @Inject constructor(
    private val folderPreferenceRepository: FolderPreferenceRepository,
) {
    /**
     * @param folderKey node-handle-as-string for cloud/shares, file path for offline
     * @return a [Flow] of the folder's [FolderPreference], or null if none has been stored
     */
    operator fun invoke(folderKey: String): Flow<FolderPreference?> =
        folderPreferenceRepository.monitorFolderPreference(folderKey)
}
