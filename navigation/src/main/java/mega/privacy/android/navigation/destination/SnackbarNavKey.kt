package mega.privacy.android.navigation.destination

import android.content.Context
import android.os.Parcelable
import androidx.annotation.StringRes
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import mega.privacy.android.navigation.contract.navkey.NoSessionNavKey

/**
 * Navigation to show a snackbar
 */
@Serializable
@Parcelize
class SnackbarNavKey private constructor(
    private val message: String?,
    @StringRes private val messageResource: Int?,
) : NoSessionNavKey.Optional, Parcelable {
    constructor(message: String) : this(message, null)
    constructor(@StringRes messageResource: Int) : this(null, messageResource)

    /**
     * Get the message to display in the snackbar
     */
    fun getMessage(context: Context): String? =
        message ?: messageResource?.let { context.getString(it) }
}