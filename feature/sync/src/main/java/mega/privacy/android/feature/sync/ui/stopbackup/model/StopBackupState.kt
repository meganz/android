package mega.privacy.android.feature.sync.ui.stopbackup.model

import mega.privacy.android.feature.sync.domain.entity.RemoteFolder

/**
 * State of StopBackupConfirmationDialog
 * @param selectedMegaFolder Selected MEGA folder
 */
internal data class StopBackupState(
    val selectedMegaFolder: RemoteFolder? = null,
)