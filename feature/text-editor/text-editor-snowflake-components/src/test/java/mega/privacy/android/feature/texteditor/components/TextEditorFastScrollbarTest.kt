package mega.privacy.android.feature.texteditor.components

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TextEditorFastScrollbarTest {

    @Test
    fun `test that calculateScrollProportion returns 1f at the true bottom when list can scroll back`() {
        val result = calculateScrollProportion(
            firstVisibleItemIndex = 99,
            firstVisibleItemScrollOffset = 0,
            firstVisibleItemSize = 100f,
            itemCount = 100,
            canScrollForward = false,
            canScrollBackward = true,
        )
        assertThat(result).isEqualTo(1f)
    }

    @Test
    fun `test that calculateScrollProportion does not snap to end while the last item is only partially visible`() {
        // Regression for AND-23767 / T21378947: the last (very tall) chunk is on screen but the list
        // can still scroll forward, so the thumb must keep tracking the scroll instead of jumping to 1f.
        val result = calculateScrollProportion(
            firstVisibleItemIndex = 2,
            firstVisibleItemScrollOffset = 0,
            firstVisibleItemSize = 100f,
            itemCount = 4,
            canScrollForward = true,
            canScrollBackward = true,
        )
        assertThat(result).isWithin(0.001f).of(0.5f)
        assertThat(result).isLessThan(1f)
    }

    @Test
    fun `test that calculateScrollProportion returns 0f when the list cannot scroll either way`() {
        val result = calculateScrollProportion(
            firstVisibleItemIndex = 0,
            firstVisibleItemScrollOffset = 0,
            firstVisibleItemSize = 100f,
            itemCount = 2,
            canScrollForward = false,
            canScrollBackward = false,
        )
        assertThat(result).isEqualTo(0f)
    }

    @Test
    fun `test that calculateScrollProportion returns 0f when at start with no scroll offset`() {
        val result = calculateScrollProportion(
            firstVisibleItemIndex = 0,
            firstVisibleItemScrollOffset = 0,
            firstVisibleItemSize = 100f,
            itemCount = 100,
            canScrollForward = true,
            canScrollBackward = false,
        )
        assertThat(result).isEqualTo(0f)
    }

    @Test
    fun `test that calculateScrollProportion returns correct proportion for mid-list position`() {
        val result = calculateScrollProportion(
            firstVisibleItemIndex = 50,
            firstVisibleItemScrollOffset = 0,
            firstVisibleItemSize = 100f,
            itemCount = 100,
            canScrollForward = true,
            canScrollBackward = true,
        )
        assertThat(result).isEqualTo(0.5f)
    }

    @Test
    fun `test that calculateScrollProportion includes sub-item scroll offset in proportion`() {
        val result = calculateScrollProportion(
            firstVisibleItemIndex = 50,
            firstVisibleItemScrollOffset = 50,
            firstVisibleItemSize = 100f,
            itemCount = 100,
            canScrollForward = true,
            canScrollBackward = true,
        )
        assertThat(result).isWithin(0.001f).of(0.505f)
    }

    @Test
    fun `test that calculateScrollProportion uses default item size of 1 when firstVisibleItemSize is null`() {
        val result = calculateScrollProportion(
            firstVisibleItemIndex = 25,
            firstVisibleItemScrollOffset = 0,
            firstVisibleItemSize = null,
            itemCount = 100,
            canScrollForward = true,
            canScrollBackward = true,
        )
        assertThat(result).isEqualTo(0.25f)
    }

    @Test
    fun `test that calculateScrollProportion uses default item size of 1 when firstVisibleItemSize is zero`() {
        val result = calculateScrollProportion(
            firstVisibleItemIndex = 25,
            firstVisibleItemScrollOffset = 0,
            firstVisibleItemSize = 0f,
            itemCount = 100,
            canScrollForward = true,
            canScrollBackward = true,
        )
        assertThat(result).isEqualTo(0.25f)
    }

    @Test
    fun `test that calculateScrollProportion returns 0f when itemCount is 0`() {
        val result = calculateScrollProportion(
            firstVisibleItemIndex = 0,
            firstVisibleItemScrollOffset = 0,
            firstVisibleItemSize = null,
            itemCount = 0,
            canScrollForward = false,
            canScrollBackward = false,
        )
        assertThat(result).isEqualTo(0f)
    }

    @Test
    fun `test that calculateScrollTarget returns first item with no offset at proportion 0`() {
        val result = calculateScrollTarget(proportion = 0f, itemCount = 10)
        assertThat(result.index).isEqualTo(0)
        assertThat(result.offsetFraction).isEqualTo(0f)
    }

    @Test
    fun `test that calculateScrollTarget returns last item fully scrolled at proportion 1`() {
        val result = calculateScrollTarget(proportion = 1f, itemCount = 10)
        // 1.0 * 10 = 10.0 -> clamped index 9, fraction 1.0 (fully into the last item = end of list)
        assertThat(result.index).isEqualTo(9)
        assertThat(result.offsetFraction).isWithin(0.001f).of(1f)
    }

    @Test
    fun `test that calculateScrollTarget maps mid proportion to mid item`() {
        val result = calculateScrollTarget(proportion = 0.5f, itemCount = 10)
        assertThat(result.index).isEqualTo(5)
        assertThat(result.offsetFraction).isWithin(0.001f).of(0f)
    }

    @Test
    fun `test that calculateScrollTarget yields sub-item offset for continuous position`() {
        // 0.25 over 4 items -> position 1.0 exactly is index 1 offset 0; use a value that lands mid item
        val result = calculateScrollTarget(proportion = 0.3f, itemCount = 4)
        // 0.3 * 4 = 1.2 -> index 1, fraction 0.2
        assertThat(result.index).isEqualTo(1)
        assertThat(result.offsetFraction).isWithin(0.001f).of(0.2f)
    }

    @Test
    fun `test that calculateScrollTarget clamps proportion above 1`() {
        val result = calculateScrollTarget(proportion = 1.5f, itemCount = 10)
        // clamped to 1.0 -> last item fully scrolled
        assertThat(result.index).isEqualTo(9)
        assertThat(result.offsetFraction).isWithin(0.001f).of(1f)
    }

    @Test
    fun `test that calculateScrollTarget clamps proportion below 0`() {
        val result = calculateScrollTarget(proportion = -0.5f, itemCount = 10)
        assertThat(result.index).isEqualTo(0)
        assertThat(result.offsetFraction).isEqualTo(0f)
    }

    @Test
    fun `test that calculateScrollTarget returns zero target when itemCount is 0`() {
        val result = calculateScrollTarget(proportion = 0.5f, itemCount = 0)
        assertThat(result.index).isEqualTo(0)
        assertThat(result.offsetFraction).isEqualTo(0f)
    }

    @Test
    fun `test that calculateScrollTarget never returns index out of bounds`() {
        // proportion just below 1 with small list must still clamp to last index
        val result = calculateScrollTarget(proportion = 0.999999f, itemCount = 2)
        assertThat(result.index).isEqualTo(1)
        assertThat(result.offsetFraction).isAtLeast(0f)
        assertThat(result.offsetFraction).isAtMost(1f)
    }

    @Test
    fun `test that shouldShowScrollbar returns false when itemCount is 0`() {
        assertThat(shouldShowScrollbar(0)).isFalse()
    }

    @Test
    fun `test that shouldShowScrollbar returns true when itemCount is 1`() {
        // A single chunk can still span many screens (e.g. an XML file under the 50k-char chunk cap);
        // actual thumb visibility is then gated by whether the list can scroll.
        assertThat(shouldShowScrollbar(1)).isTrue()
    }

    @Test
    fun `test that shouldShowScrollbar returns true when itemCount is 2`() {
        assertThat(shouldShowScrollbar(2)).isTrue()
    }

    @Test
    fun `test that shouldShowScrollbar returns true when itemCount is large`() {
        assertThat(shouldShowScrollbar(100)).isTrue()
    }
}
