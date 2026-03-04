package mega.privacy.android.app.textEditor

import mega.privacy.android.app.utils.Constants
import mega.privacy.android.core.nodecomponents.model.NodeSourceTypeInt
import mega.privacy.android.domain.entity.texteditor.TextEditorMode
import mega.privacy.android.feature.texteditor.presentation.model.DefaultTextEditorTopBarSlots
import mega.privacy.android.feature.texteditor.presentation.model.TextEditorConditionalTopBarAction
import mega.privacy.android.feature.texteditor.presentation.model.TextEditorTopBarSlot
import mega.privacy.android.feature.texteditor.presentation.model.TextEditorTopBarSlots

/**
 * Computes the ordered list of top bar slots to show in the Compose text editor
 * based on [nodeSourceType] and [mode], matching legacy [TextEditorActivity] menu visibility rules
 * where only adapter type and mode are available.
 *
 * **Why in app:** This function uses [Constants] (adapter-type ints) and [NodeSourceTypeInt];
 * the text-editor feature module must not depend on app, so the logic stays here and the
 * computed [TextEditorTopBarSlots] are passed into the feature.
 *
 * **Out of scope for this resolver:** The legacy "else" branch (cloud/shares/links) uses node,
 * getNodeAccess(), isExported, isInShare, rootParentNode, etc. to hide GetLink for non-owner /
 * ACCESS_FULL/READ and Share for incoming share. We only have [nodeSourceType] and [mode], so
 * that fine-grained logic is not implemented; we use [DefaultTextEditorTopBarSlots] for the
 * else case. A future phase may pass node/access or a use case to refine visibility.
 */
fun computeTextEditorTopBarSlots(
    nodeSourceType: Int?,
    mode: TextEditorMode,
): TextEditorTopBarSlots {
    if (mode == TextEditorMode.Edit || mode == TextEditorMode.Create) {
        return listOf(TextEditorTopBarSlot.LineNumbers, TextEditorTopBarSlot.More)
    }
    // View mode
    return when (nodeSourceType) {
        Constants.OFFLINE_ADAPTER ->
            listOf(
                TextEditorTopBarSlot.LineNumbers,
                TextEditorTopBarSlot.Conditional(TextEditorConditionalTopBarAction.Share),
                TextEditorTopBarSlot.More,
            )

        NodeSourceTypeInt.RUBBISH_BIN_ADAPTER ->
            listOf(TextEditorTopBarSlot.LineNumbers, TextEditorTopBarSlot.More)

        Constants.FILE_LINK_ADAPTER, Constants.ZIP_ADAPTER ->
            listOf(
                TextEditorTopBarSlot.Conditional(TextEditorConditionalTopBarAction.Download),
                TextEditorTopBarSlot.LineNumbers,
                TextEditorTopBarSlot.Conditional(TextEditorConditionalTopBarAction.SendToChat),
                TextEditorTopBarSlot.Conditional(TextEditorConditionalTopBarAction.Share),
                TextEditorTopBarSlot.More,
            )

        Constants.FOLDER_LINK_ADAPTER, Constants.VERSIONS_ADAPTER, Constants.FROM_CHAT ->
            listOf(
                TextEditorTopBarSlot.Conditional(TextEditorConditionalTopBarAction.Download),
                TextEditorTopBarSlot.LineNumbers,
                TextEditorTopBarSlot.More,
            )

        NodeSourceTypeInt.INCOMING_SHARES_ADAPTER ->
            listOf(
                TextEditorTopBarSlot.Conditional(TextEditorConditionalTopBarAction.Download),
                TextEditorTopBarSlot.LineNumbers,
                TextEditorTopBarSlot.Conditional(TextEditorConditionalTopBarAction.SendToChat),
                TextEditorTopBarSlot.Conditional(TextEditorConditionalTopBarAction.Share),
                TextEditorTopBarSlot.More,
            )

        else -> DefaultTextEditorTopBarSlots
    }
}
