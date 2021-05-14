package mega.privacy.android.app.utils

import android.app.Activity
import androidx.fragment.app.Fragment

object ExtraUtils {

    inline fun <reified T : Any> Activity.extra(key: String, default: T? = null) = lazy {
        val value = intent?.extras?.get(key)
        if (value is T) value else default
    }

    inline fun <reified T : Any> Activity.extraNotNull(key: String, default: T? = null) = lazy {
        val value = intent?.extras?.get(key)
        requireNotNull(if (value is T) value else default) { key }
    }

    inline fun <reified T : Any> Fragment.extra(key: String, default: T? = null) = lazy {
        val value = arguments?.get(key)
        if (value is T) value else default
    }

    inline fun <reified T : Any> Fragment.extraNotNull(key: String, default: T? = null) = lazy {
        val value = arguments?.get(key)
        requireNotNull(if (value is T) value else default) { key }
    }
}
