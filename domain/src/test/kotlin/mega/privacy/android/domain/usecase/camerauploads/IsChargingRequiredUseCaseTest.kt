package mega.privacy.android.domain.usecase.camerauploads

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.whenever

internal class IsChargingRequiredUseCaseTest {
    private lateinit var underTest: IsChargingRequiredUseCase

    private val getVideoCompressionSizeLimitUseCase = mock<GetVideoCompressionSizeLimitUseCase>()
    private val isChargingRequiredForVideoCompressionUseCase =
        mock<IsChargingRequiredForVideoCompressionUseCase>()

    @Before
    fun setUp() {
        underTest = IsChargingRequiredUseCase(
            getVideoCompressionSizeLimitUseCase = getVideoCompressionSizeLimitUseCase,
            isChargingRequiredForVideoCompressionUseCase = isChargingRequiredForVideoCompressionUseCase,
        )
    }

    @Test
    fun `test that false is returned if isChargingRequiredForVideoCompression is set to false`() =
        runTest {
            isChargingRequiredForVideoCompressionUseCase.stub {
                onBlocking { invoke() }.thenReturn(false)
            }

            assertThat(underTest(5)).isFalse()
        }

    @Test
    fun `test that false is returned if queue is smaller than the limit`() = runTest {
        val queueSize = 5

        whenever(getVideoCompressionSizeLimitUseCase()).thenReturn(queueSize + 1)
        isChargingRequiredForVideoCompressionUseCase.stub {
            onBlocking { invoke() }.thenReturn(true)
        }

        assertThat(underTest(queueSize.toLong())).isFalse()
    }

    @Test
    fun `test that true is returned if queue is larger than the limit`() = runTest {
        val queueSize = 5

        whenever(getVideoCompressionSizeLimitUseCase()).thenReturn(queueSize - 1)
        isChargingRequiredForVideoCompressionUseCase.stub {
            onBlocking { invoke() }.thenReturn(true)
        }

        assertThat(underTest(queueSize.toLong())).isTrue()
    }
}
