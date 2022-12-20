package mega.privacy.android.app.presentation.clouddrive.model

import mega.privacy.android.app.presentation.settings.model.MediaDiscoveryViewSettings

/**
 * File browser UI state
 *
 * @param fileBrowserHandle current file browser handle
 * @param mediaDiscoveryViewSettings current settings for displaying discovery view
 */
data class FileBrowserState(
    val fileBrowserHandle: Long = -1L,
    val mediaDiscoveryViewSettings: Int = MediaDiscoveryViewSettings.INITIAL.ordinal
)
