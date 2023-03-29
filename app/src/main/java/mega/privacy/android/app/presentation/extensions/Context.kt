package mega.privacy.android.app.presentation.extensions

import android.content.Context
import androidx.annotation.StringRes

/**
 * Get formatted string or default
 *
 * @param resId of the formatted string
 * @param formatArgs Format arguments for the string
 */
@Deprecated(
    message = "This has been deprecated in favour of getString",
    replaceWith = ReplaceWith(expression = "android.content.Context#getString(int, Object...)")
)
fun Context.getFormattedStringOrDefault(@StringRes resId: Int, vararg formatArgs: Any?) =
    getString(resId, *formatArgs)


/**
 * Get formatted quantity string or default
 *
 * @param resId of the plural.
 * @param formatArgs Format arguments for the string
 */
@Deprecated(
    message = "This has been deprecated in favour of getQuantityString",
    replaceWith = ReplaceWith(expression = "android.content.res.Resources#getQuantityString(int, int, Object...)")
)
fun Context.getQuantityStringOrDefault(resId: Int, quantity: Int, vararg formatArgs: Any?) =
    resources.getQuantityString(resId, quantity, *formatArgs)
