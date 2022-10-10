package test.mega.privacy.android.app.domain.usecase

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.CameraUploadRepository
import mega.privacy.android.domain.usecase.DefaultGetGPSCoordinates
import mega.privacy.android.domain.usecase.GetGPSCoordinates
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultGetGPSCoordinatesTest {

    private lateinit var underTest: GetGPSCoordinates

    private val cameraUploadRepository = mock<CameraUploadRepository>()

    @Before
    fun setUp() {
        underTest = DefaultGetGPSCoordinates(
            cameraUploadRepository
        )
    }

    @Test
    fun `test that the GPS coordinates of file type video are retrieved`() =
        runTest {
            val result = Pair(6F, 9F)
            whenever(cameraUploadRepository.getVideoGPSCoordinates(any())).thenReturn(result)
            assertThat(underTest("", true)).isEqualTo(result)
        }

    @Test
    fun `test that the GPS coordinates of file type photo are retrieved`() =
        runTest {
            val result = Pair(6F, 9F)
            whenever(cameraUploadRepository.getPhotoGPSCoordinates(any())).thenReturn(result)
            assertThat(underTest("", false)).isEqualTo(result)
        }
}
