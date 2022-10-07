package mega.privacy.android.app.presentation.extensions

import android.content.Intent
import android.os.Build
import android.os.Bundle
import java.io.Serializable

/**
 * Serializable extension to extract serialized objects from a bundle
 *
 * @param T deserialized type
 * @param key
 * @return the deserialized object
 */
inline fun <reified T : Serializable> Bundle.serializable(key: String): T? = when {
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> getSerializable(key, T::class.java)
    else -> @Suppress("DEPRECATION") getSerializable(key) as? T
}

/**
 * Serializable extension to extract serialized objects from an intent
 *
 * @param T deserialized type
 * @param key
 * @return the deserialized object
 */
inline fun <reified T : Serializable> Intent.serializable(key: String): T? = when {
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> getSerializableExtra(key, T::class.java)
    else -> @Suppress("DEPRECATION") getSerializableExtra(key) as? T
}