package mega.privacy.android.data.mapper.videosection

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.model.VideoRecentlyWatchedItem
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class VideoRecentlyWatchedItemMapperTest {
    private lateinit var underTest: VideoRecentlyWatchedItemMapper

    private val expectedHandle = 123456L
    private val expectedTimestamp = 100000L

    @BeforeAll
    fun setUp() {
        underTest = VideoRecentlyWatchedItemMapper()
    }

    @Test
    fun `test that VideoRecentlyWatchedItem can be mapped correctly`() =
        runTest {
            val item = underTest(expectedHandle, expectedTimestamp)
            assertMappedVideoRecentlyWatchedItemObject(item)
        }

    private fun assertMappedVideoRecentlyWatchedItemObject(
        item: VideoRecentlyWatchedItem,
    ) {
        item.let {
            assertAll(
                "Grouped Assertions of ${VideoRecentlyWatchedItem::class.simpleName}",
                { assertThat(it.videoHandle).isEqualTo(expectedHandle) },
                { assertThat(it.watchedTimestamp).isEqualTo(expectedTimestamp) },
            )
        }
    }
}