package mega.privacy.android.domain.entity.zipbrowser

/**
 * The zip entry type
 */
enum class ZipEntryType {
    /**
     * The zip entry is zip file
     */
    Zip,

    /**
     * The zip entry is file
     */
    File,

    /**
     * The zip entry is folder
     */
    Folder
}