package mega.privacy.android.domain.usecase.folderpreference

import mega.privacy.android.domain.entity.preference.FolderPreference
import mega.privacy.android.domain.repository.FolderPreferenceRepository
import javax.inject.Inject

/**
 * Store a folder's preferences, replacing any existing values.
 */
class SetFolderPreferenceUseCase @Inject constructor(
    private val folderPreferenceRepository: FolderPreferenceRepository,
) {
    suspend operator fun invoke(preference: FolderPreference) =
        folderPreferenceRepository.setFolderPreference(preference)
}
