package mega.privacy.android.feature.photos.presentation.cuprogress.action

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.shared.resources.R as SharedR

sealed interface CameraUploadsProgressAction {

    data object SettingOptionsMenuAction : MenuActionWithIcon {
        @Composable
        override fun getIconPainter(): Painter =
            rememberVectorPainter(IconPack.Medium.Thin.Outline.GearSix)

        override val testTag: String = "camera_uploads_progress_action:setting_options"

        @Composable
        override fun getDescription(): String = stringResource(id = SharedR.string.general_settings)
    }
}
