package mega.privacy.android.app.camera.state

import android.Manifest
import android.content.Context
import androidx.annotation.RequiresPermission
import androidx.camera.core.Camera
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.TorchState
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.camera.video.FileDescriptorOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import java.util.concurrent.Executor

/**
 * A state object that can be hoisted to control camera, take picture or record video.
 *
 * To be created use [rememberCameraState].
 * */
@Stable
class CameraState(private val context: Context) {

    /**
     * Main Executor to action as take picture or record.
     * */
    private val mainExecutor: Executor = ContextCompat.getMainExecutor(context)

    /**
     * CameraX use cases.
     * */
    private val previewUseCase: Preview = Preview.Builder().build()
    private val imageCaptureUseCase: ImageCapture = ImageCapture.Builder().build()
    private var recorder: Recorder = Recorder.Builder()
        .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
        .build()
    private var videoCaptureUseCase: VideoCapture<Recorder> = VideoCapture.withOutput(recorder)

    /**
     * Camera provider and bound camera.
     * */
    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? by mutableStateOf(null)
    private var boundLifecycleOwner: LifecycleOwner? = null

    /**
     * Record controller for video capture.
     * */
    private var recordController: Recording? = null

    /**
     * Surface request flow for CameraXViewfinder.
     * */
    private val _surfaceRequest = MutableStateFlow<SurfaceRequest?>(null)
    val surfaceRequest: StateFlow<SurfaceRequest?> = _surfaceRequest.asStateFlow()

    /**
     * Metering point factory for tap-to-focus coordinate transformation.
     * */
    private var surfaceMeteringPointFactory: SurfaceOrientedMeteringPointFactory? = null

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
    private var hasFlashUnit: Boolean by mutableStateOf(true)

    /**
     * Focus state to track when focus is active.
     * */
    var isFocusActive: Boolean by mutableStateOf(false)
        internal set

    /**
     * Capture mode to be added on camera.
     * */
    internal var captureMode: CaptureMode = CaptureMode.Image
        set(value) {
            if (field != value) {
                field = value
                rebind()
            }
        }

