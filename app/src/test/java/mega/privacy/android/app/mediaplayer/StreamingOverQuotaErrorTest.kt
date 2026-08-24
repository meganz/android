package mega.privacy.android.app.mediaplayer

import androidx.media3.common.PlaybackException
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.usecase.transfers.overquota.IsInTransferOverQuotaUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StreamingOverQuotaErrorTest {

    private val isInTransferOverQuotaUseCase = mock<IsInTransferOverQuotaUseCase>()

    @AfterEach
    fun resetMocks() {
        reset(isInTransferOverQuotaUseCase)
    }

    @ParameterizedTest
    @ValueSource(
        ints = [
            PlaybackException.ERROR_CODE_UNSPECIFIED,
            PlaybackException.ERROR_CODE_IO_UNSPECIFIED - 1,
            PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS + 1,
        ]
    )
    fun `test that isStreamingOverQuotaError returns false without querying the quota when the error is not an IO error`(
        errorCode: Int,
    ) = runTest {
        assertThat(isStreamingOverQuotaError(errorCode, isInTransferOverQuotaUseCase)).isFalse()

        verifyNoInteractions(isInTransferOverQuotaUseCase)
    }

    @ParameterizedTest
    @ValueSource(
        ints = [
            PlaybackException.ERROR_CODE_IO_UNSPECIFIED,
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
            PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
        ]
    )
    fun `test that isStreamingOverQuotaError returns true when an IO error occurs while over quota`(
        errorCode: Int,
    ) = runTest {
        whenever(isInTransferOverQuotaUseCase()).thenReturn(true)

        assertThat(isStreamingOverQuotaError(errorCode, isInTransferOverQuotaUseCase)).isTrue()
    }

    @Test
    fun `test that isStreamingOverQuotaError returns false when an IO error occurs while not over quota`() =
        runTest {
            whenever(isInTransferOverQuotaUseCase()).thenReturn(false)

            assertThat(
                isStreamingOverQuotaError(
                    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
                    isInTransferOverQuotaUseCase,
                )
            ).isFalse()
        }

    @Test
    fun `test that isStreamingOverQuotaError returns false when the quota query fails`() =
        runTest {
            whenever(isInTransferOverQuotaUseCase()).thenThrow(RuntimeException())

            assertThat(
                isStreamingOverQuotaError(
                    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
                    isInTransferOverQuotaUseCase,
                )
            ).isFalse()
        }
}
