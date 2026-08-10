package mega.privacy.android.feature.photos.presentation.mediadiscovery

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.StaticImageFileTypeInfo
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.photos.Photo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.LocalDateTime
import kotlin.time.Duration.Companion.seconds

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CloudDriveMediaDiscoveryTakenDownClickTest {

    private val time = LocalDateTime.of(2026, 3, 16, 10, 0)

    private fun image(isTakenDown: Boolean): Photo = Photo.Image(
        id = 1L,
        parentId = 2L,
        name = "photo.jpg",
        isFavourite = false,
        creationTime = time,
        modificationTime = time,
        thumbnailFilePath = null,
        previewFilePath = null,
        fileTypeInfo = StaticImageFileTypeInfo("image/jpeg", "jpg"),
        isTakenDown = isTakenDown,
    )

    private fun video(isTakenDown: Boolean): Photo = Photo.Video(
        id = 3L,
        parentId = 2L,
        name = "video.mp4",
        isFavourite = false,
        creationTime = time,
        modificationTime = time,
        thumbnailFilePath = null,
        previewFilePath = null,
        fileTypeInfo = VideoFileTypeInfo("video/mp4", "mp4", 30.seconds),
        isTakenDown = isTakenDown,
    )

    @Test
    fun `test that shouldDisputeTakenDownOnClick returns true for a taken down photo outside selection mode`() {
        assertThat(image(isTakenDown = true).shouldDisputeTakenDownOnClick(isInSelectionMode = false))
            .isTrue()
    }

    @Test
    fun `test that shouldDisputeTakenDownOnClick returns true for a taken down video outside selection mode`() {
        assertThat(video(isTakenDown = true).shouldDisputeTakenDownOnClick(isInSelectionMode = false))
            .isTrue()
    }

    @Test
    fun `test that shouldDisputeTakenDownOnClick returns false in selection mode`() {
        assertThat(image(isTakenDown = true).shouldDisputeTakenDownOnClick(isInSelectionMode = true))
            .isFalse()
    }

    @Test
    fun `test that shouldDisputeTakenDownOnClick returns false for a photo that is not taken down`() {
        assertThat(image(isTakenDown = false).shouldDisputeTakenDownOnClick(isInSelectionMode = false))
            .isFalse()
    }
}
