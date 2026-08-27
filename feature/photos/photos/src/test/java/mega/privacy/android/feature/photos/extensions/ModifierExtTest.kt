package mega.privacy.android.feature.photos.extensions

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.TouchInjectionScope
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class ModifierExtTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val zoomIns = mutableListOf<Unit>()
    private val zoomOuts = mutableListOf<Unit>()
    private val pinchActiveChanges = mutableListOf<Boolean>()

    @Test
    fun `test that photosZoomGestureDetector does not zoom when a single pointer drags`() {
        composeRule.setZoomContent()

        composeRule.onNodeWithTag(TARGET_TAG).performTouchInput {
            down(center)
            moveTo(center + Offset(0f, -300f))
            up()
        }

        assertThat(zoomIns).isEmpty()
        assertThat(zoomOuts).isEmpty()
        assertThat(pinchActiveChanges).isEmpty()
    }

    @Test
    fun `test that photosZoomGestureDetector does not zoom when the spread stays below the slop`() {
        composeRule.setZoomContent()

        composeRule.onNodeWithTag(TARGET_TAG).performTouchInput {
            pinch(from = 100f, to = 100f + viewConfiguration.touchSlop / 2f)
        }

        assertThat(zoomIns).isEmpty()
        assertThat(zoomOuts).isEmpty()
    }

    @Test
    fun `test that photosZoomGestureDetector zooms in once when the spread passes one step`() {
        composeRule.setZoomContent()

        composeRule.onNodeWithTag(TARGET_TAG).performTouchInput {
            pinch(from = 60f, to = 60f * ZOOM_STEP_RATIO * OVERSHOOT)
        }

        assertThat(zoomIns).hasSize(1)
        assertThat(zoomOuts).isEmpty()
    }

    @Test
    fun `test that photosZoomGestureDetector zooms in twice within one gesture when the spread passes two steps`() {
        composeRule.setZoomContent()

        composeRule.onNodeWithTag(TARGET_TAG).performTouchInput {
            pinch(from = 60f, to = 60f * ZOOM_STEP_RATIO * ZOOM_STEP_RATIO * OVERSHOOT)
        }

        assertThat(zoomIns).hasSize(2)
        assertThat(zoomOuts).isEmpty()
    }

    @Test
    fun `test that photosZoomGestureDetector zooms out when the spread shrinks past one step`() {
        composeRule.setZoomContent()

        composeRule.onNodeWithTag(TARGET_TAG).performTouchInput {
            pinch(from = 120f, to = 120f / ZOOM_STEP_RATIO / OVERSHOOT)
        }

        assertThat(zoomOuts).hasSize(1)
        assertThat(zoomIns).isEmpty()
    }

    @Test
    fun `test that photosZoomGestureDetector does not zoom when two pointers pan in parallel`() {
        composeRule.setZoomContent()

        composeRule.onNodeWithTag(TARGET_TAG).performTouchInput {
            val left = center + Offset(-60f, 0f)
            val right = center + Offset(60f, 0f)
            down(0, left)
            down(1, right)
            repeat(PINCH_STEPS) { step ->
                val dy = -100f * (step + 1) / PINCH_STEPS
                updatePointerTo(0, left + Offset(0f, dy))
                updatePointerTo(1, right + Offset(0f, dy))
                move()
            }
            up(0)
            up(1)
        }

        assertThat(zoomIns).isEmpty()
        assertThat(zoomOuts).isEmpty()
    }

    @Test
    fun `test that photosZoomGestureDetector reports the pinch as active for the gesture`() {
        composeRule.setZoomContent()

        composeRule.onNodeWithTag(TARGET_TAG).performTouchInput {
            pinch(from = 100f, to = 100f * ZOOM_STEP_RATIO)
        }

        assertThat(pinchActiveChanges).containsExactly(true, false).inOrder()
    }

    @Test
    fun `test that photosZoomGestureDetector reports the pinch as inactive when it ends below the slop`() {
        composeRule.setZoomContent()

        composeRule.onNodeWithTag(TARGET_TAG).performTouchInput {
            pinch(from = 100f, to = 100f + viewConfiguration.touchSlop / 2f)
        }

        assertThat(pinchActiveChanges).containsExactly(true, false).inOrder()
    }

    @Test
    fun `test that photosZoomGestureDetector stops an enclosing scroll once the pinch is claimed`() {
        lateinit var scrollState: ScrollState
        composeRule.setContent {
            scrollState = rememberScrollState()
            // Mirrors production: the detector sits outside the scrollable it has to preempt.
            Box(
                modifier = Modifier
                    .size(300.dp)
                    .testTag(TARGET_TAG)
                    .photosZoomGestureDetector(
                        onZoomIn = { zoomIns.add(Unit) },
                        onZoomOut = { zoomOuts.add(Unit) },
                        onPinchActiveChanged = { pinchActiveChanges.add(it) },
                    )
                    .verticalScroll(scrollState),
            ) {
                Box(modifier = Modifier.height(2000.dp).background(Color.Gray))
            }
        }

        // Both pointers travel upwards while spreading apart, so the centroid drifts far enough to
        // scroll — the claim is what has to keep the scroll at rest.
        composeRule.onNodeWithTag(TARGET_TAG).performTouchInput {
            down(0, center + Offset(-30f, 0f))
            down(1, center + Offset(30f, 0f))
            repeat(PINCH_STEPS) { step ->
                val progress = (step + 1).toFloat() / PINCH_STEPS
                val spread = 30f + 90f * progress
                val dy = -100f * progress
                updatePointerTo(0, center + Offset(-spread, dy))
                updatePointerTo(1, center + Offset(spread, dy))
                move()
            }
            up(0)
            up(1)
        }

        composeRule.runOnIdle {
            assertThat(zoomIns).isNotEmpty()
            assertThat(scrollState.value).isEqualTo(0)
        }
    }

    /**
     * Spreads two horizontally-opposed pointers from [from] to [to] pixels either side of the
     * centre, in [PINCH_STEPS] increments so the detector accumulates the change across several
     * events the way a real pinch does.
     */
    private fun TouchInjectionScope.pinch(from: Float, to: Float) {
        down(0, center + Offset(-from, 0f))
        down(1, center + Offset(from, 0f))
        repeat(PINCH_STEPS) { step ->
            val spread = from + (to - from) * (step + 1) / PINCH_STEPS
            updatePointerTo(0, center + Offset(-spread, 0f))
            updatePointerTo(1, center + Offset(spread, 0f))
            move()
        }
        up(0)
        up(1)
    }

    private fun ComposeContentTestRule.setZoomContent() = setContent {
        ZoomTarget(
            onZoomIn = { zoomIns.add(Unit) },
            onZoomOut = { zoomOuts.add(Unit) },
            onPinchActiveChanged = { pinchActiveChanges.add(it) },
        )
    }

    @Composable
    private fun ZoomTarget(
        onZoomIn: () -> Unit,
        onZoomOut: () -> Unit,
        onPinchActiveChanged: (Boolean) -> Unit,
    ) {
        Box(
            modifier = Modifier
                .size(300.dp)
                .background(Color.Gray)
                .photosZoomGestureDetector(
                    onZoomIn = onZoomIn,
                    onZoomOut = onZoomOut,
                    onPinchActiveChanged = onPinchActiveChanged,
                )
                .testTag(TARGET_TAG),
        )
    }

    private companion object {
        const val TARGET_TAG = "zoom_target"

        /** Mirrors the production `ZOOM_STEP_RATIO`, which is private to the modifier. */
        const val ZOOM_STEP_RATIO = 1.4f

        /** Enough intermediate events for the accumulator to cross the slop before the target. */
        const val PINCH_STEPS = 10

        /**
         * Keeps the injected spread clear of the step boundary. Accumulating the ratio across
         * [PINCH_STEPS] events lands a hair under it in float arithmetic, and the boundary itself is
         * not what these tests are about.
         */
        const val OVERSHOOT = 1.05f
    }
}
