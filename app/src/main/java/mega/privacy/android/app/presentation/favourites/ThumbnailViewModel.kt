package mega.privacy.android.app.presentation.favourites

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
import mega.privacy.android.domain.usecase.thumbnailpreview.GetPublicNodeThumbnailUseCase
import mega.privacy.android.domain.usecase.thumbnailpreview.GetThumbnailUseCase
import timber.log.Timber
import java.io.File
import javax.inject.Inject

/**
 * ViewModel for get thumbnail
 *
 * @property getThumbnailUseCase GetThumbnailByFlow
 * @property ioDispatcher CoroutineDispatcher
 * @property mainDispatcher CoroutineDispatcher
 */
@HiltViewModel
class ThumbnailViewModel @Inject constructor(
    private val getThumbnailUseCase: GetThumbnailUseCase,
    private val getPublicNodeThumbnailUseCase: GetPublicNodeThumbnailUseCase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @MainDispatcher private val mainDispatcher: CoroutineDispatcher,
) : ViewModel() {

    /**
     * Get thumbnail
     * @param handle the node handle that gets the thumbnail
     * @param onFinished the callback after the thumbnail is downloaded
     */
    fun getThumbnail(handle: Long, onFinished: (file: File?) -> Unit) {
        Timber.v("ThumbnailViewModel start to getThumbnail thumbnail $handle")
        viewModelScope.launch(ioDispatcher) {
            runCatching {
                flowOf(getThumbnailUseCase(handle)).collectLatest { file ->
                    withContext(mainDispatcher) {
                        onFinished(file)
                    }
                }
            }.onFailure {
                Timber.e("ThumbnailViewModel getFlowThumbnail error message is ${it.message}")
            }
        }
    }

    /**
     * Get public node thumbnail
     * @param handle the node handle that gets the thumbnail
     * @param onFinished the callback after the thumbnail is downloaded
     */
    fun getPublicNodeThumbnail(handle: Long, onFinished: (file: File?) -> Unit) {
        Timber.v("ThumbnailViewModel start to getPublicNodeThumbnail thumbnail $handle")
        viewModelScope.launch(ioDispatcher) {
            runCatching {
                flowOf(getPublicNodeThumbnailUseCase(handle)).collectLatest { file ->
                    withContext(mainDispatcher) {
                        onFinished(file)
                    }
                }
            }.onFailure {
                Timber.e("ThumbnailViewModel getFlowThumbnail error message is ${it.message}")
            }
        }
    }
}