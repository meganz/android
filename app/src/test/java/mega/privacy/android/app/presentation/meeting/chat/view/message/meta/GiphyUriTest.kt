package mega.privacy.android.app.presentation.meeting.chat.view.message.meta

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GiphyUriTest {

    @Test
    fun `test that toGiphyUri replaces the giphy scheme with the MEGA giphy endpoint`() {
        assertThat("giphy://media/abc/200.webp".toGiphyUri().toString())
            .isEqualTo("https://giphy.mega.nz/media/abc/200.webp")
    }

    @Test
    fun `test that toGiphyUri keeps a URL already on the MEGA giphy endpoint`() {
        assertThat("https://giphy.mega.nz/media/abc/200.webp".toGiphyUri().toString())
            .isEqualTo("https://giphy.mega.nz/media/abc/200.webp")
    }

    @Test
    fun `test that toGiphyUri returns null when the URL points to another host`() {
        assertThat("http://192.168.1.10:8000/webp.gif".toGiphyUri()).isNull()
    }

    @Test
    fun `test that toGiphyUri returns null when the giphy scheme is not a prefix`() {
        assertThat("https://evil.example.com/?a=giphy://media/abc.webp".toGiphyUri()).isNull()
    }

    @Test
    fun `test that toGiphyUri returns null when the host only starts with the MEGA giphy host`() {
        assertThat("https://giphy.mega.nz.evil.example.com/abc.webp".toGiphyUri()).isNull()
    }

    @Test
    fun `test that toGiphyUri returns null when the MEGA giphy host is not using https`() {
        assertThat("http://giphy.mega.nz/media/abc.webp".toGiphyUri()).isNull()
    }

    @Test
    fun `test that toGiphyUri returns null when the URL is empty`() {
        assertThat("".toGiphyUri()).isNull()
    }
}
