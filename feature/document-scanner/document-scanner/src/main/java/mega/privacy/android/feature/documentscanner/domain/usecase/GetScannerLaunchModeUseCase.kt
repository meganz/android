package mega.privacy.android.feature.documentscanner.domain.usecase

import mega.privacy.android.domain.entity.ConnectivityState
import mega.privacy.android.domain.usecase.documentscanner.isCustomScannerEnabled
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.network.GetCurrentConnectivityStateUseCase
import mega.privacy.android.feature.documentscanner.domain.launchmode.CellularConsentRequiredException
import mega.privacy.android.feature.documentscanner.domain.launchmode.LegacyReason
import mega.privacy.android.feature.documentscanner.domain.launchmode.ScannerLaunchMode
import mega.privacy.android.feature.documentscanner.domain.repository.ScannerPreferencesRepository
import javax.inject.Inject

/**
 * Decides which scanner the user should land in when they tap the scan entry point.
 *
 * Returns:
 * - [ScannerLaunchMode.Legacy] when the feature flag is off or the device is offline.
 * - [ScannerLaunchMode.New] otherwise (Wi-Fi, or cellular with prior consent).
 *
 * "User said no this session" is not represented here: once the prompt is
 * declined the entry point routes to [ScannerLaunchMode.Legacy] directly
 * without re-consulting this use case, so [LegacyReason.UserDeclined] is
 * produced by the prepare screen rather than by this decision tree.
 *
 * Any non-Wi-Fi connected transport (incl. ethernet) is treated as "cellular"
 * for consent purposes — intentional, to keep the metered-data guard simple.
 *
 * @throws CellularConsentRequiredException when on cellular without a stored
 * consent — the prepare screen catches it, shows the consent dialog, and
 * either persists a "yes" and retries, or routes to legacy on decline.
 */
class GetScannerLaunchModeUseCase @Inject constructor(
    private val getFeatureFlagValue: GetFeatureFlagValueUseCase,
    private val getCurrentConnectivityState: GetCurrentConnectivityStateUseCase,
    private val scannerPreferences: ScannerPreferencesRepository,
) {
    suspend operator fun invoke(): ScannerLaunchMode {
        if (!getFeatureFlagValue.isCustomScannerEnabled()) {
            return ScannerLaunchMode.Legacy(LegacyReason.FlagOff)
        }
        return when (val state = getCurrentConnectivityState()) {
            ConnectivityState.Disconnected -> ScannerLaunchMode.Legacy(LegacyReason.NoNetwork)
            is ConnectivityState.Connected ->
                if (state.isOnWifi || scannerPreferences.hasGrantedCellularConsent()) {
                    ScannerLaunchMode.New
                } else {
                    throw CellularConsentRequiredException()
                }
        }
    }
}
