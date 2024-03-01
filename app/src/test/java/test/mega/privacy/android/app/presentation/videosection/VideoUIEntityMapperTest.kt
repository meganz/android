package test.mega.privacy.android.app.presentation.videosection

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.presentation.time.mapper.DurationInSecondsTextMapper
import mega.privacy.android.app.presentation.videosection.mapper.VideoUIEntityMapper
import mega.privacy.android.app.presentation.videosection.model.VideoUIEntity
import mega.privacy.android.domain.entity.node.ExportedData
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedVideoNode
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import kotlin.time.Duration.Companion.minutes

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class VideoUIEntityMapperTest {
    private lateinit var underTest: VideoUIEntityMapper

    private val durationInSecondsTextMapper = DurationInSecondsTextMapper()

    private val expectedId = NodeId(123456L)
    private val expectedParentId = NodeId(654321L)
    private val expectedName = "video file name"
    private val expectedSize: Long = 100
    private val expectedDurationString = "10:00"
    private val expectedThumbnail = "video file thumbnail"
    private val expectedAvailableOffline = true
    private val expectedDurationTime = 10.minutes
    private val expectedIsFavourite = false
    private val expectedExportedData = mock<ExportedData>()

    @BeforeAll
    fun setUp() {
        underTest = VideoUIEntityMapper(durationInSecondsTextMapper)
    }

    @Test
    fun `test that VideoUIEntity can be mapped correctly`() = runTest {
        val testNode = initTypedVideoNode(expectedExportedData)
        val videoUIEntity = underTest(testNode)
        assertMappedVideoUIEntity(
            videoUIEntity = videoUIEntity,
            expectedIsShared = true
        )
    }

    @Test
    fun `test that VideoUIEntity can be mapped correctly when exportedData is null`() = runTest {
        val testNode = initTypedVideoNode(null)
        val videoUIEntity = underTest(testNode)
        assertMappedVideoUIEntity(
            videoUIEntity = videoUIEntity,
            expectedIsShared = false
        )
    }

    private fun initTypedVideoNode(
        exportData: ExportedData?,
    ) = mock<TypedVideoNode> {
        on { id }.thenReturn(expectedId)
        on { parentId }.thenReturn(expectedParentId)
        on { name }.thenReturn(expectedName)
        on { size }.thenReturn(expectedSize)
        on { isFavourite }.thenReturn(expectedIsFavourite)
        on { isAvailableOffline }.thenReturn(expectedAvailableOffline)
        on { duration }.thenReturn(expectedDurationTime)
        on { thumbnailPath }.thenReturn(expectedThumbnail)
        on { exportedData }.thenReturn(exportData)
    }

    private fun assertMappedVideoUIEntity(
        videoUIEntity: VideoUIEntity,
        expectedIsShared: Boolean,
    ) {
        videoUIEntity.let {
            Assertions.assertAll(
                "Grouped Assertions of ${videoUIEntity::class.simpleName}",
                { assertThat(it.id).isEqualTo(expectedId) },
                { assertThat(it.parentId).isEqualTo(expectedParentId) },
                { assertThat(it.name).isEqualTo(expectedName) },
                { assertThat(it.size).isEqualTo(expectedSize) },
                { assertThat(it.durationString).isEqualTo(expectedDurationString) },
                { assertThat(it.durationInMinutes).isEqualTo(10) },
                { assertThat(it.thumbnail?.path).isEqualTo(expectedThumbnail) },
                { assertThat(it.nodeAvailableOffline).isEqualTo(expectedAvailableOffline) },
                { assertThat(it.isFavourite).isEqualTo(expectedIsFavourite) },
                { assertThat(it.isSharedItems).isEqualTo(expectedIsShared) },
            )
        }
    }
}