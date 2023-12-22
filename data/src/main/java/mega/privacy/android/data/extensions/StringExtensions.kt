package mega.privacy.android.data.extensions

import android.util.Base64
import java.io.File

/**
 * Encode String to Base64
 */
fun String.encodeBase64(): String =
    Base64.encodeToString(this.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)

/**
 * Removes the first elements already contained in [repeated] list respecting order.
 * It only removes the elements until one is not equals.
 *
 * Example
 * listOf("a","b", "c").dropRepeated(listOf("a","b")) will return a list containing only "c"
 */
fun List<String>.dropRepeated(repeated: List<String>): List<String> {
    return this.indices.dropWhile { index ->
        this[index] == repeated.getOrNull(index)
    }.map { this[it] }
}

/**
 * Return a list of strings representing all the folders of this path
 */
fun String.getAllFolders() = this.split(File.separator).filter { it.isNotBlank() }