package mega.privacy.android.domain.entity

/**
 * Folder type
 *
 */
sealed interface FolderType {

    /**
     * Default
     */
    data object Default : FolderType

    /**
     * MediaSyncFolder
     */
    data object MediaSyncFolder : FolderType

    /**
     * ChatFilesFolder
     */
    data object ChatFilesFolder : FolderType

    /**
     * Root backup folder
     */
    data object RootBackup : FolderType

    /**
     * Non-Root backup folder
     */
    data object ChildBackup : FolderType

    /**
     * Device Backup folder
     *
     * @property deviceType
     */
    data class DeviceBackup(val deviceType: DeviceType) : FolderType

    /**
     * Synced folder
     */
    data object Sync : FolderType
}