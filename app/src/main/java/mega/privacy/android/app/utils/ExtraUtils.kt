package mega.privacy.android.app.utils

import android.app.Activity
import androidx.fragment.app.Fragment

/**
 * Provide inline Intent/Bundle extras extractor
 * Took from https://proandroiddev.com/one-liner-intent-bundle-extras-extractor-in-kotlin-f4b614b390c4
 */
object ExtraUtils {

    /**
     * Get Activity Intent extra at runtime when accessing the data.
     */
    inline fun <reified T : Any> Activity.extra(key: String, default: T? = null) = lazy {
        val value = intent?.extras?.getString(key)
        if (value is T) value else default
    }

    /**
     * Get Activity Intent not nullable extra at runtime when accessing the data.
     */
    inline fun <reified T : Any> Activity.extraNotNull(key: String, default: T? = null) = lazy {
        val value = intent?.extras?.getString(key)
        requireNotNull(if (value is T) value else default) { key }
    }

    /**
     * Get Fragment Argument at runtime when accessing the data.
     */
    inline fun <reified T : Any> Fragment.extra(key: String, default: T? = null) = lazy {
        val value = arguments?.getString(key)
        if (value is T) value else default
    }

    /**
     * Get Fragment not nullable Argument at runtime when accessing the data.
     */
    inline fun <reified T : Any> Fragment.extraNotNull(key: String, default: T? = null) = lazy {
        val value = arguments?.getString(key)
        requireNotNull(if (value is T) value else default) { key }
    }
}
