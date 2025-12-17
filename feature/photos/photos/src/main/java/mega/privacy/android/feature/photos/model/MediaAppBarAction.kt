package mega.privacy.android.feature.photos.model

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import mega.android.core.ui.model.menu.MenuAction
import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.android.core.ui.model.menu.MenuActionWithoutIcon
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.shared.resources.R as sharedResR

sealed interface MediaAppBarAction : MenuAction {

    data object Search : MenuActionWithIcon, MediaAppBarAction {
        @Composable
        override fun getIconPainter(): Painter =
            rememberVectorPainter(IconPack.Medium.Thin.Outline.SearchLarge)

        override val testTag: String = "media_app_bar:search"

        @Composable
        override fun getDescription(): String = "Search"
    }

    data object More : MenuActionWithIcon, MediaAppBarAction {
        @Composable
        override fun getIconPainter(): Painter =
            rememberVectorPainter(IconPack.Medium.Thin.Outline.MoreVertical)

        override val testTag: String = "media_app_bar:more"

        @Composable
        override fun getDescription(): String = "More"
    }

    data object SortBy : MenuActionWithoutIcon(
        testTag = "media_app_bar:sort_by",
        descriptionRes = sharedResR.string.timeline_tab_sort_by_text
    ), MediaAppBarAction {
        override val orderInCategory: Int = 110
    }

    data object Filter : MenuActionWithIcon, MediaAppBarAction {

        override val orderInCategory: Int = 75

        override val testTag: String = "media_app_bar:filter"

        @Composable
        override fun getDescription(): String = "Filter"

        @Composable
        override fun getIconPainter(): Painter =
            rememberVectorPainter(IconPack.Medium.Thin.Outline.Filter)
    }

    data object FilterSecondary : MenuActionWithoutIcon(
        testTag = "media_app_bar:filter_secondary",
        descriptionRes = sharedResR.string.timeline_tab_filter_text
    ), MediaAppBarAction {
        override val orderInCategory: Int = 105
    }

    data object CameraUploadsSettings : MenuActionWithoutIcon(
        testTag = "media_app_bar:camera_uploads_settings",
        descriptionRes = sharedResR.string.general_settings
    ), MediaAppBarAction {
        override val orderInCategory: Int = 140
    }
}
