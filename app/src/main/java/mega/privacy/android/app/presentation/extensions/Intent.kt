package mega.privacy.android.app.presentation.extensions

import android.content.Intent
import android.os.Build
import android.os.Build.VERSION_CODES.TIRAMISU
import android.os.Bundle
import android.os.Parcelable
import java.io.Serializable

/**
 * Serializable extension to extract serialized objects from a bundle
 *
 * @param T deserialized type
 * @param key
 * @return the deserialized object
 */
inline fun <reified T : Serializable> Bundle.serializable(key: String): T? = when {
    Build.VERSION.SDK_INT >= TIRAMISU -> getSerializable(key, T::class.java)
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
    Build.VERSION.SDK_INT >= TIRAMISU -> getSerializableExtra(key, T::class.java)
    else -> @Suppress("DEPRECATION") getSerializableExtra(key) as? T
}

/**
 * Serializable extension to extract Parcelable objects from a bundle
 *
 * @param T deserialized type
 * @param key
 * @return the deserialized object
 */
inline fun <reified T : Parcelable> Intent.parcelable(key: String): T? = when {
    Build.VERSION.SDK_INT >= TIRAMISU -> getParcelableExtra(key, T::class.java)
    else -> @Suppress("DEPRECATION") getParcelableExtra(key) as? T
}

/**
 * Serializable extension to extract Parcelable objects from a bundle
 *
 * @param T deserialized type
 * @param key
 * @return the deserialized object
 */
inline fun <reified T : Parcelable> Bundle.parcelable(key: String): T? = when {
    Build.VERSION.SDK_INT >= TIRAMISU -> getParcelable(key, T::class.java)
    else -> @Suppress("DEPRECATION") getParcelable(key) as? T
}

/**
 * Serializable extension to extract Parcelable array list from a bundle
 *
 * @param T deserialized type
 * @param key
 * @return the deserialized array list
 */
inline fun <reified T : Parcelable> Bundle.parcelableArrayList(key: String): ArrayList<T>? = when {
    Build.VERSION.SDK_INT >= TIRAMISU -> getParcelableArrayList(key, T::class.java)
    else -> @Suppress("DEPRECATION") getParcelableArrayList(key)
}

/**
 * Serializable extension to extract Parcelable array list from a Intent
 *
 * @param T deserialized type
 * @param key
 * @return the deserialized array list
 */
inline fun <reified T : Parcelable> Intent.parcelableArrayList(key: String): ArrayList<T>? = when {
    Build.VERSION.SDK_INT >= TIRAMISU -> getParcelableArrayListExtra(key, T::class.java)
    else -> @Suppress("DEPRECATION") getParcelableArrayListExtra(key)
}
