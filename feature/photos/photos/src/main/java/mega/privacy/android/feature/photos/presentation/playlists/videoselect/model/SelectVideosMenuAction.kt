package mega.privacy.android.feature.photos.presentation.playlists.videoselect.model

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.shared.resources.R as sharedR

sealed interface SelectVideosMenuAction : MenuActionWithIcon {

    /**
     * Search menu action
     */
    data object Search : SelectVideosMenuAction {

        @Composable
        override fun getDescription() =
            stringResource(id = sharedR.string.search_bar_placeholder_text)

        @Composable
        override fun getIconPainter() =
            rememberVectorPainter(IconPack.Medium.Thin.Outline.SearchSmall)

        override val orderInCategory = 0

        override val testTag: String = "video_playlist_detail_selection:search"
    }
}
