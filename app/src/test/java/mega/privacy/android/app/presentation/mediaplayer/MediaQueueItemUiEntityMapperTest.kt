package mega.privacy.android.app.presentation.mediaplayer

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.mediaplayer.mapper.MediaQueueItemUiEntityMapper
import mega.privacy.android.app.mediaplayer.queue.model.MediaQueueItemType
import mega.privacy.android.app.mediaplayer.queue.model.MediaQueueItemUiEntity
import mega.privacy.android.app.presentation.time.mapper.DurationInSecondsTextMapper
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.icon.pack.R
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import kotlin.time.Duration.Companion.minutes

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MediaQueueItemUiEntityMapperTest {
    private lateinit var underTest: MediaQueueItemUiEntityMapper

    private val durationInSecondsTextMapper = DurationInSecondsTextMapper()

    private val expectedId = NodeId(123456L)
    private val expectedIcon = R.drawable.ic_video_medium_solid
    private val expectedName = "video file name"
    private val expectedDuration = "10:00"
    private val expectedThumbnailPath = "video file thumbnail"
    private val expectedType = MediaQueueItemType.Playing

    @BeforeAll
    fun setUp() {
        underTest = MediaQueueItemUiEntityMapper(durationInSecondsTextMapper)
    }

    @Test
    fun `test that MediaQueueItemUiEntity can be mapped correctly`() = runTest {
        val entity = underTest(
            icon = expectedIcon,
            thumbnailFile = mock {
                on { path }.thenReturn(expectedThumbnailPath)
            },
            id = expectedId,
            name = expectedName,
            type = expectedType,
            duration = 10.minutes
        )
        assertMappedMediaQueueItemUiEntity(mediaQueueItemUiEntity = entity)
    }

    private fun assertMappedMediaQueueItemUiEntity(mediaQueueItemUiEntity: MediaQueueItemUiEntity) {
        mediaQueueItemUiEntity.let {
            Assertions.assertAll(
                "Grouped Assertions of ${mediaQueueItemUiEntity::class.simpleName}",
                { assertThat(it.icon).isEqualTo(expectedIcon) },
                { assertThat(it.id).isEqualTo(expectedId) },
                { assertThat(it.nodeName).isEqualTo(expectedName) },
                { assertThat(it.duration).isEqualTo(expectedDuration) },
                { assertThat(it.thumbnail?.path).isEqualTo(expectedThumbnailPath) },
                { assertThat(it.type).isEqualTo(expectedType) }
            )
        }
    }
}