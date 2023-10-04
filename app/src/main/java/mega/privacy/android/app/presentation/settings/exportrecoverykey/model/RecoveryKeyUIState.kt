package mega.privacy.android.app.presentation.settings.exportrecoverykey.model

import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import java.io.File

/**
 * Recovery Key UI State
 * @param isActionGroupVertical - The orientation for the Button group
 * @param message - The SnackBar message, null will hide the SnackBar
 * @param printRecoveryKey - Event to print the recovery key
 */
data class RecoveryKeyUIState(
    val isActionGroupVertical: Boolean = false,
    val message: String? = null,
    val printRecoveryKey: StateEventWithContent<File?> = consumed(),
)