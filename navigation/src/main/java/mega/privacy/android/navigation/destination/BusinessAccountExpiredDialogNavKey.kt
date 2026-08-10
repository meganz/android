package mega.privacy.android.navigation.destination

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import mega.privacy.android.navigation.contract.dialog.DialogNavKey

/**
 * Navigation key for business account expired dialog
 * @property isProFlexiAccount Whether the account is Pro Flexi (true) or Business (false)
 * @property isMasterBusinessAccount Whether the user is a master business account admin
 */
@Serializable
@Parcelize
data class BusinessAccountExpiredDialogNavKey(
    val isProFlexiAccount: Boolean,
    val isMasterBusinessAccount: Boolean,
) : DialogNavKey, Parcelable

