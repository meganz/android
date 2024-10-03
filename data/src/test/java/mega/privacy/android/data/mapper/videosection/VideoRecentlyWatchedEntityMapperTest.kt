package mega.privacy.android.data.mapper.videosection

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.database.entity.VideoRecentlyWatchedEntity
import mega.privacy.android.data.model.VideoRecentlyWatchedItem
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class VideoRecentlyWatchedEntityMapperTest {
    private lateinit var underTest: VideoRecentlyWatchedEntityMapper

    private val expectedHandle = 123456L
    private val expectedTimestamp = 100000L
    private val expectedCollectionId = 654321L
    private val expectedCollectionTitle = "collection title"
    private val expectedVideoRecentlyWatchedItem = mock<VideoRecentlyWatchedItem> {
        on { videoHandle }.thenReturn(expectedHandle)
        on { watchedTimestamp }.thenReturn(expectedTimestamp)
        on { collectionId }.thenReturn(expectedCollectionId)
        on { collectionTitle }.thenReturn(expectedCollectionTitle)
    }
    private val videoRecentlyWatchedItemWithoutCollectionIdAndTitle =
        mock<VideoRecentlyWatchedItem> {
            on { videoHandle }.thenReturn(expectedHandle)
            on { watchedTimestamp }.thenReturn(expectedTimestamp)
            on { collectionId }.thenReturn(0)
            on { collectionTitle }.thenReturn(null)
        }

    @BeforeAll
    fun setUp() {
        underTest = VideoRecentlyWatchedEntityMapper()
    }

    @Test
    fun `test that VideoRecentlyWatchedItem can be mapped correctly when collectionId is 0 and collectionTitle is null`() =
        runTest {
            val item = underTest(videoRecentlyWatchedItemWithoutCollectionIdAndTitle)
            assertMappedVideoRecentlyWatchedItemObject(item)
        }

    @Test
    fun `test that VideoRecentlyWatchedItem can be mapped correctly`() =
        runTest {
            val item = underTest(expectedVideoRecentlyWatchedItem)
            assertMappedVideoRecentlyWatchedItemObject(
                item,
                expectedCollectionId,
                expectedCollectionTitle
            )
        }

    private fun assertMappedVideoRecentlyWatchedItemObject(
        item: VideoRecentlyWatchedEntity,
        collectionId: Long = 0,
        collectionTitle: String? = null,
    ) {
        item.let {
            assertAll(
                "Grouped Assertions of ${VideoRecentlyWatchedEntity::class.simpleName}",
                { assertThat(it.videoHandle).isEqualTo(expectedHandle) },
                { assertThat(it.watchedTimestamp).isEqualTo(expectedTimestamp) },
                { assertThat(it.collectionId).isEqualTo(collectionId) },
                { assertThat(it.collectionTitle).isEqualTo(collectionTitle) },
            )
        }
    }
}