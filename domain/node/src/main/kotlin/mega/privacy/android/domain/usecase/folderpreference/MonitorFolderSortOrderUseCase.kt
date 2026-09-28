package mega.privacy.android.domain.usecase.folderpreference

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.preference.SortingPreference
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.setting.MonitorSortingPreferenceUseCase
import javax.inject.Inject

/**
 * Monitor the sort order to apply to a folder: the per-folder value when the feature flag is enabled
 * and the user chose [SortingPreference.PerFolder]; otherwise [orElse], the current global behaviour.
 */
class MonitorFolderSortOrderUseCase @Inject constructor(
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
    private val monitorSortingPreferenceUseCase: MonitorSortingPreferenceUseCase,
    private val monitorFolderPreferenceUseCase: MonitorFolderPreferenceUseCase,
) {
    /**
     * @param folderKey base64 node handle for cloud/shares, file path for offline
     * @param orElse the current global sort order flow, used when per-folder sorting is off
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(folderKey: String, orElse: Flow<SortOrder>): Flow<SortOrder> = flow {
        val enabled = getFeatureFlagValueUseCase(ApiFeatures.SortingAndViewMode)

        emitAll(
            monitorSortingPreferenceUseCase().flatMapLatest { preference ->
                if (enabled && preference == SortingPreference.PerFolder) {
                    monitorFolderPreferenceUseCase(folderKey)
                        .map { it?.sortOrder ?: SortOrder.ORDER_DEFAULT_ASC }
                } else {
                    orElse
                }
            }
        )
    }
}
