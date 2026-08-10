package mega.privacy.android.data.extensions

import android.net.Uri
import mega.privacy.android.domain.entity.uri.UriPath
import timber.log.Timber
import java.io.File


fun UriPath.toUri(): Uri {
    return runCatching { Uri.parse(this.value) }
        .onFailure { Timber.e(it) }
        .getOrNull()?.takeUnless { it.scheme.isNullOrEmpty() }
        ?: Uri.fromFile(File(this.value))
}

/**
 * @return true if this UriPath represents a file, false otherwise
 */
fun UriPath.isFile() = toUri().scheme == "file"

/**
 * Creates a [UriPath] from a [Uri].
 *
 */
fun Uri.toUriPath() = UriPath(toString())

/**
 * Creates a [UriPath] from a [String], it will add file scheme in case of file path
 */
fun String.toUriPath() = UriPath(this).toUri().toUriPath()