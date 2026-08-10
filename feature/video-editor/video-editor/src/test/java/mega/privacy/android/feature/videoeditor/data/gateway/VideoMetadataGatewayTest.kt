package mega.privacy.android.feature.videoeditor.data.gateway

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowMediaMetadataRetriever
import org.robolectric.shadows.util.DataSource

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q], manifest = Config.NONE)
class VideoMetadataGatewayTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val underTest = VideoMetadataGateway(context)

    private fun stub(uri: Uri, vararg metadata: Pair<Int, String>) {
        val dataSource = DataSource.toDataSource(context, uri)
        metadata.forEach { (key, value) ->
            ShadowMediaMetadataRetriever.addMetadata(dataSource, key, value)
        }
    }

    @Test
    fun `test that duration and dimensions are read`() {
        val uri = Uri.parse("content://media/external/video/1")
        stub(
            uri,
            MediaMetadataRetriever.METADATA_KEY_DURATION to "5000",
            MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH to "1920",
            MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT to "1080",
        )

        val result = underTest.getVideoMetadata(uri.toString())

        assertThat(result.durationMs).isEqualTo(5000L)
        assertThat(result.widthPx).isEqualTo(1920)
        assertThat(result.heightPx).isEqualTo(1080)
    }

    @Test
    fun `test that width and height are swapped for an odd-multiple rotation`() {
        val uri = Uri.parse("content://media/external/video/2")
        stub(
            uri,
            MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION to "90",
            MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH to "1920",
            MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT to "1080",
        )

        val result = underTest.getVideoMetadata(uri.toString())

        assertThat(result.widthPx).isEqualTo(1080)
        assertThat(result.heightPx).isEqualTo(1920)
    }

    @Test
    fun `test that capture date and location are parsed`() {
        val uri = Uri.parse("content://media/external/video/3")
        stub(
            uri,
            MediaMetadataRetriever.METADATA_KEY_DATE to "20210102T030405.000Z",
            MediaMetadataRetriever.METADATA_KEY_LOCATION to "+27.5916+086.5640/",
        )

        val result = underTest.getVideoMetadata(uri.toString())

        assertThat(result.dateTakenMs).isEqualTo(1_609_556_645_000L)
        assertThat(result.latitude).isNotNull()
        assertThat(result.latitude!!).isWithin(0.0001f).of(27.5916f)
        assertThat(result.longitude!!).isWithin(0.0001f).of(86.5640f)
    }

    @Test
    fun `test that missing metadata yields zeros and null library fields`() {
        val uri = Uri.parse("content://media/external/video/4")
        stub(uri) // no metadata registered

        val result = underTest.getVideoMetadata(uri.toString())

        assertThat(result.durationMs).isEqualTo(0L)
        assertThat(result.widthPx).isEqualTo(0)
        assertThat(result.heightPx).isEqualTo(0)
        assertThat(result.dateTakenMs).isNull()
        assertThat(result.latitude).isNull()
        assertThat(result.longitude).isNull()
    }
}
