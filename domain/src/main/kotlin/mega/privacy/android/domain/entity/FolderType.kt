package mega.privacy.android.domain.entity

/**
 * Folder type
 *
 */
sealed interface FolderType {

    /**
     * Default
     */
    object Default : FolderType

    /**
     * MediaSyncFolder
     */
    object MediaSyncFolder : FolderType

    /**
     * ChatFilesFolder
     */
    object ChatFilesFolder : FolderType

    /**
     * Root backup folder
     */
    object RootBackup : FolderType

    /**
     * Non-Root backup folder
     */
    object ChildBackup : FolderType

    /**
     * Device Backup folder
     *
     * @property deviceType
     */
    data class DeviceBackup(val deviceType: DeviceType) : FolderType
}