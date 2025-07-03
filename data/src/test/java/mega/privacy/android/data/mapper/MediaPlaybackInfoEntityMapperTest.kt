package mega.privacy.android.data.mapper

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.database.entity.MediaPlaybackInfoEntity
import mega.privacy.android.data.model.MediaPlaybackInfo
import mega.privacy.android.data.model.MediaType
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MediaPlaybackInfoEntityMapperTest {
    private lateinit var underTest: MediaPlaybackInfoEntityMapper

    private val expectedHandle = 123456L
    private val expectedCurrentPosition = 100000L
    private val expectedTotalDuration = 654321L
    private val expectedMediaType = MediaType.Audio

    private val expectedMediaPlaybackInfo = mock<MediaPlaybackInfo> {
        on { mediaHandle }.thenReturn(expectedHandle)
        on { totalDuration }.thenReturn(expectedTotalDuration)
        on { currentPosition }.thenReturn(expectedCurrentPosition)
        on { mediaType }.thenReturn(expectedMediaType)
    }

    @BeforeAll
    fun setUp() {
        underTest = MediaPlaybackInfoEntityMapper()
    }

    @Test
    fun `test that MediaPlaybackInfoEntity can be mapped correctly`() =
        runTest {
            val item = underTest(expectedMediaPlaybackInfo)
            assertMappedMediaPlaybackInfoEntityObject(item)
        }

    private fun assertMappedMediaPlaybackInfoEntityObject(
        item: MediaPlaybackInfoEntity,
    ) {
        item.let {
            assertAll(
                "Grouped Assertions of ${MediaPlaybackInfoEntity::class.simpleName}",
                { assertThat(it.mediaHandle).isEqualTo(expectedHandle) },
                { assertThat(it.totalDuration).isEqualTo(expectedTotalDuration) },
                { assertThat(it.currentPosition).isEqualTo(expectedCurrentPosition) },
                { assertThat(it.mediaType).isEqualTo(expectedMediaType) },
            )
        }
    }
}