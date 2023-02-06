package mega.privacy.android.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.CameraUploadRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultResetTotalUploadsTest {
    private lateinit var underTest: ResetTotalUploads

    private val hasPendingUploads = mock<HasPendingUploads>()
    private val cameraUploadRepository = mock<CameraUploadRepository>()

    @Before
    fun setUp() {
        underTest = DefaultResetTotalUploads(
            hasPendingUploads = hasPendingUploads,
            cameraUploadRepository = cameraUploadRepository,
        )
    }

    @Test
    fun `test that resetTotalUploads is invoked when there are no pending uploads`() = runTest {
        whenever(hasPendingUploads()).thenReturn(false)

        underTest()

        verify(cameraUploadRepository).resetTotalUploads()
    }

    @Test
    fun `test that resetTotalUploads is not invoked when there are pending uploads`() = runTest {
        whenever(hasPendingUploads()).thenReturn(true)

        underTest()

        verifyNoInteractions(cameraUploadRepository)
    }
}