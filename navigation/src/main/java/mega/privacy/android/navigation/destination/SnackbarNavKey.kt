package mega.privacy.android.navigation.destination

import android.content.Context
import android.os.Parcelable
import androidx.annotation.StringRes
import androidx.navigation3.runtime.NavKey
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

/**
 * Navigation to show a snackbar
 */
@Serializable
@Parcelize
data class SnackbarNavKey private constructor(
    private val message: String?,
    @StringRes private val messageResource: Int?,
) : NavKey, Parcelable {
    constructor(message: String) : this(message, null)
    constructor(@StringRes messageResource: Int) : this(null, messageResource)

    /**
     * Get the message to display in the snackbar
     */
    fun getMessage(context: Context): String? =
        message ?: messageResource?.let { context.getString(it) }
}