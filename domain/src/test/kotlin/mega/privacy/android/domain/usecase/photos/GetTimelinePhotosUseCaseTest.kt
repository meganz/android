package mega.privacy.android.domain.usecase.photos

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.repository.PhotosRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class GetTimelinePhotosUseCaseTest {
    private lateinit var underTest: GetTimelinePhotosUseCase

    private val photosRepository = mock<PhotosRepository>()

    @Before
    fun setUp() {
        underTest = GetTimelinePhotosUseCase(
            photosRepository = photosRepository,
        )
    }

    @Test
    fun `test that current photos are returned`() = runTest {
        val photo = mock<Photo>()
        val expected = listOf(photo)

        whenever(photosRepository.monitorPhotos()).thenReturn(
            flowOf(expected)
        )

        underTest().test {
            assertThat(awaitItem()).isEqualTo(expected)
            cancelAndIgnoreRemainingEvents()
        }
    }
}