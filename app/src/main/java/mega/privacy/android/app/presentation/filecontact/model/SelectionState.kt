package mega.privacy.android.app.presentation.filecontact.model

/**
 * Selection state
 *
 * @property selectedCount
 * @property allSelected
 */
internal data class SelectionState(
    val selectedCount: Int,
    val allSelected: Boolean,
)