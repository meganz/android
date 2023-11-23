package mega.privacy.android.app.presentation.videosection

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.videosection.model.UIVideo
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.legacy.core.ui.controls.LegacyMegaEmptyView

@Composable
internal fun AllVideosView(
    items: List<UIVideo>,
    lazyListState: LazyListState,
    sortOrder: String,
    modifier: Modifier,
    onClick: () -> Unit,
    onMenuClick: (UIVideo) -> Unit,
    onSortOrderClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
) {
    LegacyMegaEmptyView(
        modifier = modifier,
        text = stringResource(id = R.string.homepage_empty_hint_video),
        imagePainter = painterResource(id = R.drawable.ic_homepage_empty_video)
    )
}

@CombinedThemePreviews
@Composable
private fun VideoPlaylistsViewPreview() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        AllVideosView(
            emptyList(),
            LazyListState(),
            "name",
            Modifier,
            {},
            {},
            {}
        )
    }
}

