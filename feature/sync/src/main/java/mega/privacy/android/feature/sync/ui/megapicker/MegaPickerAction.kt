package mega.privacy.android.feature.sync.ui.megapicker

import mega.privacy.android.domain.entity.node.TypedNode

internal sealed interface MegaPickerAction {

    data class FolderClicked(val folder: TypedNode) : MegaPickerAction

    object BackClicked : MegaPickerAction

    object CurrentFolderSelected : MegaPickerAction
}