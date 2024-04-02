package mega.privacy.android.app.camera

import android.app.Application
import android.net.Uri
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mega.privacy.android.domain.qualifier.ApplicationScope
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
internal class PreviewViewModel @Inject constructor(
    private val application: Application,
    @ApplicationScope private val applicationScope: CoroutineScope,
) : ViewModel() {

    /**
     * Delete video
     *
     * @param uri
     */
    fun deleteVideo(uri: Uri) {
        applicationScope.launch {
            runCatching {
                application.contentResolver.delete(uri, null, null)
            }.onFailure {
                Timber.e(it, "Failed to delete video")
            }
        }
    }
}