package mega.privacy.android.domain.entity.uri

import kotlinx.serialization.Serializable
import java.io.File

/**
 * A value class that encapsulates a string representation of an Android Uri or file path.
 *
 * @property value the string representation of the Uri or path
 */

@JvmInline
@Serializable
value class UriPath(val value: String){
    companion object {
        /**
         * Helper function to get a [UriPath] from a [File].
         *
         * It's preferable to work with `content` [Uri] whenever possible (like `content://com.android.externalstorage.documents/tree/`) but for compatibility reasons we can use file path.
         */
        fun fromFile(file: File) = UriPath(file.absolutePath)
    }
}