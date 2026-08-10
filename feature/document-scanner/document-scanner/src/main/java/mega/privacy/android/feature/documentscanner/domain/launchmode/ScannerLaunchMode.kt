package mega.privacy.android.feature.documentscanner.domain.launchmode

/**
 * Decision the scanner entry point makes before showing the camera.
 *
 * Computed by `GetScannerLaunchModeUseCase` from the feature flag, the current
 * network state, and the persisted cellular consent. The prepare screen and
 * the legacy-fallback router act on it.
 *
 * "On cellular without consent" is not a mode: the use case throws
 * [CellularConsentRequiredException] instead, and the caller shows the
 * consent prompt before deciding where to route.
 */
sealed interface ScannerLaunchMode {

    /**
     * Feature flag is on and the model is already cached, so the scanner can
     * open immediately — this works even offline, since no download is needed.
     * The presentation layer routes straight to the camera.
     */
    data object New : ScannerLaunchMode

    /**
     * Feature flag is on, the model is not cached yet, and the device is online
     * on a network we may download over (Wi-Fi, or cellular with prior consent).
     * The presentation layer shows the download-confirmation dialog / prepare
     * screen before the camera.
     */
    data object NeedsDownload : ScannerLaunchMode

    /** The new scanner is not appropriate; route to the legacy ML Kit scanner. */
    data class Legacy(val reason: LegacyReason) : ScannerLaunchMode
}

/** Why the entry point chose [ScannerLaunchMode.Legacy]. Logged + drives UX copy. */
enum class LegacyReason {
    /** Feature flag disabled (defaults / rollout off / killswitch). */
    FlagOff,

    /** Device is offline so the model cannot be downloaded on first use. */
    NoNetwork,

    /** User said "no" to the cellular-consent prompt this session. */
    UserDeclined,

    /** Launch-mode resolution failed unexpectedly; fall back to legacy as a safety net. */
    Unknown,
}
