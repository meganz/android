package mega.privacy.android.feature.sync.ui.model.exclusion

/**
 * What the Edit exclusion by-size screen shows.
 *
 * The bounds belong to the file rather than to a rule, so unlike the by-name screen there is
 * nothing here identifying which entry is being edited.
 *
 * @property canSave whether the form declares bounds the file could hold. Read from the mapper that
 * builds them rather than judged here, so a save cannot close the screen having staged nothing
 */
internal data class EditSyncSizeExclusionUiState(
    val bySize: AddExclusionBySizeFormState = AddExclusionBySizeFormState(),
    val canSave: Boolean = false,
)
