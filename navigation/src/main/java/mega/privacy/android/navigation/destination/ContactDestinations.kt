package mega.privacy.android.navigation.destination

import kotlinx.serialization.Serializable
import mega.privacy.android.navigation.contract.dialog.DialogNavKey

/**
 * Cannot verify contact dialog nav key
 *
 * @property email
 */
@Serializable
data class CannotVerifyContactDialogNavKey(val email: String) : DialogNavKey
