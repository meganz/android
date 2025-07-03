package mega.privacy.android.app.camera.menu

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import mega.privacy.android.app.R
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.shared.original.core.ui.model.MenuActionWithIcon

internal sealed interface CameraMenuAction : MenuActionWithIcon {
    class FlashOff : CameraMenuAction {
        @Composable
        override fun getDescription(): String = stringResource(id = R.string.camera_flash_mode_off)

        @Composable
        override fun getIconPainter() =
            rememberVectorPainter(IconPack.Medium.Regular.Outline.ZapOff)

        override val testTag = "flash_off"
        override val orderInCategory = 1
    }

    class FlashOn : CameraMenuAction {
        @Composable
        override fun getDescription(): String = stringResource(id = R.string.camera_flash_mode_on)

        @Composable
        override fun getIconPainter() = rememberVectorPainter(IconPack.Medium.Regular.Outline.Zap)
        override val testTag = "flash_on"
        override val orderInCategory = 2
    }

    class FlashAuto : CameraMenuAction {
        @Composable
        override fun getDescription(): String = stringResource(id = R.string.camera_flash_mode_auto)

        @Composable
        override fun getIconPainter() =
            rememberVectorPainter(IconPack.Medium.Regular.Outline.ZapAuto)

        override val testTag = "flash_auto"
        override val orderInCategory = 3
    }

    class Setting : CameraMenuAction {
        @Composable
        override fun getDescription(): String = stringResource(id = R.string.action_settings)

        @Composable
        override fun getIconPainter() =
            rememberVectorPainter(IconPack.Medium.Regular.Outline.GearSix)

        override val testTag = "setting"
        override val orderInCategory = 4
    }
}