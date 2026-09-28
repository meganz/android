package mega.privacy.android.domain.usecase.folderpreference

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import mega.privacy.android.domain.entity.preference.ViewModePreference
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.setting.MonitorViewModePreferenceUseCase
import javax.inject.Inject

/**
 * Monitor the view type to apply to a folder: the per-folder value when the feature flag is enabled
 * and the user chose [ViewModePreference.PerFolder]; otherwise [orElse], the current global behaviour.
 */
class MonitorFolderViewTypeUseCase @Inject constructor(
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
    private val monitorViewModePreferenceUseCase: MonitorViewModePreferenceUseCase,
    private val monitorFolderPreferenceUseCase: MonitorFolderPreferenceUseCase,
) {
    /**
     * @param folderKey base64 node handle for cloud/shares, file path for offline
     * @param orElse the current global view type flow, used when per-folder view mode is off
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(folderKey: String, orElse: Flow<ViewType>): Flow<ViewType> = flow {
        val enabled = getFeatureFlagValueUseCase(ApiFeatures.SortingAndViewMode)

        emitAll(
            monitorViewModePreferenceUseCase().flatMapLatest { preference ->
                if (enabled && preference == ViewModePreference.PerFolder) {
                    monitorFolderPreferenceUseCase(folderKey).map { it?.viewType ?: ViewType.LIST }
                } else {
                    orElse
                }
            }
        )
    }
}
