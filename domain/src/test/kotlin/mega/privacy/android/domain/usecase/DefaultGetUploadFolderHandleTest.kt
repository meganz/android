package mega.privacy.android.domain.usecase

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.CameraUploadRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultGetUploadFolderHandleTest {
    private lateinit var underTest: GetUploadFolderHandle
    private val cameraUploadRepository = mock<CameraUploadRepository>()

    @Before
    fun setUp() {
        underTest = DefaultGetUploadFolderHandle(
            cameraUploadRepository = cameraUploadRepository
        )
    }

    @Test
    fun `test that invoke with true returns Primary Upload Folder's handle `() = runTest {
        val primaryHandle = 123456789L
        whenever(cameraUploadRepository.getPrimarySyncHandle()).thenReturn(primaryHandle)
        assertThat(underTest(true)).isEqualTo(primaryHandle)
    }

    @Test
    fun `test that invoke with false returns Secondary Upload Folder's handle `() = runTest {
        val secondaryHandle = 123456789L
        whenever(cameraUploadRepository.getSecondarySyncHandle()).thenReturn(secondaryHandle)
        assertThat(underTest(false)).isEqualTo(secondaryHandle)
    }

    @Test
    fun `test that invoke with true returns Invalid Handle when Primary Upload Handle is null `() =
        runTest {
            val invalidHandle = 0L
            whenever(cameraUploadRepository.getPrimarySyncHandle()).thenReturn(null)
            whenever(cameraUploadRepository.getInvalidHandle()).thenReturn(invalidHandle)
            assertThat(underTest(true)).isEqualTo(invalidHandle)
            verify(cameraUploadRepository, times(1)).getPrimarySyncHandle()
        }

    @Test
    fun `test that invoke with false returns Invalid Handle when Secondary Upload Handle is null `() =
        runTest {
            val invalidHandle = 0L
            whenever(cameraUploadRepository.getSecondarySyncHandle()).thenReturn(null)
            whenever(cameraUploadRepository.getInvalidHandle()).thenReturn(invalidHandle)
            assertThat(underTest(false)).isEqualTo(invalidHandle)
            verify(cameraUploadRepository, times(1)).getSecondarySyncHandle()
        }
}