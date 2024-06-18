package mega.privacy.android.shared.original.core.ui.utils

import android.content.Context
import android.content.ContextWrapper
import androidx.fragment.app.FragmentActivity

/**
 * Find the [FragmentActivity] from the [Context].
 *
 * This method could be handy if you need to initialize a ViewModel from a Composable function
 * attached to a FragmentActivity lifecycle
 */
fun Context.findFragmentActivity(): FragmentActivity? {
    var currentContext = this
    var previousContext: Context? = null
    while (currentContext is ContextWrapper && previousContext != currentContext) {
        if (currentContext is FragmentActivity) {
            return currentContext
        }
        previousContext = currentContext
        currentContext = currentContext.baseContext
    }
    return null
}