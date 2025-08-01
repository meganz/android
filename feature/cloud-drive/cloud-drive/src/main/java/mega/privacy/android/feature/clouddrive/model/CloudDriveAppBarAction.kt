package mega.privacy.android.feature.clouddrive.model

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import mega.android.core.ui.model.TopAppBarAction
import mega.privacy.android.icon.pack.IconPack

sealed interface CloudDriveAppBarAction : TopAppBarAction {
    data object Search : TopAppBarAction {
        override val testTag: String = "cloud_drive_app_bar:search"

        @Composable
        override fun getDescription() = "Search"

        @Composable
        override fun getIconPainter() =
            rememberVectorPainter(IconPack.Medium.Thin.Outline.SearchLarge)
    }

    data object More : TopAppBarAction {
        override val testTag: String = "cloud_drive_app_bar:more"

        @Composable
        override fun getDescription() = "More"

        @Composable
        override fun getIconPainter() =
            rememberVectorPainter(IconPack.Medium.Thin.Outline.MoreVertical)
    }
}