package mega.privacy.android.app.presentation.data

import android.content.res.Resources
import androidx.annotation.StringRes
import androidx.core.content.res.ResourcesCompat
import mega.privacy.android.app.utils.Constants

/**
 * SnackBar item
 *
 * @property type
 * @property stringRes
 * @property stringArg
 * @property intArg
 */
data class SnackBarItem(
    val type: Int = Constants.SNACKBAR_TYPE,
    @StringRes var stringRes: Int,
    val stringArg: String? = null,
    val intArg: Int? = null,
) {

    /**
     * Get SnackBar message
     *
     * @param resources     [Resources]
     */
    fun getMessage(resources: Resources): String =
        resources.getString(stringRes, stringArg ?: intArg)
}
