package mega.privacy.android.domain.usecase.transfers.active

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.transfer.InProgressTransfer
import mega.privacy.android.domain.repository.CameraUploadsRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MonitorCameraUploadsInProgressTransfersUseCaseTest {
    private lateinit var underTest: MonitorCameraUploadsInProgressTransfersUseCase

    private val cameraUploadsRepository = mock<CameraUploadsRepository>()

    @BeforeAll
    fun setUp() {
        underTest = MonitorCameraUploadsInProgressTransfersUseCase(
            cameraUploadsRepository = cameraUploadsRepository,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(cameraUploadsRepository)
    }

    @Test
    fun `test that invoke returns transferRepository in progress transfers`() {
        val expected = mock<Flow<Map<Long, InProgressTransfer>>>()
        whenever(cameraUploadsRepository.monitorCameraUploadsInProgressTransfers()).thenReturn(
            expected
        )
        assertThat(underTest.invoke()).isEqualTo(expected)
    }
}