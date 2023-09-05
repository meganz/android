package mega.privacy.android.app.presentation.backups.model

import mega.privacy.android.domain.entity.preference.ViewType
import nz.mega.sdk.MegaNode

/**
 * The Backups UI State
 *
 * @property currentViewType The current [ViewType]
 * @property hideMultipleItemSelection Whether to hide the Multiple Item Selection or not
 * @property backupsHandle The current Backups Handle
 * @property nodes The list of Backups Nodes
 * @property shouldExitBackups Whether the User should leave the Backups page or not
 * @property triggerBackPress Whether the User has triggered a Back Press behavior or not
 * @property isPendingRefresh Whether a refresh of the Backup Contents is needed or not
 */
data class BackupsState(
    val currentViewType: ViewType = ViewType.LIST,
    val hideMultipleItemSelection: Boolean = false,
    val backupsHandle: Long = -1L,
    val nodes: List<MegaNode> = emptyList(),
    val shouldExitBackups: Boolean = false,
    val triggerBackPress: Boolean = false,
    val isPendingRefresh: Boolean = false,
)