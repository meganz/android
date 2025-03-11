package mega.privacy.android.feature.devicecenter.domain.entity

/**
 * Sealed class representing the Status of each Device in Device Center
 *
 * @property priority When determining the Device Status to be displayed from the list of Backup
 * Folders, this decides what Status to be displayed. The higher the number, the more
 * that Status is prioritized
 */
sealed class DeviceStatus(val priority: Int) {

    /**
     * The default value assigned when prioritizing what Device Status should be displayed and none
     * is found
     */
    data object Unknown : DeviceStatus(0)

    /**
     * The Device has nothing set up yet
     */
    data object NothingSetUp : DeviceStatus(1)

    /**
     * The Device is up to date
     */
    data object UpToDate : DeviceStatus(2)

    /**
     * The Device is updating
     */
    data object Updating : DeviceStatus(3)

    /**
     * The Device requires some attention
     */
    data object AttentionNeeded : DeviceStatus(4)

    /**
     * The Device is inactive
     */
    data object Inactive : DeviceStatus(5)
}