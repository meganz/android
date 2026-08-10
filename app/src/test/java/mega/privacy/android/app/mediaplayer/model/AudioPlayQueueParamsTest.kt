package mega.privacy.android.app.mediaplayer.model

import android.content.Intent
import android.net.Uri
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_ADAPTER_TYPE
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_FILE_NAME
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_HANDLE
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_IS_PLAYLIST
import mega.privacy.android.app.utils.Constants.INVALID_VALUE
import mega.privacy.android.domain.entity.SortOrder
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

// In JVM unit tests Build.VERSION.SDK_INT == 0 (< TIRAMISU), so from() always takes the
// deprecated getSerializableExtra path. Mockito returns null for object types by default,
// which causes sortOrder to fall back to ORDER_DEFAULT_ASC.

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AudioPlayQueueParamsTest {

    @Test
    fun `test that from returns null when adapter type is invalid`() {
        val intent = buildValidIntent().also {
            whenever(it.getIntExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, INVALID_VALUE))
                .thenReturn(INVALID_VALUE)
        }

        assertThat(AudioPlayQueueParams.from(intent)).isNull()
    }

    @Test
    fun `test that from returns null when intent data is null`() {
        val intent = buildValidIntent().also {
            doReturn(null).whenever(it).data
        }

        assertThat(AudioPlayQueueParams.from(intent)).isNull()
    }

    @Test
    fun `test that from returns null when handle is invalid`() {
        val intent = buildValidIntent().also {
            whenever(it.getLongExtra(INTENT_EXTRA_KEY_HANDLE, INVALID_HANDLE))
                .thenReturn(INVALID_HANDLE)
        }

        assertThat(AudioPlayQueueParams.from(intent)).isNull()
    }

    @Test
    fun `test that from returns null when file name is null`() {
        val intent = buildValidIntent().also {
            whenever(it.getStringExtra(INTENT_EXTRA_KEY_FILE_NAME)).thenReturn(null)
        }

        assertThat(AudioPlayQueueParams.from(intent)).isNull()
    }

    @Test
    fun `test that from returns AudioPlayQueueParams when all required fields are present`() {
        val result = AudioPlayQueueParams.from(buildValidIntent())

        assertThat(result).isNotNull()
    }

    @Test
    fun `test that from maps adapter type correctly`() {
        val adapterType = 3
        val intent = buildValidIntent().also {
            whenever(it.getIntExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, INVALID_VALUE))
                .thenReturn(adapterType)
        }

        assertThat(AudioPlayQueueParams.from(intent)?.adapterType).isEqualTo(adapterType)
    }

    @Test
    fun `test that from maps handle correctly`() {
        val handle = 987L
        val intent = buildValidIntent().also {
            whenever(it.getLongExtra(INTENT_EXTRA_KEY_HANDLE, INVALID_HANDLE)).thenReturn(handle)
        }

        assertThat(AudioPlayQueueParams.from(intent)?.handle).isEqualTo(handle)
    }

    @Test
    fun `test that from maps file name correctly`() {
        val fileName = "my_track.mp3"
        val intent = buildValidIntent().also {
            whenever(it.getStringExtra(INTENT_EXTRA_KEY_FILE_NAME)).thenReturn(fileName)
        }

        assertThat(AudioPlayQueueParams.from(intent)?.fileName).isEqualTo(fileName)
    }

    @Test
    fun `test that from sets isPlayQueue to true by default`() {
        val intent = buildValidIntent().also {
            whenever(it.getBooleanExtra(INTENT_EXTRA_KEY_IS_PLAYLIST, true)).thenReturn(true)
        }

        assertThat(AudioPlayQueueParams.from(intent)?.isPlayQueue).isTrue()
    }

    @Test
    fun `test that from maps isPlayQueue to false when intent extra is false`() {
        val intent = buildValidIntent().also {
            whenever(it.getBooleanExtra(INTENT_EXTRA_KEY_IS_PLAYLIST, true)).thenReturn(false)
        }

        assertThat(AudioPlayQueueParams.from(intent)?.isPlayQueue).isFalse()
    }

    @Test
    fun `test that from sets sort order to default when serializable extra is null`() {
        // Build.VERSION.SDK_INT == 0 in JVM tests, so the deprecated path runs and
        // getSerializableExtra returns null (Mockito default), falling back to ORDER_DEFAULT_ASC.
        val result = AudioPlayQueueParams.from(buildValidIntent())

        assertThat(result?.sortOrder).isEqualTo(SortOrder.ORDER_DEFAULT_ASC)
    }

    private fun buildValidIntent(): Intent = mock<Intent>().also {
        whenever(it.getIntExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, INVALID_VALUE)).thenReturn(1)
        doReturn(mock<Uri>()).whenever(it).data
        whenever(it.getLongExtra(INTENT_EXTRA_KEY_HANDLE, INVALID_HANDLE)).thenReturn(123L)
        whenever(it.getStringExtra(INTENT_EXTRA_KEY_FILE_NAME)).thenReturn("track.mp3")
    }
}
