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
import mega.privacy.android.app.presentation.fileinfo.model.FileInfoViewState
import mega.privacy.android.core.ui.controls.menus.MenuActions
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme

@Composable
internal fun RowScope.FileInfoMenuActions(
    actions: List<FileInfoMenuAction>,
    tint: Color,
    onActionClick: (FileInfoMenuAction) -> Unit,
    enabled: Boolean = true,
) = MenuActions(
    actions = actions,
    maxActionsToShow = MENU_ACTIONS_TO_SHOW,
    dropDownIcon = painterResource(id = R.drawable.ic_dots_vertical_white),
    tint = tint,
    onActionClick = {
        (it as FileInfoMenuAction).let(onActionClick)
    },
    enabled = enabled,
)

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
                    actions = viewState.actions,
                    tint = MaterialTheme.colors.onSurface,
                    onActionClick = {}
                )
            }
        )
    }
}