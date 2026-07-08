package mega.privacy.android.data.mapper

import mega.privacy.android.data.database.entity.FolderPreferenceEntity
import mega.privacy.android.domain.entity.preference.FolderPreference
import javax.inject.Inject

/**
 * Mapper to convert a [FolderPreference] into a [FolderPreferenceEntity].
 */
internal class FolderPreferenceEntityMapper @Inject constructor(
    private val sortOrderIntMapper: SortOrderIntMapper,
) {
    operator fun invoke(preference: FolderPreference) = FolderPreferenceEntity(
        folderKey = preference.folderKey,
        sortOrder = sortOrderIntMapper(preference.sortOrder),
        viewType = preference.viewType.id,
    )
}
