package mega.privacy.android.app.presentation.fileinfo

/**
 * Enum class to set from which origin the information screen was opened
 * @param fromInShares convenient field to group incoming shares
 */
enum class FileInfoOrigin(val fromInShares: Boolean = false) {
    /**
     * From first level in incoming shares
     */
    IncomingSharesFirstLevel(true),

    /**
     * From incoming shares, but not first level
     */
    IncomingSharesOtherLevel(true),

    /**
     * From outgoing shares
     */
    OutgoingShares,

    /**
     * Other source, not relevant which one for this screen
     */
    Other;
}
