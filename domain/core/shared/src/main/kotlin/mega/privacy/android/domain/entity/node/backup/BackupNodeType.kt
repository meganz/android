package mega.privacy.android.domain.entity.node.backup

/**
 * Different types of backup type
 */
enum class BackupNodeType {
    /**
     * The Node is not a Backup Node
     */
    NonBackupNode,

    /**
     * The Backup Node is the Root Backup Node
     */
    RootNode,

    /**
     * The Backup Node is a Device Node
     */
    DeviceNode,

    /**
     * The Backup Node is a Folder Node
     */
    FolderNode,

    /**
     * The Backup Node is a Child Folder Node
     */
    ChildFolderNode,
}