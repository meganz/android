package mega.privacy.android.feature.documentscanner.domain.usecase

import mega.privacy.android.feature.documentscanner.domain.repository.ScannerPreferencesRepository
import javax.inject.Inject

/**
 * Records that the user agreed to download the scanner model over a cellular
 * (metered) connection.
 *
 * The consent is persisted per-install by [ScannerPreferencesRepository] and never
 * re-asked: once granted, [GetScannerLaunchModeUseCase] stops throwing
 * [mega.privacy.android.feature.documentscanner.domain.launchmode.CellularConsentRequiredException]
 * and treats cellular like Wi-Fi for the download.
 */
class GrantScannerCellularConsentUseCase @Inject constructor(
    private val scannerPreferences: ScannerPreferencesRepository,
) {
    suspend operator fun invoke() = scannerPreferences.grantCellularConsent()
}
