package mega.privacy.android.app.presentation.photos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.qualifier.MainDispatcher
import mega.privacy.android.domain.usecase.GetPreview
import timber.log.Timber
import java.io.File
import javax.inject.Inject

/**
 * ViewModel for get thumbnail
 *
 * @property getPreview GetPreview
 * @property ioDispatcher CoroutineDispatcher
 * @property mainDispatcher CoroutineDispatcher
 */
@HiltViewModel
class PreviewViewModel @Inject constructor(
    private val getPreview: GetPreview,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @MainDispatcher private val mainDispatcher: CoroutineDispatcher,
) : ViewModel() {

    /**
     * Get preview
     * @param handle the node handle that gets the preview
     * @param onFinished the callback after the preview is downloaded
     */
    fun getPreview(handle: Long, onFinished: (file: File?) -> Unit) {
        Timber.v("PreviewViewModel start to download thumbnail $handle")
        viewModelScope.launch(ioDispatcher) {
            runCatching {
                flowOf(getPreview(handle)).collectLatest { file ->
                    withContext(mainDispatcher) {
                        onFinished(file)
                    }
                }
            }.onFailure {
                Timber.e("getFlowThumbnail error message is ${it.message}")
            }
        }
    }
}