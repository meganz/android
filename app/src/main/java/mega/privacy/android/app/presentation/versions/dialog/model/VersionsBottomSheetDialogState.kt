package mega.privacy.android.app.presentation.versions.dialog.model

import nz.mega.sdk.MegaNode

/**
 * Data class representing the UI State for
 * [mega.privacy.android.app.modalbottomsheet.VersionsBottomSheetDialogFragment]
 *
 * @property canDeleteVersion Checks whether this specific [node] Version can be deleted or not
 * @property canRevertVersion Checks whether this specific [node] Version can be reverted or not
 * @property node The Node used for the Dialog
 */
data class VersionsBottomSheetDialogState(
    val canDeleteVersion: Boolean = false,
    val canRevertVersion: Boolean = false,
    val node: MegaNode? = null,
)
