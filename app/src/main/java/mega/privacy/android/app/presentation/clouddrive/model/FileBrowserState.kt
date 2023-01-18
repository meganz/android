package mega.privacy.android.app.presentation.clouddrive.model

import mega.privacy.android.app.presentation.settings.model.MediaDiscoveryViewSettings
import nz.mega.sdk.MegaNode

/**
 * File browser UI state
 *
 * @property fileBrowserHandle current file browser handle
 * @property mediaDiscoveryViewSettings current settings for displaying discovery view
 * @property nodes List of FileBrowser Nodes
 * @property parentHandle Parent Handle of current Node
 * @property mediaHandle MediaHandle of current Node
 */
data class FileBrowserState(
    val fileBrowserHandle: Long = -1L,
    val mediaDiscoveryViewSettings: Int = MediaDiscoveryViewSettings.INITIAL.ordinal,
    val nodes: List<MegaNode> = emptyList(),
    val parentHandle: Long? = null,
    val mediaHandle: Long = -1L
)
