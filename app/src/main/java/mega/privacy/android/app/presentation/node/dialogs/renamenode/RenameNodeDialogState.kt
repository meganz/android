package mega.privacy.android.app.presentation.node.dialogs.renamenode

import androidx.annotation.StringRes
import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed

internal data class RenameNodeDialogState(
    val nodeName: String? = null,
    @StringRes val errorMessage: Int? = null,
    val showChangeNodeExtensionDialogEvent: StateEventWithContent<String> = consumed(),
    val renameValidationPassedEvent: StateEvent = consumed,
)