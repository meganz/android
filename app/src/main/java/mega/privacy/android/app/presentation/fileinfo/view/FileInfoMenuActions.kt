package mega.privacy.android.app.presentation.fileinfo.view

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.MaterialTheme
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.fileinfo.model.FileInfoMenuAction
import mega.privacy.android.app.presentation.fileinfo.model.FileInfoOrigin
import mega.privacy.android.app.presentation.fileinfo.model.FileInfoViewState
import mega.privacy.android.core.ui.controls.MenuActions
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.domain.entity.shares.AccessPermission

@Composable
internal fun RowScope.FileInfoMenuActions(
    viewState: FileInfoViewState,
    tint: Color,
    onActionClick: (FileInfoMenuAction) -> Unit,
) = MenuActions(
    actions = getActions(viewState),
    maxActionsToShow = 3,
    dropDownIcon = painterResource(id = R.drawable.ic_dots_vertical_white),
    tint = tint,
    onActionClick = {
        (it as FileInfoMenuAction).let(onActionClick)
    }
)

private fun getActions(viewState: FileInfoViewState): List<FileInfoMenuAction> = with(viewState) {
    if (isNodeInRubbish) {
        listOf(FileInfoMenuAction.Delete)
    } else {
        val list = mutableListOf<FileInfoMenuAction>()
        if (!isTakenDown && isFile) {
            list.add(FileInfoMenuAction.SendToChat)
        }
        if (origin.fromInShares) {
            if (!isTakenDown) {
                list.add(FileInfoMenuAction.Download)
                if (origin == FileInfoOrigin.IncomingSharesFirstLevel) {
                    list.add(FileInfoMenuAction.Leave)
                }
                list.add(FileInfoMenuAction.Copy)
            }
            if (accessPermission == AccessPermission.OWNER || accessPermission == AccessPermission.FULL) {
                if (origin == FileInfoOrigin.IncomingSharesFirstLevel) {
                    list.add(FileInfoMenuAction.MoveToRubbishBin)
                }
                list.add(FileInfoMenuAction.Rename)
            }
        } else {
            if (!isTakenDown) {
                list.add(FileInfoMenuAction.Download)
                if (!isFile) {
                    list.add(FileInfoMenuAction.ShareFolder)
                }
                if (isExported) {
                    list.add(FileInfoMenuAction.ManageLink)
                    list.add(FileInfoMenuAction.RemoveLink)
                } else {
                    list.add(FileInfoMenuAction.GetLink)
                }
                list.add(FileInfoMenuAction.Copy)
            } else {
                list.add(FileInfoMenuAction.DisputeTakedown)
            }
            list.add(FileInfoMenuAction.MoveToRubbishBin)
            list.add(FileInfoMenuAction.Rename)
            list.add(FileInfoMenuAction.Move)
        }
        list
    }
}

@CombinedThemePreviews
@Composable
private fun MenuActionsPreview(
    @PreviewParameter(FileInfoViewStatePreviewsProvider::class) viewState: FileInfoViewState,
) {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        TopAppBar(
            title = {},
            actions = {
                FileInfoMenuActions(
                    viewState = viewState,
                    tint = MaterialTheme.colors.onSurface,
                    onActionClick = {}
                )
            }
        )
    }
}