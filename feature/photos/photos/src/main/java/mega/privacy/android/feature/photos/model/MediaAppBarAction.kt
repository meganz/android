package mega.privacy.android.feature.photos.model

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.privacy.android.core.sharedcomponents.R as sharedComponentsR
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.shared.resources.R as sharedResR

sealed interface MediaAppBarAction : MenuActionWithIcon {

    @Stable
    data class CameraUpload(val status: CameraUploadStatus) : MenuActionWithIcon {
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

    data object Search : MenuActionWithIcon {
        @Composable
        override fun getIconPainter(): Painter =
            rememberVectorPainter(IconPack.Medium.Thin.Outline.SearchLarge)

        override val testTag: String = "media_app_bar:search"

        @Composable
        override fun getDescription(): String = "Search"
    }

    data object More : MenuActionWithIcon {
        @Composable
        override fun getIconPainter(): Painter =
            rememberVectorPainter(IconPack.Medium.Thin.Outline.MoreVertical)

        override val testTag: String = "media_app_bar:more"

        @Composable
        override fun getDescription(): String = "More"
    }
}