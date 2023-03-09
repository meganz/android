package mega.privacy.android.domain.usecase

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.CameraUploadRepository
import mega.privacy.android.domain.usecase.camerauploads.IsChargingRequiredForVideoCompression
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub

/**
 * Test class for [DefaultIsChargingRequired]
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DefaultIsChargingRequiredTest {
    private lateinit var underTest: IsChargingRequired

    private val cameraUploadRepository = mock<CameraUploadRepository>()
    private val isChargingRequiredForVideoCompression =
        mock<IsChargingRequiredForVideoCompression>()

    @Before
    fun setUp() {
        underTest = DefaultIsChargingRequired(
            cameraUploadRepository = cameraUploadRepository,
            isChargingRequiredForVideoCompression = isChargingRequiredForVideoCompression,
        )
    }

    @Test
    fun `test that false is returned if isChargingRequiredForVideoCompression is set to false`() =
        runTest {
            isChargingRequiredForVideoCompression.stub {
                onBlocking { invoke() }.thenReturn(false)
            }

            assertThat(underTest(5)).isFalse()
        }

    @Test
    fun `test that false is returned if queue is smaller than the limit`() = runTest {
        val queueSize = 5

        cameraUploadRepository.stub {
            onBlocking { getChargingOnSize() }.thenReturn(queueSize + 1)
        }
        isChargingRequiredForVideoCompression.stub {
            onBlocking { invoke() }.thenReturn(true)
        }

        assertThat(underTest(queueSize.toLong())).isFalse()
    }

    @Test
    fun `test that true is returned if queue is larger than the limit`() = runTest {
        val queueSize = 5

        cameraUploadRepository.stub {
            onBlocking { getChargingOnSize() }.thenReturn(queueSize - 1)
        }
        isChargingRequiredForVideoCompression.stub {
            onBlocking { invoke() }.thenReturn(true)
        }

        assertThat(underTest(queueSize.toLong())).isTrue()
    }
}