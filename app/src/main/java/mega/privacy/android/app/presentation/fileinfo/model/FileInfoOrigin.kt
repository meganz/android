package mega.privacy.android.app.presentation.fileinfo.model

/**
 * Enum class to set from which origin the information screen was opened
 */
enum class FileInfoOrigin {
    /**
     * From first level in incoming shares
     */
    IncomingSharesFirstLevel,

    /**
     * From incoming shares, but not first level
     */
    IncomingSharesOtherLevel,

    /**
     * From outgoing shares
     */
    OutgoingShares,

    /**
     * Other source, not relevant which one for this screen
     */
    Other;

    /**
     * convenient field to group shares
     */

    val fromShares: Boolean
        get() = this != Other

    /**
     * convenient field to group incoming shares
     */
    val fromInShares: Boolean
        get() = this == IncomingSharesFirstLevel || this == IncomingSharesOtherLevel
}
