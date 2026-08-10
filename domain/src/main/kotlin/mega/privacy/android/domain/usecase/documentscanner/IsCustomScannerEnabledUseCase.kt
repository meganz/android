package mega.privacy.android.domain.usecase.documentscanner

import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase

/**
 * Checks if the custom continuous document scanner feature flag is enabled.
 *
 * @return true if the custom continuous document scanner is enabled
 */
suspend fun GetFeatureFlagValueUseCase.isCustomScannerEnabled(): Boolean =
    this(ApiFeatures.ContinuousDocumentScanner)
