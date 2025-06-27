package mega.privacy.android.app.camera

import android.annotation.SuppressLint
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.camera.core.FocusMeteringAction
import androidx.camera.video.QualitySelector
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.concurrent.futures.await
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
 * @param onTapFocus callback when user taps to focus
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
    onTapFocus: ((Float, Float) -> Unit),
) {
    CameraPreviewContent(
        modifier = modifier,
        cameraState = cameraState,
        camSelector = camSelector,
        captureMode = captureMode,
        flashMode = flashMode,
        enableTorch = enableTorch,
        videoQualitySelector = videoQualitySelector,
        onTapFocus = onTapFocus,
    )
}

@SuppressLint("RestrictedApi", "ClickableViewAccessibility")
@Composable
internal fun CameraPreviewContent(
    cameraState: CameraState,
    camSelector: CamSelector,
    captureMode: CaptureMode,
    flashMode: FlashMode,
    enableTorch: Boolean,
    videoQualitySelector: QualitySelector,
    modifier: Modifier = Modifier,
    onTapFocus: ((Float, Float) -> Unit),
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
                // Disable built-in tap-to-focus to avoid conflicts with manual implementation
                setTapToFocusEnabled(false)
            }
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER

            previewStreamState.observe(lifecycleOwner) { state ->
                cameraState.isStreaming = state == PreviewView.StreamState.STREAMING
            }

            val gestureDetector =
                GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
                    override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                        cameraState.isFocusActive = true
                        onTapFocus(e.x, e.y)
                        val focusStartTime = System.currentTimeMillis()
                        val focusPoint = meteringPointFactory.createPoint(e.x, e.y)
                        val meteringAction = FocusMeteringAction.Builder(focusPoint).build()
                        lifecycleOwner.lifecycleScope.launch {
                            runCatching {
                                cameraState.controller.cameraControl
                                    ?.startFocusAndMetering(meteringAction)?.await()
                            }
                            val focusDuration = System.currentTimeMillis() - focusStartTime
                            val minFocusRingDuration = 450L

                            // Add delay only if focus completed too fast
                            if (focusDuration < minFocusRingDuration) {
                                val remainingTime = minFocusRingDuration - focusDuration
                                delay(remainingTime)
                            }

                            cameraState.isFocusActive = false
                        }
                        return true
                    }

                    override fun onDown(e: MotionEvent): Boolean {
                        return true // Always return true to handle the gesture
                    }
                })

            setOnTouchListener { _, event ->
                gestureDetector.onTouchEvent(event)
            }
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
