package mega.privacy.android.app.presentation.videoplayer.mapper

import androidx.media3.common.PlaybackException
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.app.presentation.videoplayer.model.PlayerErrorType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PlayerErrorTypeMapperTest {

    private lateinit var underTest: PlayerErrorTypeMapper

    @BeforeEach
    fun setUp() {
        underTest = PlayerErrorTypeMapper()
    }

    @Test
    fun `test that FILE_NOT_SUPPORTED is returned when error code is ERROR_CODE_DECODING_FORMAT_UNSUPPORTED`() {
        val result = underTest(
            errorCode = PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED,
            isConnected = true,
        )
        assertThat(result).isEqualTo(PlayerErrorType.FILE_NOT_SUPPORTED)
    }

    @Test
    fun `test that FILE_NOT_SUPPORTED is returned when error code is ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED`() {
        val result = underTest(
            errorCode = PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED,
            isConnected = true,
        )
        assertThat(result).isEqualTo(PlayerErrorType.FILE_NOT_SUPPORTED)
    }

    @Test
    fun `test that FILE_NOT_SUPPORTED is returned when error code is ERROR_CODE_PARSING_MANIFEST_UNSUPPORTED`() {
        val result = underTest(
            errorCode = PlaybackException.ERROR_CODE_PARSING_MANIFEST_UNSUPPORTED,
            isConnected = true,
        )
        assertThat(result).isEqualTo(PlayerErrorType.FILE_NOT_SUPPORTED)
    }

    @Test
    fun `test that NO_NETWORK is returned when not connected and error code is not a format error`() {
        val result = underTest(
            errorCode = PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
            isConnected = false,
        )
        assertThat(result).isEqualTo(PlayerErrorType.NO_NETWORK)
    }

    @Test
    fun `test that CANNOT_PLAY is returned when connected and error code is not a format error`() {
        val result = underTest(
            errorCode = PlaybackException.ERROR_CODE_UNSPECIFIED,
            isConnected = true,
        )
        assertThat(result).isEqualTo(PlayerErrorType.CANNOT_PLAY)
    }

    @Test
    fun `test that FILE_NOT_SUPPORTED is returned when not connected but error code is a format error`() {
        val result = underTest(
            errorCode = PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED,
            isConnected = false,
        )
        assertThat(result).isEqualTo(PlayerErrorType.FILE_NOT_SUPPORTED)
    }
}
