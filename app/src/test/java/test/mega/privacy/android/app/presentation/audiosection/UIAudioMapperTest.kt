package test.mega.privacy.android.app.presentation.audiosection

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.presentation.audiosection.mapper.UIAudioMapper
import mega.privacy.android.domain.entity.node.ExportedData
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedAudioNode
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UIAudioMapperTest {
    private lateinit var underTest: UIAudioMapper

    @BeforeAll
    fun setUp() {
        underTest = UIAudioMapper()
    }

    @Test
    fun `test that UIAudio can be mapped correctly`() = runTest {
        val expectedId = NodeId(123456L)
        val expectedName = "Audio file name"
        val expectedSize: Long = 100
        val expectedDurationString = "1:40"
        val expectedThumbnail = "Audio file thumbnail"
        val expectedIsFavourite = true
        val expectedIsExported = ExportedData("link", 100)
        val expectedIsTakeDown = true
        val expectedHasVersions = true
        val expectedModificationTime: Long = 999
        val expectedLabel = 1
        val expectedAvailableOffline = true

        val expectedTypedAudioNode = mock<TypedAudioNode> {
            on { id }.thenReturn(expectedId)
            on { name }.thenReturn(expectedName)
            on { size }.thenReturn(expectedSize)
            on { isFavourite }.thenReturn(false)
            on { duration }.thenReturn(100)
            on { thumbnailPath }.thenReturn(expectedThumbnail)
            on { isFavourite }.thenReturn(expectedIsFavourite)
            on { exportedData }.thenReturn(expectedIsExported)
            on { isTakenDown }.thenReturn(expectedIsTakeDown)
            on { hasVersion }.thenReturn(expectedHasVersions)
            on { modificationTime }.thenReturn(expectedModificationTime)
            on { label }.thenReturn(expectedLabel)
            on { isAvailableOffline }.thenReturn(expectedAvailableOffline)
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
        }
    }
}