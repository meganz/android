package mega.privacy.android.feature.photos.extensions

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput

fun Modifier.photosZoomGestureDetector(
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
) = this.pointerInput(Unit) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false)
        do {
            val event = awaitPointerEvent(
                pass = PointerEventPass.Initial
            )
            if (event.changes.any { it.isConsumed })
                break
            val zoomChange = event.calculateZoom()
            if (zoomChange != 1.0f) {
                if (zoomChange > 1.0f) {
                    onZoomIn()
                } else {
                    onZoomOut()
                }
                // Consume event in case to trigger scroll
                event.changes.map { it.consume() }
                break
            }
        } while (event.changes.any { it.pressed })
    }
}