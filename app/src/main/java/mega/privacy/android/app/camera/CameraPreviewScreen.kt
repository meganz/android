package mega.privacy.android.app.camera

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import mega.privacy.android.app.R
import mega.privacy.android.shared.original.core.ui.controls.appbar.AppBarType
import mega.privacy.android.shared.original.core.ui.controls.appbar.MegaAppBar
import mega.privacy.android.shared.original.core.ui.controls.camera.CameraPreviewBottomBar
import mega.privacy.android.shared.original.core.ui.controls.layouts.MegaScaffold

@Composable
internal fun CameraPreviewScreen(
    uri: Uri,
    title: String,
    onBackPressed: () -> Unit,
    onSend: (Uri) -> Unit,
    viewModel: PreviewViewModel = hiltViewModel(),
    content: @Composable (Modifier) -> Unit,
) {
    BackHandler {
        viewModel.deleteVideo(uri)
        onBackPressed()
    }
    MegaScaffold(
        modifier = Modifier.systemBarsPadding(),
        topBar = {
            MegaAppBar(
                appBarType = AppBarType.CLOSE,
                title = stringResource(id = R.string.camera_send_to, title),
                onNavigationPressed = {
                    viewModel.deleteVideo(uri)
                    onBackPressed()
                },
            )
        },
        bottomBar = {
            CameraPreviewBottomBar {
                onSend(uri)
            }
        }
    ) { innerPadding ->
        content(Modifier.padding(innerPadding))
    }
}