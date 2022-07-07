package mega.privacy.android.app.presentation.favourites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mega.privacy.android.app.di.IoDispatcher
import mega.privacy.android.app.di.MainDispatcher
import mega.privacy.android.domain.usecase.GetThumbnail
import timber.log.Timber
import java.io.File
import javax.inject.Inject

/**
 * ViewModel for get thumbnail
 *
 * @property getThumbnail GetThumbnailByFlow
 * @property ioDispatcher CoroutineDispatcher
 * @property mainDispatcher CoroutineDispatcher
 */
@HiltViewModel
class ThumbnailViewModel @Inject constructor(
    private val getThumbnail: GetThumbnail,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @MainDispatcher private val mainDispatcher: CoroutineDispatcher,
) : ViewModel() {

    /**
     * Get thumbnail
     * @param handle the node handle that gets the thumbnail
     * @param onFinished the callback after the thumbnail is downloaded
     */
    fun getThumbnail(handle: Long, onFinished: (file: File?) -> Unit) {
        viewModelScope.launch(ioDispatcher) {
            runCatching {
                flowOf(getThumbnail(handle)).collectLatest { file ->
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