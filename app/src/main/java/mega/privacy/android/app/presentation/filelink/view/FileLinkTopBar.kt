package mega.privacy.android.app.presentation.filelink.view

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import mega.privacy.android.app.R
import mega.privacy.android.shared.theme.MegaAppTheme
import mega.privacy.android.core.ui.controls.appbar.AppBarForCollapsibleHeader
import mega.privacy.android.core.ui.controls.appbar.AppBarType
import mega.privacy.android.core.ui.model.MenuActionWithIcon
import mega.privacy.android.core.ui.preview.CombinedTextAndThemePreviews

internal const val SHARE_BUTTON_TAG = "file_link_top_bar:icon_share"

@Composable
internal fun FileLinkTopBar(
    title: String,
    onBackPressed: () -> Unit,
    onShareClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AppBarForCollapsibleHeader(
        appBarType = AppBarType.BACK_NAVIGATION,
        title = title,
        onNavigationPressed = onBackPressed,
        actions = listOf(ShareMenuAction()),
        onActionPressed = {
            onShareClicked()
        },
        modifier = modifier.fillMaxWidth()
    )
}

private class ShareMenuAction : MenuActionWithIcon {
    @Composable
    override fun getIconPainter() = painterResource(id = R.drawable.ic_social_share_white)

    @Composable
    override fun getDescription() = stringResource(id = R.string.general_share)

    override val testTag = SHARE_BUTTON_TAG

}

@CombinedTextAndThemePreviews
@Composable
private fun FileInfoTopBarPreview() {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        FileLinkTopBar(
            title = "Title",
            onBackPressed = {},
            onShareClicked = {},
        )
    }
}