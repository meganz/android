package mega.privacy.android.domain.usecase.folderpreference

import mega.privacy.android.domain.repository.FolderPreferenceRepository
import javax.inject.Inject

/**
 * Clear all stored per-folder preferences.
 */
class ClearFolderPreferencesUseCase @Inject constructor(
    private val folderPreferenceRepository: FolderPreferenceRepository,
) {
    suspend operator fun invoke() = folderPreferenceRepository.clearFolderPreferences()
}
