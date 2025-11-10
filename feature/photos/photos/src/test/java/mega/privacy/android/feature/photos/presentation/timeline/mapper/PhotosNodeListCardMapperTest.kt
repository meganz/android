package mega.privacy.android.feature.photos.presentation.timeline.mapper

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.entity.photos.PhotoDateResult
import mega.privacy.android.domain.entity.photos.PhotoResult
import mega.privacy.android.feature.photos.presentation.timeline.model.PhotosNodeListCard
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PhotosNodeListCardMapperTest {

    private lateinit var underTest: PhotosNodeListCardMapper

    @BeforeEach
    fun setup() {
        underTest = PhotosNodeListCardMapper()
    }

    @Test
    fun `test that the list of photo date results is successfully mapped`() {
        val photo = mock<Photo.Image>()
        val date = "2022-01-01"
        val photoResult = PhotoResult(photo = photo, isMarkedSensitive = false)
        val photoCount = "1"
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

        val actual = underTest(photosDateResults = photosDateResults)

        val expected = listOf(
            PhotosNodeListCard.Days(
                date = date,
                photo = photoResult,
                photosCount = photoCount
            ),
            PhotosNodeListCard.Months(
                date = date,
                photo = photoResult
            ),
            PhotosNodeListCard.Years(
                date = date,
                photo = photoResult
            )
        )
        assertThat(actual).isEqualTo(expected)
    }
}
