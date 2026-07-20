package mega.privacy.android.navigation.payment

/**
 * The quota situation that triggered the quota-warning upsell screen. Determines the
 * illustration, title, subtitle and which usage metric (storage or transfer) is shown.
 */
enum class QuotaWarningType {
    /**
     * Storage warning. Whether it is "almost full" or "full" is derived from the backend
     * [mega.privacy.android.domain.entity.StorageState], not from the caller.
     */
    Storage,

    /**
     * Transfer-quota warning. Whether it is "running low" or "exceeded" is derived from the
     * backend transfer over-quota state, not from the caller.
     */
    Transfer,
}
