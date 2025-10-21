package mega.privacy.android.app.usecase.orientation

import android.os.Build
import mega.privacy.android.app.features.OrientationMigrationFeature
import mega.privacy.android.domain.repository.EnvironmentRepository
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import javax.inject.Inject

/**
 * Use case to determine if the app should show adaptive layout for Android 16+ orientation compatibility.
 *
 * This use case combines two conditions:
 * 1. Device must be running Android 16 or higher
 * 2. The orientation migration feature flag must be enabled
 *
 * When both conditions are met, the app should use adaptive layouts that can handle
 * orientation changes gracefully on large screen devices (tablets and foldables).
 */
class ShouldShowAdaptiveLayoutUseCase @Inject constructor(
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
    private val environmentRepository: EnvironmentRepository,
) {
    /**
     * Determines if adaptive layout should be shown.
     *
     * @return true if both Android 16+ is supported and the feature flag is enabled, false otherwise
     */
    suspend operator fun invoke(): Boolean =
        isAndroid16OrHigher() && getFeatureFlagValueUseCase(OrientationMigrationFeature.Android16OrientationMigrationEnabled)

    /**
     * Checks if the device is running Android 16 or higher.
     *
     * Android 16 introduced new orientation requirements for large screen devices.
     * Apps must adapt to ignore fixed orientations on tablets and foldables.
     *
     * @return true if the device is running Android 16+, false otherwise
     */
    private suspend fun isAndroid16OrHigher(): Boolean =
        environmentRepository.getDeviceSdkVersionInt() >= Build.VERSION_CODES.VANILLA_ICE_CREAM
}
