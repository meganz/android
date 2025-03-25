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

fun UriPath.isPath(): Boolean = value.startsWith("file").not() && isFile()

fun UriPath.fromUri(uri: Uri) = UriPath(uri.toString())