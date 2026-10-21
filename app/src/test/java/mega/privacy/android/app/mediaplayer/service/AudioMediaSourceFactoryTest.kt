package mega.privacy.android.app.mediaplayer.service

import android.app.Application
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Covers [buildAudioMediaSourceFactory], which must stay insensitive to the item's name.
 *
 * Robolectric, because the factory resolves `android.net.Uri`. JUnit4 rather than the project's
 * usual JUnit5 follows from that: Robolectric ships a runner, not a Jupiter extension, and this
 * repository carries no extension dependency.
 *
 * Asserting the source is progressive, rather than that the call returned, fails both when an
 * adaptive factory is loaded and when loading one throws.
 *
 * Not covered: which factory `initializePlayer` installs, and the load-error policy, for which
 * media3 exposes no getter. Dropping either from the production code leaves these green.
 */
@RunWith(RobolectricTestRunner::class)
internal class AudioMediaSourceFactoryTest {

    private val application = ApplicationProvider.getApplicationContext<Application>()

    @Test
    fun `test that buildAudioMediaSourceFactory returns a progressive source for a streamed track`() {
        assertProgressiveSourceFor("http://127.0.0.1:4443/abc/song.mp3")
    }

    @Test
    fun `test that buildAudioMediaSourceFactory returns a progressive source for a local file`() {
        assertProgressiveSourceFor("file:///storage/emulated/0/Music/song.flac")
    }

    @Test
    fun `test that buildAudioMediaSourceFactory returns a progressive source when the name ends in m3u8`() {
        assertProgressiveSourceFor("http://127.0.0.1:4443/abc/playlist.m3u8")
    }

    @Test
    fun `test that buildAudioMediaSourceFactory returns a progressive source when the name ends in m3u`() {
        assertProgressiveSourceFor("http://127.0.0.1:4443/abc/playlist.m3u")
    }

    @Test
    fun `test that buildAudioMediaSourceFactory returns a progressive source when the name ends in mpd`() {
        assertProgressiveSourceFor("http://127.0.0.1:4443/abc/manifest.mpd")
    }

    @Test
    fun `test that buildAudioMediaSourceFactory returns a progressive source when the name ends in ism`() {
        assertProgressiveSourceFor("http://127.0.0.1:4443/abc/stream.ism")
    }

    @Test
    fun `test that buildAudioMediaSourceFactory returns a progressive source when the name has no extension`() {
        assertProgressiveSourceFor("http://127.0.0.1:4443/abc/track")
    }

    private fun assertProgressiveSourceFor(uri: String) {
        val source = buildAudioMediaSourceFactory(
            dataSourceFactory = DefaultDataSource.Factory(application),
            loadErrorRetryCount = ANY_RETRY_COUNT,
        ).createMediaSource(MediaItem.Builder().setUri(uri.toUri()).build())

        assertThat(source).isInstanceOf(ProgressiveMediaSource::class.java)
    }

    private companion object {
        /** Any value: the retry count reaches the load-error policy, which nothing here observes. */
        const val ANY_RETRY_COUNT = 2
    }
}
