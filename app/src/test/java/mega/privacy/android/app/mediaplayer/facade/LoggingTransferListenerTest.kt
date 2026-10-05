package mega.privacy.android.app.mediaplayer.facade

import android.net.Uri
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.exoplayer.upstream.BandwidthMeter
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.core.test.RecordingTimberTree
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import timber.log.Timber

@OptIn(UnstableApi::class)
@RunWith(AndroidJUnit4::class)
class LoggingTransferListenerTest {

    private val tree = RecordingTimberTree()
    private val source: DataSource = mock()
    private val bandwidthMeter: BandwidthMeter = mock()
    private val dataSpec = DataSpec(Uri.parse("http://127.0.0.1:4443/$HANDLE!$KEY!123/$FILE_NAME"))

    private val underTest = LoggingTransferListener(bandwidthMeter)

    @Before
    fun plantTree() {
        Timber.plant(tree)
    }

    @After
    fun uprootTree() {
        Timber.uproot(tree)
    }

    @Test
    fun `test that transfer callbacks do not log the node key or the file name`() {
        underTest.onTransferInitializing(source, dataSpec, true)
        underTest.onTransferStart(source, dataSpec, true)
        underTest.onBytesTransferred(source, dataSpec, true, 2048)
        underTest.onTransferEnd(source, dataSpec, true)

        assertThat(tree.messages).isNotEmpty()
        assertThat(tree.messages.none { it.contains(KEY) }).isTrue()
        assertThat(tree.messages.none { it.contains(FILE_NAME) }).isTrue()
    }

    private companion object {
        const val HANDLE = "aBcDeFgH"
        const val KEY = "SeCrEtNoDeKeY_0123456789abcdefghijklmnopqrs"
        const val FILE_NAME = "secret-video.mp4"
    }
}
