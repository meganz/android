package mega.privacy.android.feature.texteditor.presentation

import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.entity.texteditor.TextEditorMode
import mega.privacy.android.feature.texteditor.presentation.model.TextEditorBottomBarAction
import javax.inject.Inject

/**
 * Maps node metadata into the list of actions to show in the text editor bottom floating bar.
 * Matches legacy text editor visibility rules; adapter-specific logic is passed in via the boolean flags.
 *
 * - **Download**: View mode, when [showDownload] (e.g. not offline or rubbish bin).
 * - **Get Link**: View mode, when not in excluded adapter, access is OWNER, and node is not exported.
 * - **Share**: View mode, when [showShare].
 * - **Edit**: View mode, when not in excluded adapter and access >= READWRITE.
 */
class TextEditorBottomBarActionsMapper @Inject constructor() {

    operator fun invoke(
        mode: TextEditorMode,
        accessPermission: AccessPermission?,
        isNodeExported: Boolean?,
        inExcludedAdapterForGetLinkAndEdit: Boolean,
        showDownload: Boolean,
        showShare: Boolean,
    ): List<TextEditorBottomBarAction> {
        if (mode != TextEditorMode.View) return emptyList()

        val showGetLink = !inExcludedAdapterForGetLinkAndEdit
                && accessPermission == AccessPermission.OWNER
                && isNodeExported == false

        val showEdit = !inExcludedAdapterForGetLinkAndEdit
                && (accessPermission == AccessPermission.OWNER
                || accessPermission == AccessPermission.FULL
                || accessPermission == AccessPermission.READWRITE)

        return buildList {
            if (showDownload) add(TextEditorBottomBarAction.Download)
            if (showGetLink) add(TextEditorBottomBarAction.GetLink)
            if (showShare) add(TextEditorBottomBarAction.Share)
            if (showEdit) add(TextEditorBottomBarAction.Edit)
        }
    }
}
