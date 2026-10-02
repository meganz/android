package mega.privacy.android.domain.entity.zipbrowser

/**
 * A single entry read from the central directory of an archive. Nothing here requires the entry to
 * have been decompressed, so a whole archive can be described without writing anything to disk.
 *
 * @property index position in the central directory. Unique within one archive and used as the
 * stable identity of the entry; entry paths are not usable as identities because two different
 * paths can share a hash code.
 * @property path full entry path inside the archive, without a trailing separator
 * @property name display name, the last segment of [path]
 * @property parentPath path of the containing folder, null at the archive root
 * @property size uncompressed size in bytes
 * @property modificationTime last modified time in milliseconds since the epoch
 * @property isDirectory whether the entry is a folder, declared by the archive or implied by the
 * path of another entry
 * @property isEncrypted whether this entry's content is encrypted
 * @property isNameDecodable false when none of the supported charsets produced a usable name
 */
data class ZipArchiveEntry(
    val index: Int,
    val path: String,
    val name: String,
    val parentPath: String?,
    val size: Long,
    val modificationTime: Long,
    val isDirectory: Boolean,
    val isEncrypted: Boolean,
    val isNameDecodable: Boolean,
) {
    val isNestedArchive: Boolean
        get() = !isDirectory && name.endsWith(ZIP_SUFFIX, ignoreCase = true)

    private companion object {
        const val ZIP_SUFFIX = ".zip"
    }
}
