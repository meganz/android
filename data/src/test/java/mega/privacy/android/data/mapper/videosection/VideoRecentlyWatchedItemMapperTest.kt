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
    private val expectedCollectionId = 654321L
    private val expectedCollectionTitle = "collection title"

    @BeforeAll
    fun setUp() {
        underTest = VideoRecentlyWatchedItemMapper()
    }

    @Test
    fun `test that VideoRecentlyWatchedItem can be mapped correctly when collectionId is 0 and collectionTitle is null`() =
        runTest {
            val item = underTest(expectedHandle, expectedTimestamp, 0, null)
            assertMappedVideoRecentlyWatchedItemObject(item)
        }

    @Test
    fun `test that VideoRecentlyWatchedItem can be mapped correctly`() =
        runTest {
            val item = underTest(
                expectedHandle,
                expectedTimestamp,
                expectedCollectionId,
                expectedCollectionTitle
            )
            assertMappedVideoRecentlyWatchedItemObject(
                item,
                expectedCollectionId,
                expectedCollectionTitle
            )
        }

    private fun assertMappedVideoRecentlyWatchedItemObject(
        item: VideoRecentlyWatchedItem,
        collectionId: Long = 0,
        collectionTitle: String? = null,
    ) {
        item.let {
            assertAll(
                "Grouped Assertions of ${VideoRecentlyWatchedItem::class.simpleName}",
                { assertThat(it.videoHandle).isEqualTo(expectedHandle) },
                { assertThat(it.watchedTimestamp).isEqualTo(expectedTimestamp) },
                { assertThat(it.collectionId).isEqualTo(collectionId) },
                { assertThat(it.collectionTitle).isEqualTo(collectionTitle) }
            )
        }
    }
}