package mega.privacy.android.feature.photos.presentation.timeline.mapper

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.entity.photos.PhotoDateResult
import mega.privacy.android.domain.entity.photos.PhotoResult
import mega.privacy.android.feature.photos.mapper.PhotoUiStateMapper
import mega.privacy.android.feature.photos.model.PhotoUiState
import mega.privacy.android.feature.photos.presentation.timeline.model.PhotoNodeListCardItem
import mega.privacy.android.feature.photos.presentation.timeline.model.PhotosNodeListCard
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PhotosNodeListCardMapperTest {

    private lateinit var underTest: PhotosNodeListCardMapper

    private val photoUiStateMapper: PhotoUiStateMapper = mock()

    @BeforeEach
    fun setup() {
        underTest = PhotosNodeListCardMapper(photoUiStateMapper = photoUiStateMapper)
    }

    @AfterEach
    fun tearDown() {
        reset(photoUiStateMapper)
    }

    @Test
    fun `test that the list of photo date results is successfully mapped`() {
        val photo = mock<Photo.Image>()
        val date = "2022-01-01"
        val photoResult = PhotoResult(photo = photo, isMarkedSensitive = false)
        val photoCount = 1
        val photosDateResults = listOf(
            PhotoDateResult.Day(
                date = date,
                photo = photoResult,
                photosCount = photoCount
            ),
            PhotoDateResult.Month(
                date = date,
                photo = photoResult
            ),
            PhotoDateResult.Year(
                date = date,
                photo = photoResult
            )
        )
        val photoUiState = mock<PhotoUiState.Image>()
        whenever(photoUiStateMapper(photo = any())) doReturn photoUiState

        val actual = underTest(photosDateResults = photosDateResults)

        val expected = listOf(
            PhotosNodeListCard.Days(
                date = date,
                photoItem = PhotoNodeListCardItem(
                    photo = photoUiState,
                    isMarkedSensitive = photoResult.isMarkedSensitive
                ),
                photosCount = photoCount
            ),
            PhotosNodeListCard.Months(
                date = date,
                photoItem = PhotoNodeListCardItem(
                    photo = photoUiState,
                    isMarkedSensitive = photoResult.isMarkedSensitive
                ),
            ),
            PhotosNodeListCard.Years(
                date = date,
                photoItem = PhotoNodeListCardItem(
                    photo = photoUiState,
                    isMarkedSensitive = photoResult.isMarkedSensitive
                ),
            )
        )
        assertThat(actual).isEqualTo(expected)
    }
}
