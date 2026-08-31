package mega.privacy.android.app.camera

import androidx.camera.compose.CameraXViewfinder
import androidx.camera.viewfinder.compose.MutableCoordinateTransformer
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.lifecycle.compose.LocalLifecycleOwner
import mega.privacy.android.app.camera.state.CameraState
import mega.privacy.android.app.camera.state.rememberCameraState

/**
 * Creates a Camera Preview's composable using CameraXViewfinder.
 *
 * @param cameraState camera state hold some states and camera's controller, it can be useful to given action like [CameraState.takePicture]
 * @param onTapFocus callback when user taps to focus, providing layout coordinates
 * @see CameraState
 * */
@Composable
internal fun CameraPreview(
    modifier: Modifier = Modifier,
    cameraState: CameraState = rememberCameraState(),
    onTapFocus: ((Float, Float) -> Unit),
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val surfaceRequest by cameraState.surfaceRequest.collectAsState()
    val coordinateTransformer = remember { MutableCoordinateTransformer() }

    LaunchedEffect(lifecycleOwner) {
        cameraState.bind(lifecycleOwner)
    }

    DisposableEffect(cameraState) {
        onDispose { cameraState.unbind() }
    }

    surfaceRequest?.let { request ->
        CameraXViewfinder(
            surfaceRequest = request,
            coordinateTransformer = coordinateTransformer,
            modifier = modifier
                .pointerInput(cameraState) {
                    detectTransformGestures { _, _, zoom, _ ->
                        if (zoom != 1f) {
                            cameraState.scaleZoomRatio(zoom)
                        }
                    }
                }
                .pointerInput(cameraState, coordinateTransformer) {
                    detectTapGestures { tapCoords ->
                        cameraState.isFocusActive = true
                        onTapFocus(tapCoords.x, tapCoords.y)

                        with(coordinateTransformer) {
                            cameraState.tapToFocus(tapCoords.transform())
                        }
                    }
                },
        )
    }
}
