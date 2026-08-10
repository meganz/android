package mega.privacy.android.navigation.destination

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import mega.privacy.android.navigation.contract.dialog.DialogNavKey

/**
 * Navigation key for enable 2FA dialog
 */
@Serializable
@Parcelize
data object Enable2FANavKey : DialogNavKey, Parcelable
