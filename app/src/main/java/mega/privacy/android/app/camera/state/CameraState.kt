package mega.privacy.android.app.camera.state

import android.Manifest
import android.content.Context
import androidx.annotation.RequiresPermission
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.TorchState
import androidx.camera.video.FileDescriptorOutputOptions
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recording
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.video.AudioConfig
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.util.Consumer
import timber.log.Timber
import java.util.concurrent.Executor

/**
 * A state object that can be hoisted to control camera, take picture or record video.
 *
 * To be created use [rememberCameraState].
 * */
@Stable
class CameraState(context: Context) {

    /**
     * Main Executor to action as take picture or record.
     * */
    private val mainExecutor: Executor = ContextCompat.getMainExecutor(context)

    /**
     * Main controller from CameraX. useful in cases that haven't been release some feature yet.
     * */
    val controller: LifecycleCameraController = LifecycleCameraController(context)

    /**
     * Record controller to video Capture
     * */
    private var recordController: Recording? = null

    /**
     * Check if camera is streaming or not.
     * */
    var isStreaming: Boolean by mutableStateOf(false)
        internal set

    /**
     * Check if camera state is initialized or not.
     * */
    var isInitialized: Boolean by mutableStateOf(false)
        internal set

    /**
     * Verify if camera has flash or not.
     * */
    private var hasFlashUnit: Boolean by mutableStateOf(
        controller.cameraInfo?.hasFlashUnit() ?: true
    )

    /**
     * Capture mode to be added on camera.
     * */
    internal var captureMode: CaptureMode = CaptureMode.Image
        set(value) {
            if (field != value) {
                field = value
                updateCaptureMode()
            }
        }

    internal var videoQualitySelector: QualitySelector
        get() = controller.videoCaptureQualitySelector
        set(value) {
            controller.videoCaptureQualitySelector = value
        }

    /**
     * Camera mode, it can be front or back.
     * @see CamSelector
     * */
    internal var camSelector: CamSelector = CamSelector.Back
        set(value) {
            when {
                value == field -> Unit
                !isRecording && hasCamera(value) -> {
                    if (controller.cameraSelector != value.selector) {
                        controller.cameraSelector = value.selector
                        field = value
                        resetCamera()
                    }
                }

                isRecording -> Timber.d("Device is recording, switch camera is unavailable")
                else -> Timber.d("Device does not have ${value.selector} camera")
            }
        }

    /**
     * Flash Mode from the camera.
     * @see FlashMode
     * */
    internal var flashMode: FlashMode
        get() = FlashMode.find(controller.imageCaptureFlashMode)
        set(value) {
            if (hasFlashUnit && flashMode != value) {
                controller.imageCaptureFlashMode = value.mode
            }
        }

    /**
     * Enabled/Disable torch from camera.
     * */
    internal var enableTorch: Boolean
        get() = controller.torchState.value == TorchState.ON
        set(value) {
            if (enableTorch != value) {
                controller.enableTorch(hasFlashUnit && value)
            }
        }

    /**
     * Return true if it's recording.
     * */
    var isRecording: Boolean by mutableStateOf(controller.isRecording)
        private set

    init {
        controller.initializationFuture.addListener({
            resetCamera()
            isInitialized = true
        }, mainExecutor)
    }

    private fun updateCaptureMode() {
        try {
            controller.setEnabledUseCases(captureMode.value)
        } catch (exception: IllegalStateException) {
            Timber.e("Use case Image Analysis not supported")
        }
    }

    /**
     * Take a picture with the camera.
     *
     * @param outputFileOptions Output file options of the photo.
     * @param onResult Callback called when [CaptureResult] is ready
     * */
    fun takePicture(
        outputFileOptions: ImageCapture.OutputFileOptions,
        onResult: (CaptureResult) -> Unit,
    ) {
        try {
            controller.takePicture(outputFileOptions,
                mainExecutor,
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                        onResult(CaptureResult.Success(outputFileResults.savedUri))
                    }

                    override fun onError(exception: ImageCaptureException) {
                        onResult(CaptureResult.Error(exception))
                    }
                })
        } catch (exception: Exception) {
            onResult(CaptureResult.Error(exception))
        }
    }

    /**
     * Start recording camera.
     *
     * @param fileDescriptorOutputOptions file output options where the video will be saved
     * */
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun startRecording(
        fileDescriptorOutputOptions: FileDescriptorOutputOptions,
        audioConfig: AudioConfig = AudioConfig.create(true),
        onResult: (CaptureResult) -> Unit,
    ): Unit = prepareRecording(onResult) {
        controller.startRecording(
            fileDescriptorOutputOptions,
            audioConfig,
            mainExecutor,
            getConsumerEvent(onResult)
        )
    }

    private fun getConsumerEvent(
        onResult: (CaptureResult) -> Unit,
    ): Consumer<VideoRecordEvent> = Consumer<VideoRecordEvent> { event ->
        Timber.i("Video Recorder Event - $event")
        if (event is VideoRecordEvent.Finalize) {
            val result = when {
                !event.hasError() -> CaptureResult.Success(event.outputResults.outputUri)
                else -> CaptureResult.Error(event.cause)
            }
            onResult(result)
            recordController = null
            isRecording = false
        }
    }

    /**
     * Prepare recording camera.
     *
     * @param onRecordBuild lambda to retrieve record controller
     * @param onError Callback called when thrown error
     * */
    private fun prepareRecording(
        onError: (CaptureResult.Error) -> Unit,
        onRecordBuild: () -> Recording,
    ) {
        try {
            Timber.i("Prepare recording")
            isRecording = true
            recordController = onRecordBuild()
        } catch (exception: Exception) {
            Timber.i("Fail to record! - $exception")
            isRecording = false
            onError(CaptureResult.Error(exception))
        }
    }

    /**
     * Stop recording camera.
     * */
    fun stopRecording() {
        Timber.i("Stop recording")
        recordController?.stop()
    }

    /**
     * Return if has camera selector or not, camera must be initialized, otherwise result is false.
     * */
    private fun hasCamera(cameraSelector: CamSelector): Boolean =
        isInitialized && controller.hasCamera(cameraSelector.selector)

    private fun resetCamera() {
        hasFlashUnit = controller.cameraInfo?.hasFlashUnit() ?: false
        controller.isPinchToZoomEnabled = false
    }

    /**
     * Update all values from camera state.
     * */
    internal fun update(
        camSelector: CamSelector,
        captureMode: CaptureMode,
        flashMode: FlashMode,
        enableTorch: Boolean,
        videoQualitySelector: QualitySelector,
    ) {
        this.camSelector = camSelector
        this.captureMode = captureMode
        this.flashMode = flashMode
        this.enableTorch = enableTorch
        this.videoQualitySelector = videoQualitySelector
    }
}