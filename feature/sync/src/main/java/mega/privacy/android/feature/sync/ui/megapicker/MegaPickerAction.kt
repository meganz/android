package mega.privacy.android.feature.sync.ui.megapicker

import mega.privacy.android.domain.entity.node.TypedNode

internal sealed interface MegaPickerAction {

    data class FolderClicked(val folder: TypedNode) : MegaPickerAction

    data object BackClicked : MegaPickerAction

    data class CurrentFolderSelected(
        val allFilesAccessPermissionGranted: Boolean,
        val disableBatteryOptimizationPermissionGranted: Boolean,
    ) : MegaPickerAction

    /**
     * Action triggered when the snackbar message has been shown
     */
    data object SnackbarShown : MegaPickerAction

    data object AllFilesAccessPermissionDialogShown : MegaPickerAction

    data object DisableBatteryOptimizationsDialogShown : MegaPickerAction

    data object NextScreenOpened : MegaPickerAction

    /**
     * Action triggered when a disabled folder is clicked
     */
    data class DisabledFolderClicked(val node: TypedNodeUiModel) : MegaPickerAction

    /**
     * Action triggered when the user confirms the folder connection removal
     */
    data object RemoveConnectionConfirmed : MegaPickerAction

    /**
     * Action triggered when the remove connection dialog is dismissed
     */
    data object RemoveConnectionDialogDismissed : MegaPickerAction
}
