package mega.privacy.android.app.camera.menu

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import mega.privacy.android.app.R
import mega.privacy.android.shared.original.core.ui.model.MenuActionWithIcon

internal sealed interface CameraMenuAction : MenuActionWithIcon {
    class FlashOff : CameraMenuAction {
        @Composable
        override fun getDescription(): String = stringResource(id = R.string.camera_flash_mode_off)

        @Composable
        override fun getIconPainter() = painterResource(R.drawable.ic_flash_off)
        override val testTag = "flash_off"
        override val orderInCategory = 1
    }

    class FlashOn : CameraMenuAction {
        @Composable
        override fun getDescription(): String = stringResource(id = R.string.camera_flash_mode_on)

        @Composable
        override fun getIconPainter() = painterResource(R.drawable.ic_flash)
        override val testTag = "flash_on"
        override val orderInCategory = 2
    }

    class FlashAuto : CameraMenuAction {
        @Composable
        override fun getDescription(): String = stringResource(id = R.string.camera_flash_mode_auto)

        @Composable
        override fun getIconPainter() = painterResource(R.drawable.ic_flash_auto)
        override val testTag = "flash_auto"
        override val orderInCategory = 3
    }
}