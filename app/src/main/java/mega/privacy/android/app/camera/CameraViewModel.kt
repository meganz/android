package mega.privacy.android.app.camera

import android.annotation.SuppressLint
import android.app.Application
import android.net.Uri
import androidx.camera.core.ImageCapture
import androidx.camera.video.FileDescriptorOutputOptions
import androidx.camera.view.video.AudioConfig
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mega.privacy.android.app.camera.state.CameraState
import mega.privacy.android.app.camera.state.CaptureResult
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.usecase.file.CreateNewImageUriUseCase
import mega.privacy.android.domain.usecase.file.CreateNewVideoUriUseCase
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
internal class CameraViewModel @Inject constructor(
    private val createNewImageUriUseCase: CreateNewImageUriUseCase,
    private val createNewVideoUriUseCase: CreateNewVideoUriUseCase,
    private val application: Application,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {
    private val _state = MutableStateFlow(CameraUiState())
    val state = _state.asStateFlow()

    fun takePicture(cameraState: CameraState) {
        viewModelScope.launch {
            createNewImageUri()?.let { savedUri ->
                // don't call uri.use here, let camera X take care of closing the stream
                withContext(ioDispatcher) {
                    application.contentResolver.openOutputStream(savedUri)
                }?.let { outputStream ->
                    cameraState.takePicture(
                        ImageCapture.OutputFileOptions.Builder(outputStream).build(),
                    ) { result ->
                        when (result) {
                            is CaptureResult.Success -> _state.update { state ->
                                state.copy(onCapturePhotoEvent = triggered(savedUri))
                            }

                            is CaptureResult.Error -> {
                                Timber.e(result.throwable, "Image capture failed")
                            }
                        }
                    }
                }
            }

        }
    }

    @SuppressLint("MissingPermission")
    fun captureVideo(cameraState: CameraState, hasRecordAudioPermission: Boolean) {
        viewModelScope.launch {
            if (cameraState.isRecording) {
                cameraState.stopRecording()
            } else {
                val savedUri = createNewVideoUri() ?: return@launch
                withContext(ioDispatcher) {
                    application.contentResolver.openFileDescriptor(savedUri, "rw")
                }?.let { fileDescriptor ->
                    cameraState.startRecording(
                        FileDescriptorOutputOptions.Builder(fileDescriptor).build(),
                        AudioConfig.create(hasRecordAudioPermission),
                    ) {
                        when (it) {
                            is CaptureResult.Success -> {
                                _state.update { state ->
                                    state.copy(onCaptureVideoEvent = triggered(savedUri))
                                }
                            }

                            is CaptureResult.Error -> {
                                Timber.e(it.throwable, "Recording failed")
                            }
                        }
                    }
                }
            }
        }
    }

    fun onTakePictureEventConsumed() {
        _state.update { state ->
            state.copy(onCapturePhotoEvent = consumed())
        }
    }

    fun onCaptureVideoEventConsumed() {
        _state.update { state ->
            state.copy(onCaptureVideoEvent = consumed())
        }
    }

    private suspend fun createNewVideoUri(): Uri? {
        Timber.d("createNewVideoUri")
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val videoFileName = "video${timeStamp}.mp4"
        return createNewVideoUriUseCase(videoFileName)?.let { Uri.parse(it) }
    }

    private suspend fun createNewImageUri(): Uri? {
        Timber.d("createNewImageUri")
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "picture${timeStamp}.jpg"
        return createNewImageUriUseCase(imageFileName)?.let { Uri.parse(it) }
    }
}