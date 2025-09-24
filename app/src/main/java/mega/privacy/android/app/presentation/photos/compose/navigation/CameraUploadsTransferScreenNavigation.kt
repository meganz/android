package mega.privacy.android.app.presentation.photos.compose.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import mega.privacy.android.app.presentation.photos.compose.camerauploads.CameraUploadsTransferScreen
import mega.privacy.android.app.presentation.photos.timeline.viewmodel.TimelineViewModel

@Serializable
internal object CameraUploadsTransferScreen : NavKey

internal fun NavGraphBuilder.cameraUploadsTransferScreen(
    timelineViewModel: TimelineViewModel,
    navHostController: NavHostController,
    onSettingOptionClick: () -> Unit,
) {
    composable<CameraUploadsTransferScreen> {
        CameraUploadsTransferScreen(
            timelineViewModel = timelineViewModel,
            navHostController = navHostController,
            onSettingOptionClick = onSettingOptionClick
        )
    }
}