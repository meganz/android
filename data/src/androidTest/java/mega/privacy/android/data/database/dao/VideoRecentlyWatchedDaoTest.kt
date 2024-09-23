package mega.privacy.android.data.database.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.database.MegaDatabase
import mega.privacy.android.data.database.entity.VideoRecentlyWatchedEntity
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class VideoRecentlyWatchedDaoTest {
    private lateinit var videoRecentlyWatchedDao: VideoRecentlyWatchedDao
    private lateinit var db: MegaDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context, MegaDatabase::class.java
        ).build()
        videoRecentlyWatchedDao = db.videoRecentlyWatchedDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun `test_that_getAllRecentlyWatchedVideos_returns_as_expected`() = runTest {
        val entities = (1..100L).map {
            val entity = VideoRecentlyWatchedEntity(
                videoHandle = it,
                watchedTimestamp = it
            )
            videoRecentlyWatchedDao.insertOrUpdateRecentlyWatchedVideo(entity)
            entity
        }

        videoRecentlyWatchedDao.getAllRecentlyWatchedVideos().first()
            .forEachIndexed { index, entity ->
                assertThat(entity.videoHandle).isEqualTo(entities[index].videoHandle)
                assertThat(entity.watchedTimestamp).isEqualTo(entities[index].watchedTimestamp)
            }
    }

    @Test
    fun `test_that_table_empty_when_call_clearRecentlyWatchedVideos`() = runTest {
        (1..100L).map {
            val entity = VideoRecentlyWatchedEntity(
                videoHandle = it,
                watchedTimestamp = it
            )
            videoRecentlyWatchedDao.insertOrUpdateRecentlyWatchedVideo(entity)
        }

        videoRecentlyWatchedDao.clearRecentlyWatchedVideos()
        assertThat(videoRecentlyWatchedDao.getAllRecentlyWatchedVideos().first()).isEmpty()
    }

    @Test
    fun `test_that_corresponding_item_is_deleted_after_call_removeRecentlyWatchedVideo`() =
        runTest {
            val testEntities = (1..10L).map {
                VideoRecentlyWatchedEntity(
                    videoHandle = it,
                    watchedTimestamp = it
                )
            }
            val removedHandle = 5L
            videoRecentlyWatchedDao.insertOrUpdateRecentlyWatchedVideos(testEntities)
            videoRecentlyWatchedDao.getAllRecentlyWatchedVideos().first().let { entities ->
                assertThat(entities).isNotEmpty()
                assertThat(entities.size).isEqualTo(testEntities.size)
            }

            videoRecentlyWatchedDao.removeRecentlyWatchedVideo(removedHandle)
            val entities = videoRecentlyWatchedDao.getAllRecentlyWatchedVideos().first()
            assertThat(entities).isNotEmpty()
            assertThat(entities.size).isEqualTo(testEntities.size - 1)
            assertThat(entities.find { it.videoHandle == removedHandle }).isNull()
        }

    @Test
    fun `test_that_corresponding_item_is_added_after_call_insertOrUpdateRecentlyWatchedVideo`() =
        runTest {
            val expectedVideoHandle = 123456L
            val expectedTimestamp = 1000000L
            val newEntity = VideoRecentlyWatchedEntity(expectedVideoHandle, expectedTimestamp)

            videoRecentlyWatchedDao.insertOrUpdateRecentlyWatchedVideo(newEntity)
            val entities = videoRecentlyWatchedDao.getAllRecentlyWatchedVideos().first()
            assertThat(entities).isNotEmpty()
            assertThat(entities.size).isEqualTo(1)
            entities.forEach {
                assertThat(it.videoHandle).isEqualTo(expectedVideoHandle)
                assertThat(it.watchedTimestamp).isEqualTo(expectedTimestamp)
            }
        }

    @Test
    fun `test_that_corresponding_item_is_updated_after_call_insertOrUpdateRecentlyWatchedVideo_if_item_exists`() =
        runTest {
            val expectedVideoHandle = 123456L
            val expectedTimestamp = 1000000L
            val newTimestamp = 200000L
            val testEntity = VideoRecentlyWatchedEntity(expectedVideoHandle, expectedTimestamp)
            val newEntity = VideoRecentlyWatchedEntity(expectedVideoHandle, newTimestamp)
            videoRecentlyWatchedDao.insertOrUpdateRecentlyWatchedVideo(testEntity)
            videoRecentlyWatchedDao.getAllRecentlyWatchedVideos().first().let { entities ->
                assertThat(entities).isNotEmpty()
                assertThat(entities.size).isEqualTo(1)
                entities.forEach {
                    assertThat(it.videoHandle).isEqualTo(expectedVideoHandle)
                    assertThat(it.watchedTimestamp).isEqualTo(expectedTimestamp)
                }
            }

            videoRecentlyWatchedDao.insertOrUpdateRecentlyWatchedVideo(newEntity)
            videoRecentlyWatchedDao.getAllRecentlyWatchedVideos().first().let { entities ->
                assertThat(entities).isNotEmpty()
                assertThat(entities.size).isEqualTo(1)
                entities.forEach {
                    assertThat(it.videoHandle).isEqualTo(expectedVideoHandle)
                    assertThat(it.watchedTimestamp).isEqualTo(newTimestamp)
                }
            }
        }

    @Test
    fun `test_that_corresponding_items_are_added_after_call_insertOrUpdateRecentlyWatchedVideos`() =
        runTest {
            val testEntities = (1..100L).map {
                VideoRecentlyWatchedEntity(
                    videoHandle = it,
                    watchedTimestamp = it
                )
            }
            videoRecentlyWatchedDao.insertOrUpdateRecentlyWatchedVideos(testEntities)
            val entities = videoRecentlyWatchedDao.getAllRecentlyWatchedVideos().first()
            assertThat(entities).isNotEmpty()
            assertThat(entities.size).isEqualTo(testEntities.size)
            entities.forEachIndexed { index, entity ->
                assertThat(entity.videoHandle).isEqualTo(testEntities[index].videoHandle)
                assertThat(entity.watchedTimestamp).isEqualTo(testEntities[index].watchedTimestamp)
            }
        }

    @Test
    fun `test_that_corresponding_items_are_updated_after_call_insertOrUpdateRecentlyWatchedVideos_if_items_exists`() =
        runTest {
            val testEntities = (1..100L).map {
                VideoRecentlyWatchedEntity(
                    videoHandle = it,
                    watchedTimestamp = it
                )
            }
            val newEntities = (10L downTo 1).mapIndexed { index, it ->
                VideoRecentlyWatchedEntity(
                    videoHandle = it,
                    watchedTimestamp = index.toLong()
                )
            }
            videoRecentlyWatchedDao.insertOrUpdateRecentlyWatchedVideos(testEntities)
            videoRecentlyWatchedDao.getAllRecentlyWatchedVideos().first().let { entities ->
                assertThat(entities).isNotEmpty()
                assertThat(entities.size).isEqualTo(testEntities.size)
                entities.forEachIndexed { index, entity ->
                    assertThat(entity.videoHandle).isEqualTo(testEntities[index].videoHandle)
                    assertThat(entity.watchedTimestamp).isEqualTo(testEntities[index].watchedTimestamp)
                }
            }

            videoRecentlyWatchedDao.insertOrUpdateRecentlyWatchedVideos(newEntities)
            val entities = videoRecentlyWatchedDao.getAllRecentlyWatchedVideos().first()
            assertThat(entities).isNotEmpty()
            assertThat(entities.size).isEqualTo(testEntities.size)
            newEntities.map { entity ->
                assertThat(
                    entities.first {
                        it.videoHandle == entity.videoHandle
                    }.watchedTimestamp
                ).isEqualTo(
                    entity.watchedTimestamp
                )
            }
        }
}