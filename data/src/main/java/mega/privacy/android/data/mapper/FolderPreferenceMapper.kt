package mega.privacy.android.data.mapper

import mega.privacy.android.data.database.entity.FolderPreferenceEntity
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.preference.FolderPreference
import mega.privacy.android.domain.entity.preference.ViewType
import javax.inject.Inject

/**
 * Mapper to convert a [FolderPreferenceEntity] into a [FolderPreference].
 */
internal class FolderPreferenceMapper @Inject constructor(
    private val sortOrderMapper: SortOrderMapper,
) {
    operator fun invoke(entity: FolderPreferenceEntity) = FolderPreference(
        folderKey = entity.folderKey,
        sortOrder = sortOrderMapper(entity.sortOrder) ?: SortOrder.ORDER_DEFAULT_ASC,
        viewType = ViewType(entity.viewType) ?: ViewType.LIST,
    )
}
