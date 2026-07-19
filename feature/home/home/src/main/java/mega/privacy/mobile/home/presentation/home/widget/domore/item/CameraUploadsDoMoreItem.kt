package mega.privacy.mobile.home.presentation.home.widget.domore.item

import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import mega.privacy.android.domain.usecase.camerauploads.IsCameraUploadsEnabledUseCase
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.mobile.home.presentation.home.widget.domore.DoMoreWithMegaItem
import javax.inject.Inject

/**
 * "Camera uploads" shortcut in the "Do more with MEGA" section.
 */
class CameraUploadsDoMoreItem @Inject constructor(
    isCameraUploadsEnabledUseCase: IsCameraUploadsEnabledUseCase,
) : DoMoreWithMegaItem {
    override val identifier: DoMoreWithMegaItem.Identifier =
        DoMoreWithMegaItem.Identifier.CameraUploads
    override val icon: ImageVector = IconPack.Medium.Thin.Outline.Camera
    override val labelRes: Int = sharedR.string.home_do_more_with_mega_camera_uploads
    override val monitorVisibility: Flow<Boolean> =
        isCameraUploadsEnabledUseCase
            .monitorCameraUploadsEnabled
            .map { !it }
}
