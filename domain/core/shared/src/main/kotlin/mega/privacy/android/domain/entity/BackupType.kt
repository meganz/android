package mega.privacy.android.domain.entity

/**
 * Backup type
 */
sealed interface BackupType {
    /**
     * Not a backup folder
     */
    object None : BackupType

    /**
     * Root backup folder
     */
    object Root : BackupType

    /**
     * Non-Root backup folder
     */
    object Child : BackupType

    /**
     * Device Backup folder
     *
     * @property deviceType
     */
    class Device(val deviceType: DeviceType) : BackupType
}
