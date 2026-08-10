package mega.privacy.android.feature.texteditor.presentation

import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.TestScope
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
 * Unit tests for scroll-to-hide bar logic ([isBarsHidden]) and
 * [ScrollToHideBarState] property wiring.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TextEditorScrollBarStateTest {

    // region isBarsHidden

    @Test
    fun `test that isBarsHidden returns false when top bar height is zero`() {
        assertThat(isBarsHidden(0f, 0f)).isFalse()
        assertThat(isBarsHidden(0f, -100f)).isFalse()
    }

    @Test
    fun `test that isBarsHidden returns false when offset is greater than negative height plus epsilon`() {
        assertThat(isBarsHidden(100f, 0f)).isFalse()
        assertThat(isBarsHidden(100f, -50f)).isFalse()
        assertThat(isBarsHidden(100f, -98f)).isFalse()
    }

    @Test
    fun `test that isBarsHidden returns true when offset is at or past negative height`() {
        assertThat(isBarsHidden(100f, -100f)).isTrue()
        assertThat(isBarsHidden(100f, -101f)).isTrue()
        assertThat(isBarsHidden(100f, -150f)).isTrue()
    }

    @Test
    fun `test that isBarsHidden uses epsilon so boundary is stable`() {
        val height = 100f
        val epsilon = 1f
        assertThat(isBarsHidden(height, -height + 1.5f, epsilon = epsilon)).isFalse()
        assertThat(isBarsHidden(height, -height + 1f, epsilon = epsilon)).isTrue()
        assertThat(isBarsHidden(height, -height, epsilon = epsilon)).isTrue()
    }

    // endregion

    // region ScrollToHideBarState property delegation

    @Test
    fun `test that topBarHeightPx getter and setter delegate to backing state`() {
        val heightState = mutableFloatStateOf(0f)
        val state = createState(topBarHeightPxState = heightState)

        state.topBarHeightPx = 56f

        assertThat(heightState.floatValue).isEqualTo(56f)
        assertThat(state.topBarHeightPx).isEqualTo(56f)
    }

    @Test
    fun `test that topBarOffsetPx getter and setter delegate to backing state`() {
        val offsetState = mutableFloatStateOf(0f)
        val state = createState(topBarOffsetPxState = offsetState)

        state.topBarOffsetPx = -30f

        assertThat(offsetState.floatValue).isEqualTo(-30f)
        assertThat(state.topBarOffsetPx).isEqualTo(-30f)
    }

    // endregion

    private fun createState(
        topBarHeightPxState: MutableFloatState = mutableFloatStateOf(0f),
        topBarOffsetPxState: MutableFloatState = mutableFloatStateOf(0f),
        scope: CoroutineScope = TestScope(),
    ): ScrollToHideBarState = ScrollToHideBarState(
        topBarHeightPxState = topBarHeightPxState,
        topBarOffsetPxState = topBarOffsetPxState,
        barsHidden = derivedStateOf { false },
        bottomBarSlideDistancePx = 100f,
        bottomBarTranslationY = derivedStateOf { 0f },
        bottomBarEntranceOffset = Animatable(100f),
        scrollConnection = object : NestedScrollConnection {},
        scope = scope,
    )
}
