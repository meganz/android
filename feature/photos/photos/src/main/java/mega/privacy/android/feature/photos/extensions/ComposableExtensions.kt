package mega.privacy.android.feature.photos.extensions

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.flowWithLifecycle
import kotlinx.coroutines.flow.collectLatest
import mega.privacy.android.domain.entity.photos.DownloadPhotoResult
import mega.privacy.android.feature.photos.downloader.DownloadPhotoViewModel
import mega.privacy.android.feature.photos.model.PhotoUiState

// For test and preview purposes.
@SuppressLint("ComposeCompositionLocalUsage")
val LocalDownloadPhotoResultMock = compositionLocalOf<DownloadPhotoResult?> { null }

@SuppressLint("ComposeViewModelInjection")
@Composable
fun PhotoUiState.downloadAsStateWithLifecycle(isPreview: Boolean): State<DownloadPhotoResult> {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val mockDownloadResult = LocalDownloadPhotoResultMock.current
    return if (mockDownloadResult == null) {
        val viewModel: DownloadPhotoViewModel = hiltViewModel()
        produceState<DownloadPhotoResult>(
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
    } else {
        remember { mutableStateOf(mockDownloadResult) }
    }
}
