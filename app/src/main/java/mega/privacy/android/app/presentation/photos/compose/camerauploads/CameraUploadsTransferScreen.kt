package mega.privacy.android.app.presentation.photos.compose.camerauploads

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import mega.privacy.android.app.presentation.photos.timeline.viewmodel.TimelineViewModel
import mega.privacy.android.app.presentation.transfers.model.image.ActiveTransferImageViewModel

@Composable
fun CameraUploadsTransferScreen(
    timelineViewModel: TimelineViewModel,
    viewModel: ActiveTransferImageViewModel = hiltViewModel(),
    navHostController: NavHostController,
    onSettingOptionClick: () -> Unit,
) {
    //Will implement in the ticket - AND-21227
}