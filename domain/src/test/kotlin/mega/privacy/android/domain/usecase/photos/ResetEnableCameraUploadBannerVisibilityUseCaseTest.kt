package mega.privacy.android.domain.usecase.photos

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.PhotosRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ResetEnableCameraUploadBannerVisibilityUseCaseTest {

    private lateinit var underTest: ResetEnableCameraUploadBannerVisibilityUseCase

    private val photosRepository: PhotosRepository = mock()

    @BeforeEach
    fun setup() {
        underTest = ResetEnableCameraUploadBannerVisibilityUseCase(
            photosRepository = photosRepository
        )
    }

    @AfterEach
    fun tearDown() {
        reset(photosRepository)
    }

    @Test
    fun `test that the dismissal timestamp is successfully reset`() = runTest {
        underTest()

        verify(photosRepository).resetEnableCameraUploadBannerDismissedTimestamp()
    }
}
