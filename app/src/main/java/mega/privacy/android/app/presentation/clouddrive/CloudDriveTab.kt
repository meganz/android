package mega.privacy.android.app.presentation.clouddrive

/**
 * Define the different tab values for the CloudDrive and Syncs screen
 *
 * @property position the position of the tab
 */
enum class CloudDriveTab(val position: Int) {

    /**
     * None
     */
    NONE(-1),

    /**
     * Cloud Drive tab
     */
    CLOUD(0),

    /**
     * Syncs  tab
     */
    SYNC(1),
}
