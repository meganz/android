package mega.privacy.android.core.nodecomponents.sheet.nodeactions

import androidx.compose.ui.graphics.vector.ImageVector
import mega.android.core.ui.model.LocalizedText
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.shared.resources.R as sharedR


sealed class NodeActionUiOption(
    val key: String,
    val label: LocalizedText,
    val icon: ImageVector,
    val textColor: TextColor = TextColor.Primary,
    val showHelpButton: Boolean = false,
) {
    object SaveToDevice : NodeActionUiOption(
        key = SaveToDevice::class.java.simpleName,
        label = LocalizedText.StringRes(sharedR.string.general_save_to_device),
        icon = IconPack.Medium.Thin.Outline.Download,
    )

    data class ShareLink(val size: Int) : NodeActionUiOption(
        key = ShareLink::class.java.simpleName,
        label = LocalizedText.PluralsRes(
            resId = sharedR.plurals.label_share_links,
            quantity = size
        ),
        icon = IconPack.Medium.Thin.Outline.Link01,
    )

    object Hide : NodeActionUiOption(
        key = Hide::class.java.simpleName,
        label = LocalizedText.StringRes(sharedR.string.general_hide_node),
        icon = IconPack.Medium.Thin.Outline.EyeOff,
        showHelpButton = true,
    )

    object Move : NodeActionUiOption(
        key = Move::class.java.simpleName,
        label = LocalizedText.StringRes(sharedR.string.general_move),
        icon = IconPack.Medium.Thin.Outline.Move,
    )

    object Copy : NodeActionUiOption(
        key = Copy::class.java.simpleName,
        label = LocalizedText.StringRes(sharedR.string.general_copy),
        icon = IconPack.Medium.Thin.Outline.Copy01,
    )

    object MoveToRubbishBin : NodeActionUiOption(
        key = MoveToRubbishBin::class.java.simpleName,
        label = LocalizedText.StringRes(sharedR.string.node_option_move_to_rubbish_bin),
        icon = IconPack.Medium.Thin.Outline.Trash,
        textColor = TextColor.Error,
    )

    companion object {
        val defaults = getDefaults(1)

        fun getDefaults(size: Int) = listOf(
            SaveToDevice,
            ShareLink(size),
            Hide,
            Move,
            Copy,
            MoveToRubbishBin,
        )
    }
}