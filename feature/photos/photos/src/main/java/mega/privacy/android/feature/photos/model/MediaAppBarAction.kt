package mega.privacy.android.feature.photos.model

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import mega.android.core.ui.model.menu.MenuAction
import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.android.core.ui.model.menu.MenuActionWithoutIcon
import mega.privacy.android.core.sharedcomponents.R as sharedComponentsR
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.shared.resources.R as sharedResR

sealed interface MediaAppBarAction : MenuAction {

    @Stable
    data class CameraUpload(
        val status: CameraUploadStatus,
    ) : MenuActionWithIcon, MediaAppBarAction {
        @Composable
        override fun getIconPainter(): Painter = when (status) {
            CameraUploadStatus.Default ->
                painterResource(sharedComponentsR.drawable.ic_cu_status)

            CameraUploadStatus.Paused ->
                painterResource(sharedComponentsR.drawable.ic_cu_status_paused)

            CameraUploadStatus.Warning ->
                painterResource(sharedComponentsR.drawable.ic_cu_status_warning)

            CameraUploadStatus.Completed ->
                painterResource(sharedComponentsR.drawable.ic_cu_status_completed)
        }

        override val testTag: String = "media_app_bar:camera_upload"

        @Composable
        override fun getDescription(): String =
            stringResource(sharedResR.string.general_camera_uploads)

        enum class CameraUploadStatus {
            Default,
            Paused,
            Warning,
            Completed
        }
    }

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
}
