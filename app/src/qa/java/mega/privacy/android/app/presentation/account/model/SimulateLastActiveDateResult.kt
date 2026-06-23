package mega.privacy.android.app.presentation.account.model

/**
 * Result of simulating the user's last active date. These outcomes are mutually exclusive, so a
 * single event carrying this value replaces the separate success/invalid/failure events.
 */
enum class SimulateLastActiveDateResult {
    /** The last active date was simulated successfully. */
    Success,

    /** The selected date matches the previously simulated date and was rejected. */
    Invalid,

    /** Simulating the last active date failed. */
    Failure,
}
