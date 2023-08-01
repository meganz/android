package mega.privacy.android.app.presentation.photos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.usecase.DownloadPreview
import mega.privacy.android.domain.usecase.DownloadPublicNodePreview
import mega.privacy.android.domain.usecase.DownloadPublicNodeThumbnail
import mega.privacy.android.domain.usecase.DownloadThumbnail
import timber.log.Timber
import java.io.File
import javax.inject.Inject

@HiltViewModel
class PhotoDownloaderViewModel @Inject constructor(
    private val downloadThumbnail: DownloadThumbnail,
    private val downloadPreview: DownloadPreview,
    private val downloadPublicNodeThumbnail: DownloadPublicNodeThumbnail,
    private val downloadPublicNodePreview: DownloadPublicNodePreview,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {
    private val channel = Channel<PhotoCover>(
        capacity = 300,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    init {
        viewModelScope.launch(ioDispatcher) {
            handleChannel()
        }
    }

    private suspend fun handleChannel() {
        for (photoCover in channel) {
            if (photoCover.isPublicNode) {
                downloadPhotoCover(photoCover)
            } else {
                downloadPublicNodePhotoCover(photoCover)
            }
        }
    }

    private suspend fun downloadPublicNodePhotoCover(
        photoCover: PhotoCover,
    ) {
        if (photoCover.isPreview) {
            downloadPreview(photoCover.photo.id) {
                photoCover.callback(it)
            }
        } else {
            downloadThumbnail(photoCover.photo.id) {
                photoCover.callback(it)
            }
        }
    }

    private suspend fun downloadPhotoCover(
        photoCover: PhotoCover,
    ) {
        runCatching {
            if (photoCover.isPreview) {
                downloadPublicNodePreview(photoCover.photo.id)
            } else {
                downloadPublicNodeThumbnail(photoCover.photo.id)
            }
        }.onSuccess {
            photoCover.callback(it)
        }.onFailure {
            Timber.e(it)
        }
    }

    suspend fun downloadPhoto(
        isPreview: Boolean,
        photo: Photo,
        callback: (success: Boolean) -> Unit,
    ) {
        withContext(ioDispatcher) {
            if (isPreview) {
                if (photo.previewFilePath == null)
                    return@withContext
                if (File(photo.previewFilePath ?: "").exists()) {
                    callback(true)
                    return@withContext
                }
            } else {
                if (photo.thumbnailFilePath == null)
                    return@withContext
                if (File(photo.thumbnailFilePath ?: "").exists()) {
                    callback(true)
                    return@withContext
                }
            }

            enterChannel(
                PhotoCover(
                    isPreview = isPreview,
                    photo = photo,
                    callback = callback
                )
            )
        }
    }

    suspend fun downloadPublicNodePhoto(
        isPreview: Boolean,
        photo: Photo,
        callback: (success: Boolean) -> Unit,
    ) {
        withContext(ioDispatcher) {
            if (isPreview) {
                if (photo.previewFilePath == null)
                    return@withContext
                if (File(photo.previewFilePath ?: "").exists()) {
                    callback(true)
                    return@withContext
                }
            } else {
                if (photo.thumbnailFilePath == null)
                    return@withContext
                if (File(photo.thumbnailFilePath ?: "").exists()) {
                    callback(true)
                    return@withContext
                }
            }

            enterChannel(
                PhotoCover(
                    isPreview = isPreview,
                    photo = photo,
                    callback = callback,
                    isPublicNode = true
                )
            )
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun enterChannel(
        cover: PhotoCover,
    ) {
        if (channel.isClosedForSend)
            return
        channel.send(cover)
    }

    override fun onCleared() {
        channel.close()
        super.onCleared()
    }
}

data class PhotoCover(
    val isPreview: Boolean,
    val photo: Photo,
    val callback: (success: Boolean) -> Unit,
    val isPublicNode: Boolean = false,
)