    internal var videoQualitySelector: QualitySelector = QualitySelector.from(Quality.HIGHEST)
        set(value) {
            if (field != value) {
                field = value
                recorder = Recorder.Builder()
                    .setQualitySelector(value)
                    .build()
                videoCaptureUseCase = VideoCapture.withOutput(recorder)
                if (captureMode == CaptureMode.Video) {
                    rebind()
                }
            }
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
                    field = value
                    rebind()
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
        get() = FlashMode.find(imageCaptureUseCase.flashMode)
        set(value) {
            if (hasFlashUnit && flashMode != value) {
                imageCaptureUseCase.flashMode = value.mode
            }
        }

    /**
     * Enabled/Disable torch from camera.
     * */
    internal var enableTorch: Boolean
        get() = camera?.cameraInfo?.torchState?.value == TorchState.ON
        set(value) {
            if (enableTorch != value) {
                camera?.cameraControl?.enableTorch(hasFlashUnit && value)
            }
        }

    /**
     * Return true if it's recording.
     * */
    var isRecording: Boolean by mutableStateOf(false)
        private set

    /**
     * Current zoom ratio of the camera.
     * */
    var zoomRatio: Float by mutableFloatStateOf(1.0f)

    /**
     * Camera info from the bound camera, for observing zoom state.
     *
     * Backed by [camera], which is replaced on every rebind, so readers re-run when the
     * bound camera changes rather than holding on to a stale [CameraInfo].
     * */
    val cameraInfo: CameraInfo?
        get() = camera?.cameraInfo

    init {
        previewUseCase.setSurfaceProvider { request ->
            _surfaceRequest.value = request
            surfaceMeteringPointFactory = SurfaceOrientedMeteringPointFactory(
                request.resolution.width.toFloat(),
                request.resolution.height.toFloat()
            )
            isStreaming = true
        }
    }

    /**
     * Bind camera to lifecycle and initialize the camera provider.
     * Must be called from a coroutine scope.
     * */
    suspend fun bind(lifecycleOwner: LifecycleOwner) {
        cameraProvider = ProcessCameraProvider.awaitInstance(context)
        boundLifecycleOwner = lifecycleOwner
        rebind()
        isInitialized = true
    }

    /**
     * Release the camera and drop the pending surface request.
     *
     * [bind] rebinds from scratch, so this is safe to call whenever the preview leaves
     * composition instead of waiting for the [LifecycleOwner] to be destroyed.
     * */
    fun unbind() {
        cameraProvider?.unbindAll()
        cameraProvider = null
        boundLifecycleOwner = null
        camera = null
        _surfaceRequest.value = null
        isInitialized = false
    }

    private fun rebind() {
        val provider = cameraProvider ?: return
        val lifecycleOwner = boundLifecycleOwner ?: return
        provider.unbindAll()
        val useCases = when (captureMode) {
            CaptureMode.Image -> arrayOf(previewUseCase, imageCaptureUseCase)
            CaptureMode.Video -> arrayOf(previewUseCase, videoCaptureUseCase)
        }
        try {
            camera = provider.bindToLifecycle(
                lifecycleOwner,
                camSelector.selector,
                *useCases
            )
            resetCamera()
        } catch (e: Exception) {
            Timber.e(e, "Failed to bind camera use cases")
        }
    }

    /**
     * Handle tap-to-focus using transformed coordinates from CameraXViewfinder.
     *
     * @param tapCoords The tap coordinates transformed to surface-oriented space
     *                  via [MutableCoordinateTransformer].
     * */
    fun tapToFocus(tapCoords: Offset) {
        val factory = surfaceMeteringPointFactory ?: return
        val point = factory.createPoint(tapCoords.x, tapCoords.y)
        val action = FocusMeteringAction.Builder(point).build()
        camera?.cameraControl?.startFocusAndMetering(action)
    }

    /**
     * Apply zoom ratio to the camera control, clamped to the range the camera supports.
     * */
    fun applyZoomRatio(ratio: Float) {
        val zoomState = camera?.cameraInfo?.zoomState?.value ?: return
        val clampedRatio = ratio.coerceIn(zoomState.minZoomRatio, zoomState.maxZoomRatio)
        camera?.cameraControl?.setZoomRatio(clampedRatio)
        zoomRatio = clampedRatio
    }

    /**
     * Scale the current zoom ratio by [scaleFactor] reported by a pinch gesture.
     *
     * The starting ratio is read from the camera instead of [zoomRatio] so the gesture
     * stays accurate even when the camera has been rebound since the last observation.
     * */
    fun scaleZoomRatio(scaleFactor: Float) {
        val currentRatio = camera?.cameraInfo?.zoomState?.value?.zoomRatio ?: return
        applyZoomRatio(currentRatio * scaleFactor)
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
            imageCaptureUseCase.takePicture(
                outputFileOptions,
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
     * @param audioEnabled whether audio recording is enabled
     * */
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun startRecording(
        fileDescriptorOutputOptions: FileDescriptorOutputOptions,
        audioEnabled: Boolean = true,
        onResult: (CaptureResult) -> Unit,
    ) {
        try {
            Timber.i("Prepare recording")
            isRecording = true
            val pendingRecording = recorder.prepareRecording(context, fileDescriptorOutputOptions)
            if (audioEnabled) {
                pendingRecording.withAudioEnabled()
            }
            recordController = pendingRecording.start(mainExecutor) { event ->
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
        } catch (exception: Exception) {
            Timber.i("Fail to record! - $exception")
            isRecording = false
            onResult(CaptureResult.Error(exception))
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
        isInitialized && runCatching {
            cameraProvider?.hasCamera(cameraSelector.selector) ?: false
        }.getOrElse { error ->
            Timber.w(error, "Failed to check camera availability")
            false
        }

    private fun resetCamera() {
        hasFlashUnit = camera?.cameraInfo?.hasFlashUnit() ?: false
        zoomRatio = 1.0f
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
