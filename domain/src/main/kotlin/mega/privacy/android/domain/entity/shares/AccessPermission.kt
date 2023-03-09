package mega.privacy.android.domain.entity.shares

/**
 * Enum class defining access permissions for incoming shares
 */
enum class AccessPermission {
    /**
     * permission unknown
     */
    UNKNOWN,

    /**
     * read permission
     */
    READ,

    /**
     * read and write permission
     */
    READWRITE,

    /**
     * full permission
     */
    FULL,

    /**
     * the user is the owner of the node
     */
    OWNER
}