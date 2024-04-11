package mega.privacy.android.feature.sync.ui.megapicker

import mega.privacy.android.domain.entity.node.TypedNode

internal sealed interface MegaPickerAction {

    data class FolderClicked(val folder: TypedNode) : MegaPickerAction

    data object BackClicked : MegaPickerAction

    data class CurrentFolderSelected(
        val allFilesAccessPermissionGranted: Boolean,
        val disableBatteryOptimizationPermissionGranted: Boolean,
    ) : MegaPickerAction

    data object ErrorMessageShown : MegaPickerAction

    data object AllFilesAccessPermissionDialogShown : MegaPickerAction

    data object DisableBatteryOptimizationsDialogShown : MegaPickerAction

    data object NextScreenOpened : MegaPickerAction
}