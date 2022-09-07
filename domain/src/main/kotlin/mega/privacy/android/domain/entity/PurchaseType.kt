package mega.privacy.android.domain.entity

/**
 * Purchase type enum class.
 */
enum class PurchaseType {
    /**
     * Success purchase.
     */
    SUCCESS,

    /**
     * Pending purchase.
     */
    PENDING,

    /**
     * Downgrade purchase.
     */
    DOWNGRADE
}