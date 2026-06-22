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

    /** Model must be downloaded first; show the download-confirmation / prepare flow. */
    data object NeedsDownload : ScannerRoute

    /**
     * On cellular without consent; show the cellular-consent prompt before any
     * download.
     */
    data object NeedsCellularConsent : ScannerRoute

    /** Fall back to the legacy ML Kit scanner via `LegacyDocumentScanNavKey`. */
    data class UseLegacy(val reason: LegacyReason) : ScannerRoute
}
