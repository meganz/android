package mega.privacy.android.app.presentation.fileinfo.view

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.PreviewParameter
import mega.privacy.android.app.presentation.fileinfo.model.FileInfoMenuAction
import mega.privacy.android.app.presentation.fileinfo.model.FileInfoViewState
import mega.privacy.android.core.ui.controls.appbar.LocalMegaAppBarTint
import mega.privacy.android.core.ui.controls.appbar.MegaAppBarTint
import mega.privacy.android.core.ui.controls.menus.MenuActions
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme

@Composable
internal fun FileInfoMenuActions(
    actions: List<FileInfoMenuAction>,
    tint: Color,
    onActionClick: (FileInfoMenuAction) -> Unit,
    enabled: Boolean = true,
) = CompositionLocalProvider(
    LocalMegaAppBarTint provides MegaAppBarTint(tint, tint)
) {
    MenuActions(
        actions = actions,
        maxActionsToShow = MENU_ACTIONS_TO_SHOW,
        onActionClick = {
            (it as FileInfoMenuAction).let(onActionClick)
        },
        enabled = enabled,
    )
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
                    actions = viewState.actions,
                    tint = MaterialTheme.colors.onSurface,
                    onActionClick = {}
                )
            }
        )
    }
}