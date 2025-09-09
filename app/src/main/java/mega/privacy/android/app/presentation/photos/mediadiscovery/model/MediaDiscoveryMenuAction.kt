package mega.privacy.android.app.presentation.photos.mediadiscovery.model

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import mega.android.core.ui.model.TopAppBarAction
import mega.privacy.android.app.R
import mega.privacy.android.icon.pack.IconPack


sealed interface MediaDiscoveryMenuAction : TopAppBarAction {

    /**
     * Filter
     */
    data object Filter : TopAppBarAction {
        override val testTag: String = "media_discovery_app_bar:filter"

        @Composable
        override fun getDescription() = stringResource(R.string.photos_action_filter)

        @Composable
        override fun getIconPainter() =
            rememberVectorPainter(IconPack.Medium.Thin.Outline.SlidersVertical02)
    }

    /**
     * More
     */
    data object More : TopAppBarAction {
        override val testTag: String = "media_discovery_app_bar:more"

        @Composable
        override fun getDescription() = stringResource(R.string.label_more)

        @Composable
        override fun getIconPainter() =
            rememberVectorPainter(IconPack.Medium.Thin.Outline.MoreVertical)
    }

}