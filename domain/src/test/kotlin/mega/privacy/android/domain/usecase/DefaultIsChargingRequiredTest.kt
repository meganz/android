package mega.privacy.android.domain.usecase

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.CameraUploadRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultIsChargingRequiredTest {
    private lateinit var underTest: IsChargingRequired
    private val cameraUploadRepository = mock<CameraUploadRepository>()

    @Before
    fun setUp() {
        underTest = DefaultIsChargingRequired(cameraUploadRepository = cameraUploadRepository)
    }

    @Test
    fun `test that false is returned if convertOnCharfing is set to false`() = runTest {
        cameraUploadRepository.stub {
            onBlocking { convertOnCharging() }.thenReturn(false)
        }

        assertThat(underTest(5)).isFalse()
    }

    @Test
    fun `test that false is returned if queue is smaller than the limit`() = runTest {
        val queueSize = 5
        cameraUploadRepository.stub {
            onBlocking { convertOnCharging() }.thenReturn(true)
            onBlocking { getChargingOnSize() }.thenReturn(queueSize + 1)
        }

        assertThat(underTest(queueSize.toLong())).isFalse()
    }

    @Test
    fun `test that true is returned if queue is larger than the limit`() = runTest {
        val queueSize = 5
        cameraUploadRepository.stub {
            onBlocking { convertOnCharging() }.thenReturn(true)
            onBlocking { getChargingOnSize() }.thenReturn(queueSize - 1)
        }

        assertThat(underTest(queueSize.toLong())).isTrue()
    }
}