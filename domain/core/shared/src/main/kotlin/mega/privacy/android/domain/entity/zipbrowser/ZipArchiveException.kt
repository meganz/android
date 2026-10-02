package mega.privacy.android.domain.entity.zipbrowser

/**
 * Every failure the zip browser can report, so a message can name what actually went wrong instead
 * of blaming the file format for an encryption or storage problem.
 */
sealed class ZipArchiveException(message: String? = null, cause: Throwable? = null) :
    Exception(message, cause) {

    /**
     * The archive cannot be read: truncated, not an archive, or gone from disk.
     */
    class Corrupted(cause: Throwable? = null) : ZipArchiveException(cause = cause)

    class UnsupportedFormat(val extension: String) :
        ZipArchiveException(message = "Unsupported archive format: $extension")

    /**
     * The entry is encrypted and no password has been supplied yet.
     */
    data object PasswordRequired : ZipArchiveException()

    data object WrongPassword : ZipArchiveException()

    class InsufficientStorage(val requiredBytes: Long, val availableBytes: Long) :
        ZipArchiveException(
            message = "Needs $requiredBytes bytes, $availableBytes available"
        )

    class EntryNameUndecodable(val rawName: String) : ZipArchiveException()

    /**
     * One entry failed while the rest of the archive is fine, so the failure can be reported
     * against that entry rather than against the archive.
     */
    class EntryExtractionFailed(val entryPath: String, cause: Throwable? = null) :
        ZipArchiveException(message = "Could not extract $entryPath", cause = cause)

    /**
     * Part of a cloud archive could not be read, usually because the connection dropped. Unlike
     * [Corrupted] it is worth retrying.
     */
    class RemoteReadFailed(cause: Throwable? = null) : ZipArchiveException(cause = cause)
}
