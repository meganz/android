package mega.privacy.android.navigation.destination

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import mega.privacy.android.navigation.contract.dialog.DialogNavKey

/**
 * Navigation key for security upgrade dialog
 */
@Serializable
@Parcelize
data object SecurityUpgradeDialogNavKey : DialogNavKey, Parcelable

