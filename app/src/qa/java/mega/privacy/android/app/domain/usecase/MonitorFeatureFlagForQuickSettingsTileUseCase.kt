package mega.privacy.android.app.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.mapNotNull
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.data.gateway.preferences.FeatureFlagPreferencesGateway
import javax.inject.Inject

/**
 * Use service to the the selected feature flag for quick settings tile and its value
 */
class MonitorFeatureFlagForQuickSettingsTileUseCase @Inject constructor(
    private val getAllFeatureFlags: GetAllFeatureFlags,
    private val featureFlagPreferencesGateway: FeatureFlagPreferencesGateway,
) {

    /**
     * Invoke
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke() =
        featureFlagPreferencesGateway.getCurrentFeatureFlagForQuickSettingsTile()
            .flatMapLatest { current ->
                if (current == null) {
                    flowOf(null)
                } else {
                    getAllFeatureFlags().mapNotNull { features ->
                        val feature = AppFeatures.valueOf(current)
                        feature to features[feature]
                    }.distinctUntilChanged()
                }
            }
}
