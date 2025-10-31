package mega.privacy.android.feature.photos.extensions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.flowWithLifecycle
import kotlinx.coroutines.flow.collectLatest
import mega.privacy.android.domain.entity.photos.DownloadPhotoResult
import mega.privacy.android.feature.photos.downloader.DownloadPhotoViewModel
import mega.privacy.android.feature.photos.model.PhotoUiState

@Composable
fun PhotoUiState.downloadAsStateWithLifecycle(
    isPreview: Boolean,
    viewModel: DownloadPhotoViewModel = hiltViewModel(),
): State<DownloadPhotoResult> {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    return produceState<DownloadPhotoResult>(
        initialValue = DownloadPhotoResult.Idle,
        key1 = this,
        key2 = isPreview
    ) {
        viewModel.getDownloadPhotoResult(this@downloadAsStateWithLifecycle, isPreview)
            .flowWithLifecycle(lifecycle)
            .collectLatest { result ->
                this.value = result
            }
    }
}
