package mega.privacy.android.app.presentation.meeting.chat.view.message.meta

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.chat.Giphy
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GiphyMessageViewTest {

    @get:Rule
    var composeTestRule = createComposeRule()

    @Test
    fun `test that GiphyMessageView renders when width is 0`() {
        setContent(width = 0, height = 200)
        composeTestRule.onRoot().assertExists()
    }

    @Test
    fun `test that GiphyMessageView renders when width is negative`() {
        setContent(width = -100, height = 200)
        composeTestRule.onRoot().assertExists()
    }

    @Test
    fun `test that GiphyMessageView renders when height is negative`() {
        setContent(width = 200, height = -100)
        composeTestRule.onRoot().assertExists()
    }

    @Test
    fun `test that GiphyMessageView renders when height is huge`() {
        setContent(width = 1, height = Int.MAX_VALUE)
        composeTestRule.onRoot().assertExists()
    }

    @Test
    fun `test that calculateGiphyDisplaySize scales down to the max width keeping the aspect ratio`() {
        assertThat(calculateGiphyDisplaySize(width = 480, height = 270)).isEqualTo(256 to 144)
    }

    @Test
    fun `test that calculateGiphyDisplaySize keeps the size when narrower than the max width`() {
        assertThat(calculateGiphyDisplaySize(width = 200, height = 100)).isEqualTo(200 to 100)
    }

    @Test
    fun `test that calculateGiphyDisplaySize returns a square when width is 0`() {
        assertThat(calculateGiphyDisplaySize(width = 0, height = 200)).isEqualTo(256 to 256)
    }

    @Test
    fun `test that calculateGiphyDisplaySize returns a square when width is negative`() {
        assertThat(calculateGiphyDisplaySize(width = -100, height = 200)).isEqualTo(256 to 256)
    }

    @Test
    fun `test that calculateGiphyDisplaySize returns a square when height is not positive`() {
        assertThat(calculateGiphyDisplaySize(width = 200, height = 0)).isEqualTo(256 to 256)
        assertThat(calculateGiphyDisplaySize(width = 200, height = -100)).isEqualTo(256 to 256)
    }

    @Test
    fun `test that calculateGiphyDisplaySize caps the height when height is huge`() {
        assertThat(calculateGiphyDisplaySize(width = 256, height = Int.MAX_VALUE))
            .isEqualTo(256 to 1024)
    }

    @Test
    fun `test that calculateGiphyDisplaySize keeps a height of at least 1 when the giphy is very wide`() {
        assertThat(calculateGiphyDisplaySize(width = 100_000, height = 1)).isEqualTo(256 to 1)
    }

    private fun setContent(width: Int, height: Int) {
        composeTestRule.setContent {
            GiphyMessageView(
                gifInfo = Giphy(
                    mp4Src = null,
                    webpSrc = null,
                    title = null,
                    mp4Size = 0,
                    webpSize = 0,
                    width = width,
                    height = height,
                ),
                autoPlay = false,
            )
        }
    }
}
