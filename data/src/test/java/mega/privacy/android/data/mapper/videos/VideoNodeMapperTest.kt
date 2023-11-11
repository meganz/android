package mega.privacy.android.data.mapper.videos

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.data.mapper.FileDurationMapper
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.node.FileNode
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import javax.inject.Inject

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class VideoNodeMapperTest @Inject constructor() {
    private lateinit var underTest: VideoNodeMapper

    private val durationMapper = mock<FileDurationMapper>()

    @BeforeAll
    fun setUp() {
        underTest = VideoNodeMapper(durationMapper)
    }

    @Test
    fun `test that VideoNodeMapper can be mapped correctly`() {
        val expectedDuration = 1000
        val expectedFileTypeInfo = mock<VideoFileTypeInfo>()
        val expectedFileNode = mock<FileNode> {
            on { type }.thenReturn(expectedFileTypeInfo)
        }
        whenever(durationMapper(expectedFileTypeInfo)).thenReturn(1000)
        val expectedThumbnailFilePath = "thumbnail file path testing"

        underTest(
            fileNode = expectedFileNode,
            thumbnailFilePath = expectedThumbnailFilePath
        ).let {
            assertThat(it.duration).isEqualTo(expectedDuration)
            assertThat(it.thumbnailFilePath).isEqualTo(expectedThumbnailFilePath)
        }
    }
}