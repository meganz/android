package mega.privacy.android.app.camera

import android.annotation.SuppressLint
import android.view.ViewGroup
import androidx.camera.video.QualitySelector
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import mega.privacy.android.app.camera.state.CamSelector
import mega.privacy.android.app.camera.state.CameraState
import mega.privacy.android.app.camera.state.CaptureMode
import mega.privacy.android.app.camera.state.FlashMode
import mega.privacy.android.app.camera.state.rememberCameraState

/**
 * Creates a Camera Preview's composable.
 *
 * @param cameraState camera state hold some states and camera's controller, it can be useful to given action like [CameraState.takePicture]
 * @param camSelector camera selector to be added, default is back
 * @param captureMode camera capture mode, default is image
 * @param flashMode flash mode to be added, default is off
 * @param enableTorch enable torch from camera, default is false.
 * @param videoQualitySelector quality selector to the video capture
 * @see CameraState
 * */
@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    cameraState: CameraState = rememberCameraState(),
    camSelector: CamSelector = cameraState.camSelector,
    captureMode: CaptureMode = cameraState.captureMode,
    flashMode: FlashMode = cameraState.flashMode,
    enableTorch: Boolean = cameraState.enableTorch,
    videoQualitySelector: QualitySelector = cameraState.videoQualitySelector,
) {
    CameraPreviewContent(
        modifier = modifier,
        cameraState = cameraState,
        camSelector = camSelector,
        captureMode = captureMode,
        flashMode = flashMode,
        enableTorch = enableTorch,
        videoQualitySelector = videoQualitySelector,
    )
}

@SuppressLint("RestrictedApi")
@Composable
internal fun CameraPreviewContent(
    cameraState: CameraState,
    camSelector: CamSelector,
    captureMode: CaptureMode,
    flashMode: FlashMode,
    enableTorch: Boolean,
    videoQualitySelector: QualitySelector,
    modifier: Modifier = Modifier,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraIsInitialized by rememberUpdatedState(cameraState.isInitialized)

    AndroidView(modifier = modifier, factory = { context ->
        PreviewView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
            )
            controller = cameraState.controller.apply {
                bindToLifecycle(lifecycleOwner)
            }

            previewStreamState.observe(lifecycleOwner) { state ->
                cameraState.isStreaming = state == PreviewView.StreamState.STREAMING
            }
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }, update = { _ ->
        if (cameraIsInitialized) {
            cameraState.update(
                camSelector = camSelector,
                captureMode = captureMode,
                flashMode = flashMode,
                enableTorch = enableTorch,
                videoQualitySelector = videoQualitySelector,
            )
        }
    })
}
