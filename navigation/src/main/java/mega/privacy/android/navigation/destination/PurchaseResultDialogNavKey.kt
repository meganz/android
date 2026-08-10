package mega.privacy.android.navigation.destination

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import mega.privacy.android.domain.entity.PurchaseType
import mega.privacy.android.navigation.contract.dialog.DialogNavKey

/**
 * Navigation key for purchase result dialog
 * @property purchaseType The type of purchase result (SUCCESS, PENDING, DOWNGRADE)
 * @property activeSubscriptionSku The SKU of the active subscription (only used for SUCCESS type)
 */
@Serializable
@Parcelize
data class PurchaseResultDialogNavKey(
    val purchaseType: PurchaseType,
    val activeSubscriptionSku: String,
) : DialogNavKey, Parcelable

