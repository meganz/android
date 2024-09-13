package mega.privacy.android.feature.sync.ui.model

/**
 * Sync promotion state
 *
 * @property shouldShowSyncPromotion    Indicates if should show the Sync Promotion
 * @property isFreeAccount              Indicates if account is Free or not
 */
data class SyncPromotionState(
    val shouldShowSyncPromotion: Boolean = false,
    val isFreeAccount: Boolean = true,
)