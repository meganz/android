package mega.privacy.android.feature.texteditor.presentation

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TextEditorFastScrollbarTest {

    @Test
    fun `test that calculateScrollProportion returns 1f when last visible item is the last item`() {
        val result = calculateScrollProportion(
            firstVisibleItemIndex = 90,
            firstVisibleItemScrollOffset = 0,
            lastVisibleItemIndex = 99,
            firstVisibleItemSize = 100f,
            itemCount = 100,
        )
        assertThat(result).isEqualTo(1f)
    }

    @Test
    fun `test that calculateScrollProportion returns 1f when last visible item exceeds itemCount`() {
        val result = calculateScrollProportion(
            firstVisibleItemIndex = 98,
            firstVisibleItemScrollOffset = 0,
            lastVisibleItemIndex = 101,
            firstVisibleItemSize = 100f,
            itemCount = 100,
        )
        assertThat(result).isEqualTo(1f)
    }

    @Test
    fun `test that calculateScrollProportion returns 1f when lastVisibleItemIndex is null and firstVisible is last item`() {
        val result = calculateScrollProportion(
            firstVisibleItemIndex = 99,
            firstVisibleItemScrollOffset = 0,
            lastVisibleItemIndex = null,
            firstVisibleItemSize = 100f,
            itemCount = 100,
        )
        assertThat(result).isEqualTo(1f)
    }

    @Test
    fun `test that calculateScrollProportion returns 0f when at start with no scroll offset`() {
        val result = calculateScrollProportion(
            firstVisibleItemIndex = 0,
            firstVisibleItemScrollOffset = 0,
            lastVisibleItemIndex = 5,
            firstVisibleItemSize = 100f,
            itemCount = 100,
        )
        assertThat(result).isEqualTo(0f)
    }

    @Test
    fun `test that calculateScrollProportion returns correct proportion for mid-list position`() {
        val result = calculateScrollProportion(
            firstVisibleItemIndex = 50,
            firstVisibleItemScrollOffset = 0,
            lastVisibleItemIndex = 55,
            firstVisibleItemSize = 100f,
            itemCount = 100,
        )
        assertThat(result).isEqualTo(0.5f)
    }

    @Test
    fun `test that calculateScrollProportion includes sub-item scroll offset in proportion`() {
        val result = calculateScrollProportion(
            firstVisibleItemIndex = 50,
            firstVisibleItemScrollOffset = 50,
            lastVisibleItemIndex = 55,
            firstVisibleItemSize = 100f,
            itemCount = 100,
        )
        assertThat(result).isWithin(0.001f).of(0.505f)
    }

    @Test
    fun `test that calculateScrollProportion uses firstVisibleItemIndex as fallback when lastVisibleItemIndex is null`() {
        val result = calculateScrollProportion(
            firstVisibleItemIndex = 50,
            firstVisibleItemScrollOffset = 0,
            lastVisibleItemIndex = null,
            firstVisibleItemSize = 100f,
            itemCount = 100,
        )
        assertThat(result).isEqualTo(0.5f)
    }

    @Test
    fun `test that calculateScrollProportion uses default item size of 1 when firstVisibleItemSize is null`() {
        val result = calculateScrollProportion(
            firstVisibleItemIndex = 25,
            firstVisibleItemScrollOffset = 0,
            lastVisibleItemIndex = 30,
            firstVisibleItemSize = null,
            itemCount = 100,
        )
        assertThat(result).isEqualTo(0.25f)
    }

    @Test
    fun `test that calculateScrollProportion uses default item size of 1 when firstVisibleItemSize is zero`() {
        val result = calculateScrollProportion(
            firstVisibleItemIndex = 25,
            firstVisibleItemScrollOffset = 0,
            lastVisibleItemIndex = 30,
            firstVisibleItemSize = 0f,
            itemCount = 100,
        )
        assertThat(result).isEqualTo(0.25f)
    }

    @Test
    fun `test that calculateScrollProportion returns 1f when lastVisibleItemIndex exceeds itemCount minus 1`() {
        val result = calculateScrollProportion(
            firstVisibleItemIndex = 0,
            firstVisibleItemScrollOffset = 0,
            lastVisibleItemIndex = 3,
            firstVisibleItemSize = 100f,
            itemCount = 1,
        )
        assertThat(result).isEqualTo(1f)
    }

    @Test
    fun `test that calculateScrollProportion returns 1f when itemCount is 0`() {
        val result = calculateScrollProportion(
            firstVisibleItemIndex = 0,
            firstVisibleItemScrollOffset = 0,
            lastVisibleItemIndex = null,
            firstVisibleItemSize = null,
            itemCount = 0,
        )
        assertThat(result).isEqualTo(1f)
    }

    @Test
    fun `test that calculateScrollProportion handles single item list`() {
        val result = calculateScrollProportion(
            firstVisibleItemIndex = 0,
            firstVisibleItemScrollOffset = 0,
            lastVisibleItemIndex = 0,
            firstVisibleItemSize = 100f,
            itemCount = 1,
        )
        assertThat(result).isEqualTo(1f)
    }
}
