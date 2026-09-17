package mega.privacy.android.feature.sync.ui.model.exclusion

/**
 * What the Edit exclusion by-size screen shows.
 *
 * The bounds belong to the file rather than to a rule, so unlike the by-name screen there is
 * nothing here identifying which entry is being edited.
 */
internal data class EditSyncSizeExclusionUiState(
    val bySize: AddExclusionBySizeFormState = AddExclusionBySizeFormState(),
) {

    /**
     * Whether the form is holding bounds that cannot be saved. The primary button stays enabled
     * and refuses the tap, rather than going grey, matching the Add screen.
     */
    val showsBlockingError: Boolean
        get() = bySize.showsRangeError || bySize.hasUnusableBound
}
