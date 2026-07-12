package mega.privacy.android.domain.usecase.folderpreference

import kotlinx.coroutines.flow.first
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.preference.FolderPreference
import mega.privacy.android.domain.entity.preference.SortingPreference
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.setting.MonitorSortingPreferenceUseCase
import javax.inject.Inject

/**
 * Persist a folder's sort order: store it per folder when the feature flag is enabled and the user
 * chose [SortingPreference.PerFolder]; otherwise apply [orElse], the current global behaviour.
 */
class SetFolderSortOrderUseCase @Inject constructor(
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
    private val monitorSortingPreferenceUseCase: MonitorSortingPreferenceUseCase,
    private val setFolderPreferenceUseCase: SetFolderPreferenceUseCase,
) {
    /**
     * @param folderKey base64 node handle for cloud/shares, file path for offline
     * @param sortOrder the sort order to store
     * @param currentViewType the folder's current view type, kept alongside the sort order
     * @param orElse the current global sort order setter, used when per-folder sorting is off
     */
    suspend operator fun invoke(
        folderKey: String,
        sortOrder: SortOrder,
        currentViewType: ViewType,
        orElse: suspend (SortOrder) -> Unit,
    ) {
        val enabled = getFeatureFlagValueUseCase(ApiFeatures.SortingAndViewMode)
        val perFolder = enabled &&
                monitorSortingPreferenceUseCase().first() == SortingPreference.PerFolder

        if (perFolder) {
            setFolderPreferenceUseCase(
                FolderPreference(
                    folderKey = folderKey,
                    sortOrder = sortOrder,
                    viewType = currentViewType,
                )
            )
        } else {
            orElse(sortOrder)
        }
    }
}
