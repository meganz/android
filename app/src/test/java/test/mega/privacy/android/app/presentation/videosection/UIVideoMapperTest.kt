package test.mega.privacy.android.app.presentation.videosection

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.presentation.time.mapper.DurationInSecondsTextMapper
import mega.privacy.android.app.presentation.videosection.mapper.UIVideoMapper
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedVideoNode
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import kotlin.time.Duration.Companion.seconds

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UIVideoMapperTest {
    private lateinit var underTest: UIVideoMapper

    private val durationInSecondsTextMapper = DurationInSecondsTextMapper()

    @BeforeAll
    fun setUp() {
        underTest = UIVideoMapper(durationInSecondsTextMapper)
    }

    @Test
    fun `test that UIVideo can be mapped correctly`() = runTest {
        val expectedId = NodeId(123456L)
        val expectedName = "video file name"
        val expectedSize: Long = 100
        val expectedDurationString = "1:40"
        val expectedThumbnail = "video file thumbnail"
        val expectedAvailableOffline = true

        val expectedTypedVideoNode = mock<TypedVideoNode> {
            on { id }.thenReturn(expectedId)
            on { name }.thenReturn(expectedName)
            on { size }.thenReturn(expectedSize)
            on { isFavourite }.thenReturn(false)
            on { isAvailableOffline }.thenReturn(expectedAvailableOffline)
            on { duration }.thenReturn(100.seconds)
            on { thumbnailPath }.thenReturn(expectedThumbnail)
        }

        underTest(typedVideoNode = expectedTypedVideoNode).let {
            assertThat(it.id.longValue).isEqualTo(expectedId.longValue)
            assertThat(it.name).isEqualTo(expectedName)
            assertThat(it.size).isEqualTo(expectedSize)
            assertThat(it.duration).isEqualTo(expectedDurationString)
            assertThat(it.thumbnail?.path).isEqualTo(expectedThumbnail)
            assertThat(it.nodeAvailableOffline).isEqualTo(expectedAvailableOffline)
        }
    }
}