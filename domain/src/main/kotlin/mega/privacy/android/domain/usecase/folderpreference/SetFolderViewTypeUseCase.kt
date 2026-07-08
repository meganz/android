package mega.privacy.android.domain.usecase.folderpreference

import kotlinx.coroutines.flow.first
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.preference.FolderPreference
import mega.privacy.android.domain.entity.preference.ViewModePreference
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.setting.MonitorViewModePreferenceUseCase
import javax.inject.Inject

/**
 * Persist a folder's view type: store it per folder when the feature flag is enabled and the user
 * chose [ViewModePreference.PerFolder]; otherwise apply [orElse], the current global behaviour.
 */
class SetFolderViewTypeUseCase @Inject constructor(
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
    private val monitorViewModePreferenceUseCase: MonitorViewModePreferenceUseCase,
    private val setFolderPreferenceUseCase: SetFolderPreferenceUseCase,
) {
    /**
     * @param folderKey base64 node handle for cloud/shares, file path for offline
     * @param viewType the view type to store
     * @param currentSortOrder the folder's current sort order, kept alongside the view type
     * @param orElse the current global view type setter, used when per-folder view mode is off
     */
    suspend operator fun invoke(
        folderKey: String,
        viewType: ViewType,
        currentSortOrder: SortOrder,
        orElse: suspend (ViewType) -> Unit,
    ) {
        val enabled = getFeatureFlagValueUseCase(ApiFeatures.SortingAndViewMode)
        val perFolder = enabled &&
                monitorViewModePreferenceUseCase().first() == ViewModePreference.PerFolder

        if (perFolder) {
            setFolderPreferenceUseCase(
                FolderPreference(
                    folderKey = folderKey,
                    sortOrder = currentSortOrder,
                    viewType = viewType,
                )
            )
        } else {
            orElse(viewType)
        }
    }
}
