package mega.privacy.android.domain.entity.node

/**
 * Warning to surface before sharing folder(s) that are hidden/sensitive.
 *
 * Sharing a hidden folder makes it visible to the recipient, so the user is warned first. The
 * variant selects the wording (single folder vs multiple folders); [None] means no warning is needed.
 */
enum class SensitiveNodeShareWarning {
    /**
     * No warning needed — nothing being shared is hidden/sensitive (or hidden nodes are not
     * enabled for this account).
     */
    None,

    /**
     * A single hidden/sensitive folder is being shared.
     */
    Folder,

    /**
     * Multiple folders are being shared and at least one is hidden/sensitive.
     */
    Folders,
}
