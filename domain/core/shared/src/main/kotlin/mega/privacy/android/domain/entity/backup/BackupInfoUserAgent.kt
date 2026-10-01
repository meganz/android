package mega.privacy.android.domain.entity.backup

/**
 * Enum class that represents the different User Agents mapped from
 * nz.mega.sdk.MegaBackupInfo.deviceUserAgent
 */
enum class BackupInfoUserAgent {

    /**
     * Represents an Unknown User Agent. This is the default value returned when there is no
     * matching User Agent from the SDK
     */
    UNKNOWN,

    /**
     * The Backup originated from a Device running the Windows OS
     */
    WINDOWS,

    /**
     * The Backup originated from a Device running the Linux OS
     */
    LINUX,

    /**
     * The Backup originated from a Mac Device
     */
    MAC,

    /**
     * The Backup originated from an Android Device
     */
    ANDROID,

    /**
     * The Backup originated from an iPhone Device
     */
    IPHONE,
}