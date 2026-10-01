package mega.privacy.android.domain.entity.agesignal

/**
 * Defines the required compliance status for the application based on the user's age signal.
 * This is the ultimate source of truth for the Use Case.
 */
enum class UserAgeComplianceStatus {

    /**
     * This status covers SUPERVISED, SUPERVISED_APPROVAL_PENDING,
     * SUPERVISED_APPROVAL_DENIED, UNKNOWN, and API Failure.
     * All these states mandate the safest (most restrictive) data handling.
     */
    RequiresMinorRestriction,

    /**
     * Status for the standard adult flow.
     */
    AdultVerified
}
