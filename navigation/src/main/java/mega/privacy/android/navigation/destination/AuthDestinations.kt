package mega.privacy.android.navigation.destination

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import mega.privacy.android.navigation.contract.navkey.NoSessionNavKey


@Serializable
@Parcelize
data class LoginNavKey(
    val action: String? = null,
    val link: String? = null,
    val timeStamp: Long = System.currentTimeMillis(),
) : NoSessionNavKey.Mandatory, Parcelable

/**
 * Park account confirmation screen, opened from a park-account link.
 *
 * @param link the park-account confirmation link
 */
@Serializable
@Parcelize
data class ParkAccountNavKey(
    val link: String,
) : NoSessionNavKey.Optional, Parcelable

/**
 * Create Account Screen
 * @param initialEmail if set, the email field will be pre-filled with this value
 */
@Serializable
@Parcelize
data class CreateAccountNavKey(
    val initialEmail: String? = null,
) : NoSessionNavKey.Mandatory, Parcelable