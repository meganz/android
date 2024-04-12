package mega.privacy.android.app.presentation.clouddrive

import androidx.annotation.StringRes

/**
 * Interface that defines what methods the Activity should implement for [FileBrowserComposeFragment]
 */
interface FileBrowserActionListener {

    /**
     * Exits the [FileBrowserComposeFragment]
     */
    fun exitCloudDrive()

    /**
     * Refreshes the Toolbar Title for Cloud Drive
     *
     * @param invalidateOptionsMenu If true, this invalidates the Options Menu
     */
    fun updateCloudDriveToolbarTitle(invalidateOptionsMenu: Boolean)

    /**
     * Shows the Media Discovery
     *
     * @param mediaHandle The Folder Handle that contains the Media to be displayed on that page
     * @param isAccessedByIconClick true if Media Discovery is accessed by clicking the Media
     * Discovery Icon
     * @param errorMessage The [StringRes] of the error message to display
     */
    suspend fun showMediaDiscoveryFromCloudDrive(
        mediaHandle: Long,
        isAccessedByIconClick: Boolean,
        replaceFragment: Boolean,
        @StringRes errorMessage: Int?,
    )
}