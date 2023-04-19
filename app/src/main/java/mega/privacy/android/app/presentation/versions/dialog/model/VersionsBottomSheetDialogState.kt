package mega.privacy.android.app.presentation.versions.dialog.model

import nz.mega.sdk.MegaNode

/**
 * Data class representing the UI State for
 * [mega.privacy.android.app.modalbottomsheet.VersionsBottomSheetDialogFragment]
 *
 * @property node The Node used for the Dialog
 */
data class VersionsBottomSheetDialogState(
    val node: MegaNode? = null,
)
