package mega.privacy.android.app.presentation.videosection

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import mega.privacy.android.app.R
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.legacy.core.ui.controls.LegacyMegaEmptyView

@Composable
internal fun VideoPlaylistsView(
    modifier: Modifier
) {
    LegacyMegaEmptyView(
        modifier = modifier,
        text = "[B]No[/B] [A]playlists[/A] [B]found[/B]",
        imagePainter = painterResource(id = R.drawable.ic_homepage_empty_playlists)
    )
}

@CombinedThemePreviews
@Composable
private fun VideoPlaylistsViewPreview() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        VideoPlaylistsView(Modifier)
    }
}