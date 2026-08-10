package mega.privacy.android.feature.documentscanner.presentation.model

import mega.privacy.android.feature.documentscanner.domain.launchmode.LegacyReason

/**
 * Where the scanner router should send the user after resolving the launch mode.
 *
 * The router lands on `ContinuousScanNavKey`, computes this from
 * `GetScannerLaunchModeUseCase`, and the screen renders / navigates accordingly.
 */
sealed interface ScannerRoute {

    /** Still deciding — show a lightweight loading state. */
    data object Resolving : ScannerRoute

    /** Model is ready; open the camera. */
    data object LaunchCamera : ScannerRoute

    /** Model must be downloaded first; show the download-confirmation dialog (Wi-Fi variant). */
    data object NeedsDownload : ScannerRoute

    /**
     * On cellular without consent; show the download-confirmation dialog's cellular
     * variant, which asks for metered-data consent before any download.
     */
    data object NeedsCellularConsent : ScannerRoute

    /**
     * The user confirmed the download from the confirmation dialog; hand off to the
     * prepare/loading screen that drives and observes the download.
     */
    data object PreparingDownload : ScannerRoute

    /** Fall back to the legacy ML Kit scanner via `LegacyScanDocumentNavKey`. */
    data class UseLegacy(val reason: LegacyReason) : ScannerRoute
}
