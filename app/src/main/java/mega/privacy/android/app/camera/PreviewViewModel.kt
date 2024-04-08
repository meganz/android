package mega.privacy.android.app.camera

import android.net.Uri
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.usecase.file.DeleteFileByUriUseCase
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
internal class PreviewViewModel @Inject constructor(
    @ApplicationScope private val applicationScope: CoroutineScope,
    private val deleteFileByUriUseCase: DeleteFileByUriUseCase,
) : ViewModel() {

    /**
     * Delete video
     *
     * @param uri
     */
    fun deleteVideo(uri: Uri) {
        applicationScope.launch {
            runCatching {
                deleteFileByUriUseCase(uri.toString())
            }.onFailure {
                Timber.e(it, "Failed to delete video")
            }
        }
    }
}