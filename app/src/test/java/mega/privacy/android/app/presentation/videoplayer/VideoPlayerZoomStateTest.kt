package mega.privacy.android.app.presentation.videoplayer

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class VideoPlayerZoomStateTest {

    private lateinit var underTest: VideoPlayerZoomState

    // Landscape video FIT into a portrait screen: width already matches the screen,
    // so fillZoom = 2000 / 500 = 4.
    private val viewport = VideoZoomViewport(
        videoWidth = 1000,
        videoHeight = 500,
        screenWidth = 1000,
        screenHeight = 2000,
        panThresholdPx = VideoPlayerZoomState.PAN_OVERFLOW_SLACK_PX,
    )

    // Video aspect ratio matches the screen exactly, so fillZoom = 1.
    private val matchingViewport = VideoZoomViewport(
        videoWidth = 1000,
        videoHeight = 2000,
        screenWidth = 1000,
        screenHeight = 2000,
        panThresholdPx = VideoPlayerZoomState.PAN_OVERFLOW_SLACK_PX,
    )

    private val unknownViewport = VideoZoomViewport(
        videoWidth = 0,
        videoHeight = 0,
        screenWidth = 1000,
        screenHeight = 2000,
        panThresholdPx = VideoPlayerZoomState.PAN_OVERFLOW_SLACK_PX,
    )

    // Panoramic video whose fill level (2000 / 250 = 8) exceeds MAX_ZOOM.
    private val panoramicViewport = VideoZoomViewport(
        videoWidth = 1000,
        videoHeight = 250,
        screenWidth = 1000,
        screenHeight = 2000,
        panThresholdPx = VideoPlayerZoomState.PAN_OVERFLOW_SLACK_PX,
    )

    // Landscape-shaped geometry: the height nearly fills the screen while fillZoom is set by
    // the width (max(1000 / 400, 1000 / 990) = 2.5), so the vertical axis starts overflowing
    // well below fill. Each test supplies its own threshold.
    private val landscapeLikeViewport = VideoZoomViewport(
        videoWidth = 400,
        videoHeight = 990,
        screenWidth = 1000,
        screenHeight = 1000,
        panThresholdPx = VideoPlayerZoomState.PAN_OVERFLOW_SLACK_PX,
    )

    @BeforeEach
    fun setUp() {
        underTest = VideoPlayerZoomState()
    }

    @Test
    fun `test that fillZoom returns the larger screen to video ratio`() {
        assertThat(viewport.fillZoom).isEqualTo(4f)
    }

    @Test
    fun `test that fillZoom returns min zoom when the video size is unknown`() {
        assertThat(unknownViewport.fillZoom).isEqualTo(VideoPlayerZoomState.MIN_ZOOM)
    }

    @Test
    fun `test that onPinchScale scales relative to the current zoom level`() {
        underTest.onPinchScale(2f, viewport)
        assertThat(underTest.zoomLevel).isEqualTo(2f)
        underTest.onPinchScale(2f, viewport)
        assertThat(underTest.zoomLevel).isEqualTo(4f)
    }

    @Test
    fun `test that onPinchScale zooms continuously past the fill zoom without stopping`() {
        underTest.onPinchScale(100f, viewport)
        assertThat(underTest.zoomLevel).isEqualTo(VideoPlayerZoomState.MAX_ZOOM)
    }

    @Test
    fun `test that onPinchScale clamps the zoom between min and max zoom`() {
        underTest.onPinchScale(100f, matchingViewport)
        assertThat(underTest.zoomLevel).isEqualTo(VideoPlayerZoomState.MAX_ZOOM)
        underTest.onPinchScale(0.001f, matchingViewport)
        assertThat(underTest.zoomLevel).isEqualTo(VideoPlayerZoomState.MIN_ZOOM)
    }

    @Test
    fun `test that the translation is clamped per axis when the zoom returns below the fill zoom`() {
        underTest.onPinchScale(100f, viewport)
        underTest.onPan(-10000f, -10000f, viewport)
        underTest.onPinchScale(0.5f, viewport)
        assertThat(underTest.translationX).isEqualTo(-750f)
        assertThat(underTest.translationY).isEqualTo(0f)
    }

    @Test
    fun `test that the translation resets when the zoom returns to the fit zoom`() {
        underTest.onPinchScale(100f, viewport)
        underTest.onPan(-10000f, -10000f, viewport)
        underTest.onPinchScale(0.01f, viewport)
        assertThat(underTest.translationX).isEqualTo(0f)
        assertThat(underTest.translationY).isEqualTo(0f)
    }

    @Test
    fun `test that onPan clamps the translation so the video edges stop at the screen edges`() {
        underTest.onPinchScale(100f, viewport)
        underTest.onPan(-10000f, -10000f, viewport)
        assertThat(underTest.translationX).isEqualTo(-2000f)
        assertThat(underTest.translationY).isEqualTo(-250f)
    }

    @Test
    fun `test that onPan keeps the video centered on an axis where it does not overflow the screen`() {
        underTest.onPinchScale(4f, viewport)
        underTest.onPan(-500f, -500f, viewport)
        assertThat(underTest.translationX).isEqualTo(-500f)
        assertThat(underTest.translationY).isEqualTo(0f)
    }

    @Test
    fun `test that onPan leaves the translation unclamped when the video size is unknown`() {
        underTest.onPinchScale(2f, unknownViewport)
        underTest.onPan(-50f, -30f, unknownViewport)
        assertThat(underTest.translationX).isEqualTo(-50f)
        assertThat(underTest.translationY).isEqualTo(-30f)
    }

    @Test
    fun `test that zoomToFill zooms to the fill level and centers the video`() {
        underTest.onPinchScale(100f, viewport)
        underTest.onPan(-10000f, -10000f, viewport)
        underTest.zoomToFill(viewport)
        assertThat(underTest.zoomLevel).isEqualTo(viewport.fillZoom)
        assertThat(underTest.translationX).isEqualTo(0f)
        assertThat(underTest.translationY).isEqualTo(0f)
    }

    @Test
    fun `test that reset returns to the fit display`() {
        underTest.onPinchScale(100f, viewport)
        underTest.onPan(-10000f, -10000f, viewport)
        underTest.reset()
        assertThat(underTest.zoomLevel).isEqualTo(VideoPlayerZoomState.MIN_ZOOM)
        assertThat(underTest.translationX).isEqualTo(0f)
        assertThat(underTest.translationY).isEqualTo(0f)
    }

    @Test
    fun `test that isZoomedToFill returns true only at or beyond the fill zoom`() {
        assertThat(underTest.isZoomedToFill(viewport)).isFalse()
        underTest.onPinchScale(2f, viewport)
        assertThat(underTest.isZoomedToFill(viewport)).isFalse()
        underTest.onPinchScale(2f, viewport)
        assertThat(underTest.isZoomedToFill(viewport)).isTrue()
        underTest.onPinchScale(1.25f, viewport)
        assertThat(underTest.isZoomedToFill(viewport)).isTrue()
    }

    @Test
    fun `test that isZoomedToFill returns false at fit when the video already fills the screen`() {
        assertThat(underTest.isZoomedToFill(matchingViewport)).isFalse()
    }

    @Test
    fun `test that isZoomedBeyondFill returns true only past the fill zoom`() {
        underTest.onPinchScale(4f, viewport)
        assertThat(underTest.isZoomedBeyondFill(viewport)).isFalse()
        underTest.onPinchScale(1.25f, viewport)
        assertThat(underTest.isZoomedBeyondFill(viewport)).isTrue()
    }

    @Test
    fun `test that onPinchScale snaps to the fill zoom within the snap threshold`() {
        underTest.onPinchScale(3.9f, viewport)
        assertThat(underTest.zoomLevel).isEqualTo(viewport.fillZoom)
    }

    @Test
    fun `test that onPinchScale snaps to the fit zoom within the snap threshold`() {
        underTest.onPinchScale(2f, viewport)
        underTest.onPinchScale(0.52f, viewport)
        assertThat(underTest.zoomLevel).isEqualTo(VideoPlayerZoomState.MIN_ZOOM)
    }

    @Test
    fun `test that onPinchScale does not snap outside the snap threshold`() {
        underTest.onPinchScale(3.7f, viewport)
        assertThat(underTest.zoomLevel).isWithin(1e-4f).of(3.7f)
    }

    @Test
    fun `test that onPinchScale returns the boundary when the zoom first reaches the fill zoom`() {
        assertThat(underTest.onPinchScale(3.9f, viewport)).isEqualTo(ZoomBoundary.Fill)
    }

    @Test
    fun `test that onPinchScale returns Fit when the zoom comes back down to the fit zoom`() {
        underTest.onPinchScale(2f, viewport)
        assertThat(underTest.onPinchScale(0.51f, viewport)).isEqualTo(ZoomBoundary.Fit)
    }

    @Test
    fun `test that onPinchScale returns null while the zoom stays on a boundary`() {
        underTest.onPinchScale(3.9f, viewport)
        assertThat(underTest.onPinchScale(1.02f, viewport)).isNull()
        assertThat(underTest.zoomLevel).isEqualTo(viewport.fillZoom)
    }

    @Test
    fun `test that onPinchScale returns null when the zoom leaves a boundary`() {
        underTest.onPinchScale(3.9f, viewport)
        assertThat(underTest.onPinchScale(1.2f, viewport)).isNull()
    }

    @Test
    fun `test that a continuous pinch travels out of a snap zone`() {
        underTest.onPinchScale(2f, viewport)
        underTest.onPinchScale(2f, viewport)
        underTest.onPinchScale(1.04f, viewport)
        assertThat(underTest.zoomLevel).isEqualTo(viewport.fillZoom)
        underTest.onPinchScale(1.04f, viewport)
        assertThat(underTest.zoomLevel).isWithin(1e-3f).of(4.3264f)
    }

    @Test
    fun `test that onPinchEnd anchors the next pinch at the snapped zoom level`() {
        underTest.onPinchScale(3.85f, viewport)
        assertThat(underTest.zoomLevel).isEqualTo(viewport.fillZoom)
        underTest.onPinchEnd()
        underTest.onPinchScale(1.06f, viewport)
        assertThat(underTest.zoomLevel).isWithin(1e-3f).of(4.24f)
    }

    @Test
    fun `test that currentBoundary reflects the fit fill and intermediate zoom levels`() {
        assertThat(underTest.currentBoundary(viewport)).isEqualTo(ZoomBoundary.Fit)
        underTest.onPinchScale(2f, viewport)
        assertThat(underTest.currentBoundary(viewport)).isNull()
        underTest.zoomToFill(viewport)
        assertThat(underTest.currentBoundary(viewport)).isEqualTo(ZoomBoundary.Fill)
    }

    @Test
    fun `test that reset anchors the next pinch at the fit zoom`() {
        underTest.onPinchScale(3.9f, viewport)
        underTest.reset()
        underTest.onPinchScale(2f, viewport)
        assertThat(underTest.zoomLevel).isEqualTo(2f)
    }

    @Test
    fun `test that zoomToFill anchors the next pinch at the fill zoom`() {
        underTest.zoomToFill(viewport)
        underTest.onPinchScale(1.25f, viewport)
        assertThat(underTest.zoomLevel).isEqualTo(VideoPlayerZoomState.MAX_ZOOM)
    }

    @Test
    fun `test that onPinchScale snaps to fit when the fit and fill zones overlap`() {
        underTest.onPinchScale(1.04f, matchingViewport)
        assertThat(underTest.zoomLevel).isEqualTo(VideoPlayerZoomState.MIN_ZOOM)
    }

    @Test
    fun `test that onPinchScale can reach a fill zoom greater than the max zoom`() {
        underTest.onPinchScale(7.9f, panoramicViewport)
        assertThat(underTest.zoomLevel).isEqualTo(panoramicViewport.fillZoom)
    }

    @Test
    fun `test that a pinch after zoomToFill does not collapse when the fill zoom exceeds the max zoom`() {
        underTest.zoomToFill(panoramicViewport)
        underTest.onPinchScale(0.9f, panoramicViewport)
        assertThat(underTest.zoomLevel).isWithin(1e-3f).of(7.2f)
    }

    @Test
    fun `test that onPinchScale returns Fill when a single event crosses up over the fill zoom`() {
        underTest.onPinchScale(2f, viewport)
        assertThat(underTest.onPinchScale(2.5f, viewport)).isEqualTo(ZoomBoundary.Fill)
        assertThat(underTest.zoomLevel).isEqualTo(VideoPlayerZoomState.MAX_ZOOM)
    }

    @Test
    fun `test that onPinchScale returns Fill when a single event crosses down over the fill zoom`() {
        underTest.onPinchScale(100f, viewport)
        assertThat(underTest.onPinchScale(0.5f, viewport)).isEqualTo(ZoomBoundary.Fill)
    }

    @Test
    fun `test that canPanHorizontally returns true between fit and fill when the width overflows`() {
        underTest.onPinchScale(2f, viewport)
        assertThat(underTest.canPanHorizontally(viewport)).isTrue()
        assertThat(underTest.canPanVertically(viewport)).isFalse()
    }

    @Test
    fun `test that canPanHorizontally and canPanVertically return false at the fit zoom`() {
        assertThat(underTest.canPanHorizontally(viewport)).isFalse()
        assertThat(underTest.canPanVertically(viewport)).isFalse()
    }

    @Test
    fun `test that onPan moves the video only on the overflowing axis between fit and fill`() {
        underTest.onPinchScale(2f, viewport)
        underTest.onPan(-300f, -100f, viewport)
        assertThat(underTest.translationX).isEqualTo(-300f)
        assertThat(underTest.translationY).isEqualTo(0f)
    }

    @Test
    fun `test that canPanHorizontally returns false when the overflow is below the pan threshold`() {
        val thresholdViewport = viewport.copy(panThresholdPx = 100f)
        // 1.06 rather than 1.05: the latter sits on the fit snap boundary and is pulled back to 1.
        underTest.onPinchScale(1.06f, thresholdViewport)
        assertThat(underTest.canPanHorizontally(thresholdViewport)).isFalse()
    }

    @Test
    fun `test that canPanHorizontally returns true when the overflow exceeds the pan threshold`() {
        val thresholdViewport = viewport.copy(panThresholdPx = 100f)
        underTest.onPinchScale(1.5f, thresholdViewport)
        assertThat(underTest.canPanHorizontally(thresholdViewport)).isTrue()
    }

    // Zoom 1.1 overflows the vertical axis by 89px (990 * 1.1 - 1000) and is clear of both
    // snap zones ([0.95, 1.05] for fit, [2.375, 2.625] for fill), so the two tests below
    // differ only in where the threshold sits relative to that 89px.
    @Test
    fun `test that canPanVertically returns false when the overflow is below the pan threshold`() {
        val strictViewport = landscapeLikeViewport.copy(panThresholdPx = 300f)
        underTest.onPinchScale(1.1f, strictViewport)
        assertThat(underTest.canPanVertically(strictViewport)).isFalse()
    }

    @Test
    fun `test that canPanVertically returns true when the overflow exceeds the pan threshold`() {
        val looseViewport = landscapeLikeViewport.copy(panThresholdPx = 50f)
        underTest.onPinchScale(1.1f, looseViewport)
        assertThat(underTest.canPanVertically(looseViewport)).isTrue()
    }
}
