package mega.privacy.android.feature.texteditor.presentation

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/** Unit tests for the scroll-to-hide bar threshold logic ([isBarsHidden]). */
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
}
