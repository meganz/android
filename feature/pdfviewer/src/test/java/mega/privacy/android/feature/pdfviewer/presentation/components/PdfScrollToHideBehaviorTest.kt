package mega.privacy.android.feature.pdfviewer.presentation.components

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

/**
 * Unit tests for [PdfScrollToHideBehavior].
 */
class PdfScrollToHideBehaviorTest {

    private val underTest = PdfScrollToHideBehavior()

    @Test
    fun `test that the chrome shows at the top`() {
        assertThat(underTest.onScroll(atTop = true, suppressed = false)).isTrue()
    }

    @Test
    fun `test that the chrome hides away from the top`() {
        assertThat(underTest.onScroll(atTop = false, suppressed = false)).isFalse()
    }

    @Test
    fun `test that a suppressed sample at the top is ignored`() {
        assertThat(underTest.onScroll(atTop = true, suppressed = true)).isNull()
    }

    @Test
    fun `test that a suppressed sample away from the top is ignored`() {
        assertThat(underTest.onScroll(atTop = false, suppressed = true)).isNull()
    }
}
