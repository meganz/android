package mega.privacy.android.feature.devicecenter.ui.bottomsheet

/**
 * Interface that defines Bottom Sheet actions that the extending Activity should perform
 */
interface DeviceCenterBottomSheetActionListener {

    /**
     * Triggered when the "Info" Option in the Device Center Bottom Sheet is clicked
     *
     * @param backupsHandle The Backups Node Handle
     * @param backupsName The Backups Node Name
     */
    fun onDeviceCenterBackupsInfoClicked(backupsHandle: Long, backupsName: String)

    /**
     * Triggered when the "Save to device" Option in the Device Center Bottom Sheet is clicked
     *
     * @param nodeHandle The Node Handle
     */
    fun onDeviceCenterSaveToDeviceClicked(nodeHandle: Long)
}