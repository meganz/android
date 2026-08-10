package mega.privacy.android.feature.documentscanner.domain.launchmode

/**
 * Thrown by `GetScannerLaunchModeUseCase` when the device is on a metered
 * (non-Wi-Fi) connection and the user has not yet consented to downloading
 * the ~93 MB scanner model over cellular data.
 *
 * The caller catches this, shows the consent prompt, and then either persists
 * the consent (→ retry resolves to [ScannerLaunchMode.New]) or routes to
 * [ScannerLaunchMode.Legacy] with [LegacyReason.UserDeclined] for the rest of
 * the session.
 */
class CellularConsentRequiredException :
    RuntimeException("Cellular consent required before downloading the scanner model")
