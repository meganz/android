package mega.privacy.android.data.mapper

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.database.entity.MediaPlaybackInfoEntity
import mega.privacy.android.domain.entity.mediaplayer.MediaPlaybackInfo
import mega.privacy.android.domain.entity.mediaplayer.MediaType
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.mock
import org.mockito.kotlin.mock

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MediaPlaybackInfoMapperTest {
    private lateinit var underTest: MediaPlaybackInfoMapper

    private val expectedHandle = 123456L
    private val expectedCurrentPosition = 100000L
    private val expectedTotalDuration = 654321L
    private val expectedMediaType = MediaType.Audio

    @BeforeAll
    fun setUp() {
        underTest = MediaPlaybackInfoMapper()
    }

    @Test
    fun `test that MediaPlaybackInfo can be mapped correctly`() = runTest {
        val entity = mock<MediaPlaybackInfoEntity> {
            on { mediaHandle }.thenReturn(expectedHandle)
            on { totalDuration }.thenReturn(expectedTotalDuration)
            on { currentPosition }.thenReturn(expectedCurrentPosition)
            on { mediaType }.thenReturn(expectedMediaType)
        }
        val item = underTest(entity)
        assertMappedMediaPlaybackInfoObject(item)
    }

    private fun assertMappedMediaPlaybackInfoObject(
        item: MediaPlaybackInfo,
    ) {
        item.let {
            assertAll(
                "Grouped Assertions of ${MediaPlaybackInfo::class.simpleName}",
                { assertThat(it.mediaHandle).isEqualTo(expectedHandle) },
                { assertThat(it.currentPosition).isEqualTo(expectedCurrentPosition) },
                { assertThat(it.totalDuration).isEqualTo(expectedTotalDuration) },
                { assertThat(it.mediaType).isEqualTo(expectedMediaType) }
            )
        }
    }
}