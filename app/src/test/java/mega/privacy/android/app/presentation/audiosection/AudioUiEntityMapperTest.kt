package mega.privacy.android.app.presentation.audiosection

import com.google.common.truth.Truth
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.presentation.audiosection.mapper.AudioUiEntityMapper
import mega.privacy.android.core.formatter.mapper.DurationInSecondsTextMapper
import mega.privacy.android.domain.entity.AudioFileTypeInfo
import mega.privacy.android.domain.entity.node.ExportedData
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedAudioNode
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import kotlin.time.Duration.Companion.seconds

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AudioUiEntityMapperTest {
    private lateinit var underTest: AudioUiEntityMapper

    private val durationInSecondsTextMapper = DurationInSecondsTextMapper()

    @BeforeAll
    fun setUp() {
        underTest = AudioUiEntityMapper(durationInSecondsTextMapper)
    }

    @Test
    fun `test that AudioUiEntity can be mapped correctly`() = runTest {
        val expectedId = NodeId(123456L)
        val expectedName = "Audio file name"
        val expectedSize: Long = 100
        val expectedDurationString = "1:40"
        val expectedThumbnail = "Audio file thumbnail"
        val expectedIsFavourite = true
        val expectedIsExported = ExportedData("link", 100)
        val expectedIsTakeDown = true
        val expectedNumVersions = 2
        val expectedModificationTime: Long = 999
        val expectedLabel = 1
        val expectedAvailableOffline = true
        val expectedType = mock<AudioFileTypeInfo>()

        val expectedTypedAudioNode = mock<TypedAudioNode> {
            on { id }.thenReturn(expectedId)
            on { name }.thenReturn(expectedName)
            on { size }.thenReturn(expectedSize)
            on { isFavourite }.thenReturn(false)
            on { duration }.thenReturn(100.seconds)
            on { thumbnailPath }.thenReturn(expectedThumbnail)
            on { isFavourite }.thenReturn(expectedIsFavourite)
            on { exportedData }.thenReturn(expectedIsExported)
            on { isTakenDown }.thenReturn(expectedIsTakeDown)
            on { versionCount }.thenReturn(expectedNumVersions - 1)
            on { modificationTime }.thenReturn(expectedModificationTime)
            on { label }.thenReturn(expectedLabel)
            on { isAvailableOffline }.thenReturn(expectedAvailableOffline)
            on { hasVersion }.thenReturn(true)
            on { type }.thenReturn(expectedType)
        }

        underTest(typedAudioNode = expectedTypedAudioNode).let {
            Truth.assertThat(it.id.longValue).isEqualTo(expectedId.longValue)
            Truth.assertThat(it.name).isEqualTo(expectedName)
            Truth.assertThat(it.size).isEqualTo(expectedSize)
            Truth.assertThat(it.duration).isEqualTo(expectedDurationString)
            Truth.assertThat(it.thumbnail?.path).isEqualTo(expectedThumbnail)
            Truth.assertThat(it.isFavourite).isTrue()
            Truth.assertThat(it.isExported).isTrue()
            Truth.assertThat(it.isTakenDown).isTrue()
            Truth.assertThat(it.hasVersions).isTrue()
            Truth.assertThat(it.modificationTime).isEqualTo(expectedModificationTime)
            Truth.assertThat(it.label).isEqualTo(expectedLabel)
            Truth.assertThat(it.nodeAvailableOffline).isEqualTo(expectedAvailableOffline)
            Truth.assertThat(it.fileTypeInfo).isEqualTo(expectedType)
        }
    }
}