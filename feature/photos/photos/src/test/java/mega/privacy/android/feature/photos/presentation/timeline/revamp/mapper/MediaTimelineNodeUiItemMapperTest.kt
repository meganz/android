package mega.privacy.android.feature.photos.presentation.timeline.revamp.mapper

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.core.formatter.mapper.DurationInSecondsTextMapper
import mega.privacy.android.domain.entity.StaticImageFileTypeInfo
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.feature.photos.model.MediaType
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import kotlin.time.Duration.Companion.seconds

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MediaTimelineNodeUiItemMapperTest {

    private val underTest = MediaTimelineNodeUiItemMapper(
        durationInSecondsTextMapper = DurationInSecondsTextMapper(),
    )

    @Test
    fun `test that an image node maps to an image item`() {
        val node = mock<TypedFileNode> {
            on { id } doReturn NodeId(7L)
            on { type } doReturn StaticImageFileTypeInfo(mimeType = "image/jpeg", extension = "jpg")
            on { modificationTime } doReturn 1000L
            on { thumbnailPath } doReturn "thumb"
            on { previewPath } doReturn "preview"
            on { isFavourite } doReturn true
            on { isMarkedSensitive } doReturn false
            on { isSensitiveInherited } doReturn false
        }

        val actual = underTest(node)

        assertThat(actual.id).isEqualTo(7L)
        assertThat(actual.key).isEqualTo(7L)
        assertThat(actual.mediaType).isEqualTo(MediaType.Image)
        assertThat(actual.fullModificationTime).isEqualTo(1000L)
        assertThat(actual.thumbnailFilePath).isEqualTo("thumb")
        assertThat(actual.previewFilePath).isEqualTo("preview")
        assertThat(actual.extension).isEqualTo("jpg")
        assertThat(actual.isFavourite).isTrue()
        assertThat(actual.isSensitive).isFalse()
        assertThat(actual.duration).isEmpty()
    }

    @Test
    fun `test that a video node maps to a video item with a formatted duration`() {
        val node = mock<TypedFileNode> {
            on { id } doReturn NodeId(9L)
            on { type } doReturn VideoFileTypeInfo(
                mimeType = "video/mp4",
                extension = "mp4",
                duration = 90.seconds,
            )
            on { modificationTime } doReturn 2000L
            on { thumbnailPath } doReturn null
            on { previewPath } doReturn null
            on { isFavourite } doReturn false
            on { isMarkedSensitive } doReturn true
            on { isSensitiveInherited } doReturn false
        }

        val actual = underTest(node)

        assertThat(actual.mediaType).isEqualTo(MediaType.Video)
        assertThat(actual.duration).isEqualTo("1:30")
        assertThat(actual.isSensitive).isTrue()
    }
}
