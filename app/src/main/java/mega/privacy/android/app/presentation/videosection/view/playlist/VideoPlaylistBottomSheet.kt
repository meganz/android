package mega.privacy.android.app.presentation.videosection.view.playlist

import mega.privacy.android.icon.pack.R as iconPackR
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mega.privacy.android.core.ui.controls.lists.MenuActionListTile
import mega.privacy.android.core.ui.controls.sheets.BottomSheet
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.theme.MegaAppTheme

@OptIn(ExperimentalMaterialApi::class)
@Composable
internal fun VideoPlaylistBottomSheet(
    modalSheetState: ModalBottomSheetState,
    coroutineScope: CoroutineScope,
    onRenameVideoPlaylistClicked: () -> Unit,
    onDeleteVideoPlaylistClicked: () -> Unit,
) {
    BottomSheet(modalSheetState = modalSheetState,
        sheetBody = {
            VideoPlaylistBottomSheetBody(
                onRenameVideoPlaylistClicked = {
                    coroutineScope.launch { modalSheetState.hide() }
                    onRenameVideoPlaylistClicked()
                },
                onDeleteVideoPlaylistClicked = {
                    coroutineScope.launch { modalSheetState.hide() }
                    onDeleteVideoPlaylistClicked()
                }
            )
        }
    )
}

@Composable
internal fun VideoPlaylistBottomSheetBody(
    onRenameVideoPlaylistClicked: () -> Unit,
    onDeleteVideoPlaylistClicked: () -> Unit,
) {
    Column {
        RenameVideoPlaylistBottomSheetTile(onRenameVideoPlaylistClicked)
        DeleteVideoPlaylistBottomSheetTile(onDeleteVideoPlaylistClicked)
    }
}

@Composable
internal fun RenameVideoPlaylistBottomSheetTile(
    onActionClicked: () -> Unit,
) {
    MenuActionListTile(
        dividerType = null,
        text = "Rename",
        icon = painterResource(id = iconPackR.drawable.ic_pen_02_medium_regular_outline),
        onActionClicked = onActionClicked,
    )
}

@Composable
internal fun DeleteVideoPlaylistBottomSheetTile(
    onActionClicked: () -> Unit,
) {
    MenuActionListTile(
        dividerType = null,
        text = "Delete playlist",
        icon = painterResource(id = iconPackR.drawable.ic_trash_medium_regular_outline),
        onActionClicked = onActionClicked,
    )
}

@CombinedThemePreviews
@Composable
private fun VideoPlaylistBottomSheetBodyPreview() {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        VideoPlaylistBottomSheetBody(
            onRenameVideoPlaylistClicked = {},
            onDeleteVideoPlaylistClicked = {}
        )
    }
}

@CombinedThemePreviews
@Composable
private fun RenameVideoPlaylistBottomSheetTilePreview() {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        RenameVideoPlaylistBottomSheetTile(onActionClicked = {})
    }
}

@CombinedThemePreviews
@Composable
private fun DeleteVideoPlaylistBottomSheetTilePreview() {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        DeleteVideoPlaylistBottomSheetTile(onActionClicked = {})
    }
}