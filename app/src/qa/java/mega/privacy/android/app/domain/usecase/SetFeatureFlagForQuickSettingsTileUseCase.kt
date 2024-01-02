package mega.privacy.android.app.domain.usecase

import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.data.gateway.preferences.FeatureFlagPreferencesGateway
import javax.inject.Inject

/**
 * Use case to set the selected feature flag for quick settings tile
 */
class SetFeatureFlagForQuickSettingsTileUseCase @Inject constructor(
    private val featureFlagPreferencesGateway: FeatureFlagPreferencesGateway,
) {

    /**
     * Invoke
     */
    suspend operator fun invoke(appFeatures: AppFeatures) =
        featureFlagPreferencesGateway.setFeatureFlagForQuickSettingsTile(appFeatures.name)
}