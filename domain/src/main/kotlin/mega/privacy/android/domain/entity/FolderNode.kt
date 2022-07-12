package mega.privacy.android.domain.entity

/**
 * Folder node
 */
interface FolderNode {
    /**
     * Is folder in the rubbish bin
     */
    val isInRubbishBin: Boolean

    /**
     * Is the folder an incoming share
     */
    val isIncomingShare: Boolean

    /**
     * Is the folder a media sync folder
     */
    val isMediaSyncFolder: Boolean

    /**
     * Is the folder the chat files folder
     */
    val isChatFilesFolder: Boolean

    /**
     * Is the folder an outgoing share
     */
    val isShared: Boolean

    /**
     * Is the folder a pending outgoing share
     */
    val isPendingShare: Boolean

    /**
     * Backup type information of the folder
     */
    val backupType: BackupType
}
