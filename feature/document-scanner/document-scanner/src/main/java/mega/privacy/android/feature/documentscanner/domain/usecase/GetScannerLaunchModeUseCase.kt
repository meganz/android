package mega.privacy.android.feature.documentscanner.domain.usecase

import mega.privacy.android.domain.entity.ConnectivityState
import mega.privacy.android.domain.usecase.documentscanner.isCustomScannerEnabled
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.network.GetCurrentConnectivityStateUseCase
import mega.privacy.android.feature.documentscanner.domain.launchmode.CellularConsentRequiredException
import mega.privacy.android.feature.documentscanner.domain.launchmode.LegacyReason
import mega.privacy.android.feature.documentscanner.domain.launchmode.ScannerLaunchMode
import mega.privacy.android.feature.documentscanner.domain.model.ScannerModelProvider
import mega.privacy.android.feature.documentscanner.domain.repository.ScannerPreferencesRepository
import javax.inject.Inject

/**
 * Decides which scanner the user should land in when they tap the scan entry point.
 *
 * Returns:
 * - [ScannerLaunchMode.Legacy] when the feature flag is off, or the model is not
 *   cached and the device is offline (so it cannot be downloaded).
 * - [ScannerLaunchMode.New] when the model is already cached — works offline too,
 *   since no download is needed.
 * - [ScannerLaunchMode.NeedsDownload] when the flag is on, the model is not cached,
 *   and we are online on a network we may download over (Wi-Fi, or cellular with
 *   prior consent).
 *
 * The cache check comes before the network check on purpose: once the model is on
 * disk the scanner runs fully offline, so a cached model should never fall back to
 * legacy for lack of a connection.
 *
 * "User said no this session" is not represented here: once the prompt is
 * declined the router routes to [ScannerLaunchMode.Legacy] directly without
 * re-consulting this use case, so [LegacyReason.UserDeclined] is produced by the
 * prepare/dialog flow rather than by this decision tree.
 *
 * Any non-Wi-Fi connected transport (incl. ethernet) is treated as "cellular"
 * for consent purposes — intentional, to keep the metered-data guard simple.
 *
 * @throws CellularConsentRequiredException when the model is not cached and the
 * device is on cellular without a stored consent — the dialog catches it, shows
 * the consent prompt, and either persists a "yes" and retries, or routes to legacy
 * on decline.
 */
class GetScannerLaunchModeUseCase @Inject constructor(
    private val getFeatureFlagValue: GetFeatureFlagValueUseCase,
    private val getCurrentConnectivityState: GetCurrentConnectivityStateUseCase,
    private val scannerPreferences: ScannerPreferencesRepository,
    private val scannerModelProvider: ScannerModelProvider,
) {
    suspend operator fun invoke(): ScannerLaunchMode {
        if (!getFeatureFlagValue.isCustomScannerEnabled()) {
            return ScannerLaunchMode.Legacy(LegacyReason.FlagOff)
        }
        if (scannerModelProvider.cachedModelFile() != null) {
            return ScannerLaunchMode.New
        }
        return when (val state = getCurrentConnectivityState()) {
            ConnectivityState.Disconnected -> ScannerLaunchMode.Legacy(LegacyReason.NoNetwork)
            is ConnectivityState.Connected ->
                if (state.isOnWifi || scannerPreferences.hasGrantedCellularConsent()) {
                    ScannerLaunchMode.NeedsDownload
                } else {
                    throw CellularConsentRequiredException()
                }
        }
    }
}
