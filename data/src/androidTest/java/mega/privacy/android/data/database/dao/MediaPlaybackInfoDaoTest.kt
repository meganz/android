package mega.privacy.android.data.database.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.database.MegaDatabase
import mega.privacy.android.data.database.entity.MediaPlaybackInfoEntity
import mega.privacy.android.domain.entity.mediaplayer.MediaType
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.IOException

class MediaPlaybackInfoDaoTest {
    private lateinit var mediaPlaybackInfoDao: MediaPlaybackInfoDao
    private lateinit var db: MegaDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context, MegaDatabase::class.java
        ).build()
        mediaPlaybackInfoDao = db.mediaPlaybackInfoDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun `test_that_get_all_playback_info_functions_are_working_as_expected`() = runTest {
        val videoEntities = (1..100L).map {
            val videoEntity = MediaPlaybackInfoEntity(
                mediaHandle = it,
                mediaType = MediaType.Video,
                currentPosition = it * 1000L,
                totalDuration = it * 2000L,
            )
            mediaPlaybackInfoDao.insertOrUpdatePlaybackInfo(videoEntity)
            videoEntity
        }

        val audioEntities = (1..50L).map {
            val audioEntity = MediaPlaybackInfoEntity(
                mediaHandle = 1000 + it,
                mediaType = MediaType.Audio,
                currentPosition = it * 1000L,
                totalDuration = it * 2000L,
            )
            mediaPlaybackInfoDao.insertOrUpdatePlaybackInfo(audioEntity)
            audioEntity
        }

        val allPlaybackInfos = mediaPlaybackInfoDao.getAllPlaybackInfos().first()

        val videoPlaybackInfos =
            mediaPlaybackInfoDao.getAllPlaybackInfosByType(MediaType.Video).first()

        val audioPlaybackInfos =
            mediaPlaybackInfoDao.getAllPlaybackInfosByType(MediaType.Audio).first()

        assertThat(allPlaybackInfos.size).isEqualTo(videoPlaybackInfos.size + audioPlaybackInfos.size)

        videoPlaybackInfos.forEachIndexed { index, entity ->
            assertThat(entity.mediaHandle).isEqualTo(videoEntities[index].mediaHandle)
            assertThat(entity.mediaType).isEqualTo(videoEntities[index].mediaType)
            assertThat(entity.currentPosition).isEqualTo(videoEntities[index].currentPosition)
            assertThat(entity.totalDuration).isEqualTo(videoEntities[index].totalDuration)
        }
        assertThat(videoPlaybackInfos.size).isEqualTo(videoEntities.size)

        audioPlaybackInfos.forEachIndexed { index, entity ->
            assertThat(entity.mediaHandle).isEqualTo(audioPlaybackInfos[index].mediaHandle)
            assertThat(entity.mediaType).isEqualTo(audioPlaybackInfos[index].mediaType)
            assertThat(entity.currentPosition).isEqualTo(audioPlaybackInfos[index].currentPosition)
            assertThat(entity.totalDuration).isEqualTo(audioPlaybackInfos[index].totalDuration)
        }
        assertThat(audioPlaybackInfos.size).isEqualTo(audioEntities.size)
    }

    @Test
    fun `test_that_get_playback_info_functions_are_working_as_expected`() = runTest {
        val testAudioHandle = 1011L
        val testVideoHandle = 11L
        val videoEntities = (1..100L).map {
            val videoEntity = MediaPlaybackInfoEntity(
                mediaHandle = it,
                mediaType = MediaType.Video,
                currentPosition = it * 1000L,
                totalDuration = it * 2000L,
            )
            mediaPlaybackInfoDao.insertOrUpdatePlaybackInfo(videoEntity)
            videoEntity
        }

        val audioEntities = (1..50L).map {
            val audioEntity = MediaPlaybackInfoEntity(
                mediaHandle = 1000 + it,
                mediaType = MediaType.Audio,
                currentPosition = it * 1000L,
                totalDuration = it * 2000L,
            )
            mediaPlaybackInfoDao.insertOrUpdatePlaybackInfo(audioEntity)
            audioEntity
        }
        val videoReturnedEntity = mediaPlaybackInfoDao.getMediaPlaybackInfo(testVideoHandle)
        val audioReturnedEntity = mediaPlaybackInfoDao.getMediaPlaybackInfo(testAudioHandle)
        val nullEntity = mediaPlaybackInfoDao.getMediaPlaybackInfo(100000)

        assertThat(videoReturnedEntity).isEqualTo(
            videoEntities.firstOrNull { it.mediaHandle == testVideoHandle }
        )
        assertThat(audioReturnedEntity).isEqualTo(
            audioEntities.firstOrNull { it.mediaHandle == testAudioHandle }
        )
        assertThat(nullEntity).isNull()
    }

    @Test
    fun `test_that_clearAllPlaybackInfos_is_working_as_expected`() = runTest {
        (1..100L).map {
            val videoEntity = MediaPlaybackInfoEntity(
                mediaHandle = it,
                mediaType = MediaType.Video,
                currentPosition = it * 1000L,
                totalDuration = it * 2000L,
            )
            val audioEntity = MediaPlaybackInfoEntity(
                mediaHandle = 1000 + it,
                mediaType = MediaType.Audio,
                currentPosition = it * 1000L,
                totalDuration = it * 2000L,
            )
            mediaPlaybackInfoDao.insertOrUpdatePlaybackInfo(videoEntity)
            mediaPlaybackInfoDao.insertOrUpdatePlaybackInfo(audioEntity)
        }

        assertThat(mediaPlaybackInfoDao.getAllPlaybackInfos().first()).isNotEmpty()
        mediaPlaybackInfoDao.clearAllPlaybackInfos()
        assertThat(mediaPlaybackInfoDao.getAllPlaybackInfos().first()).isEmpty()
    }

    @Test
    fun `test_that_clearPlaybackInfosByType_is_working_as_expected`() = runTest {
        (1..100L).map {
            val videoEntity = MediaPlaybackInfoEntity(
                mediaHandle = it,
                mediaType = MediaType.Video,
                currentPosition = it * 1000L,
                totalDuration = it * 2000L,
            )
            val audioEntity = MediaPlaybackInfoEntity(
                mediaHandle = 1000 + it,
                mediaType = MediaType.Audio,
                currentPosition = it * 1000L,
                totalDuration = it * 2000L,
            )
            mediaPlaybackInfoDao.insertOrUpdatePlaybackInfo(videoEntity)
            mediaPlaybackInfoDao.insertOrUpdatePlaybackInfo(audioEntity)
        }

        assertThat(
            mediaPlaybackInfoDao.getAllPlaybackInfosByType(MediaType.Video).first()
        ).isNotEmpty()
        assertThat(
            mediaPlaybackInfoDao.getAllPlaybackInfosByType(MediaType.Audio).first()
        ).isNotEmpty()

        mediaPlaybackInfoDao.clearPlaybackInfosByType(MediaType.Video)
        assertThat(
            mediaPlaybackInfoDao.getAllPlaybackInfosByType(MediaType.Video).first()
        ).isEmpty()
        assertThat(
            mediaPlaybackInfoDao.getAllPlaybackInfosByType(MediaType.Audio).first()
        ).isNotEmpty()

        mediaPlaybackInfoDao.clearPlaybackInfosByType(MediaType.Audio)
        assertThat(
            mediaPlaybackInfoDao.getAllPlaybackInfosByType(MediaType.Video).first()
        ).isEmpty()
        assertThat(
            mediaPlaybackInfoDao.getAllPlaybackInfosByType(MediaType.Audio).first()
        ).isEmpty()
    }

    @Test
    fun `test_that_corresponding_item_is_deleted_after_call_removePlaybackInfo`() = runTest {
        val testHandle = 11L
        (1..100L).map {
            val entity = MediaPlaybackInfoEntity(
                mediaHandle = it,
                mediaType = MediaType.Video,
                currentPosition = it * 1000L,
                totalDuration = it * 2000L,
            )
            mediaPlaybackInfoDao.insertOrUpdatePlaybackInfo(entity)
        }

        assertThat(
            mediaPlaybackInfoDao.getAllPlaybackInfosByType(MediaType.Video)
                .first().firstOrNull { it.mediaHandle == testHandle }
        ).isNotNull()

        mediaPlaybackInfoDao.removePlaybackInfo(testHandle)
        assertThat(
            mediaPlaybackInfoDao.getAllPlaybackInfosByType(MediaType.Video)
                .first().firstOrNull { it.mediaHandle == testHandle }
        ).isNull()
    }

    @Test
    fun `test_that_corresponding_item_is_updated_after_call_insertOrUpdatePlaybackInfo`() =
        runTest {
            val testHandle = 11L
            (1..100L).map {
                val entity = MediaPlaybackInfoEntity(
                    mediaHandle = it,
                    mediaType = MediaType.Video,
                    currentPosition = it * 1000L,
                    totalDuration = it * 2000L,
                )
                mediaPlaybackInfoDao.insertOrUpdatePlaybackInfo(entity)
            }

            val testInfo = mediaPlaybackInfoDao.getAllPlaybackInfos().first()
                .firstOrNull { it.mediaHandle == testHandle }
            assertThat(testInfo?.mediaHandle).isEqualTo(testHandle)
            assertThat(testInfo?.mediaType).isEqualTo(MediaType.Video)
            assertThat(testInfo?.currentPosition).isEqualTo(11000L)
            assertThat(testInfo?.totalDuration).isEqualTo(22000L)

            val newEntity = MediaPlaybackInfoEntity(
                mediaHandle = testHandle,
                mediaType = MediaType.Audio,
                currentPosition = 0L,
                totalDuration = 0L,
            )
            mediaPlaybackInfoDao.insertOrUpdatePlaybackInfo(newEntity)

            val newTestInfo = mediaPlaybackInfoDao.getAllPlaybackInfos().first()
                .firstOrNull { it.mediaHandle == testHandle }
            assertThat(newTestInfo?.mediaHandle).isEqualTo(testHandle)
            assertThat(newTestInfo?.mediaType).isEqualTo(MediaType.Audio)
            assertThat(newTestInfo?.currentPosition).isEqualTo(0)
            assertThat(newTestInfo?.totalDuration).isEqualTo(0)
        }

    @Test
    fun `test_that_corresponding_items_are_updated_after_call_insertOrUpdatePlaybackInfos`() =
        runTest {
            val testHandles = (1..10L).toList()
            (1..100L).map {
                val entity = MediaPlaybackInfoEntity(
                    mediaHandle = it,
                    mediaType = MediaType.Video,
                    currentPosition = it * 1000L,
                    totalDuration = it * 2000L,
                )
                mediaPlaybackInfoDao.insertOrUpdatePlaybackInfo(entity)
            }

            testHandles.forEach { handle ->
                val testInfo = mediaPlaybackInfoDao.getAllPlaybackInfos().first()
                    .firstOrNull { it.mediaHandle == handle }
                assertThat(testInfo?.mediaHandle).isEqualTo(handle)
                assertThat(testInfo?.mediaType).isEqualTo(MediaType.Video)
                assertThat(testInfo?.currentPosition).isEqualTo(handle * 1000L)
                assertThat(testInfo?.totalDuration).isEqualTo(handle * 2000L)
            }

            testHandles.map { handle ->
                val newEntity = MediaPlaybackInfoEntity(
                    mediaHandle = handle,
                    mediaType = MediaType.Audio,
                    currentPosition = handle * 10L,
                    totalDuration = handle * 20L,
                )
                mediaPlaybackInfoDao.insertOrUpdatePlaybackInfo(newEntity)
            }

            testHandles.forEach { handle ->
                val newTestInfo = mediaPlaybackInfoDao.getAllPlaybackInfos().first()
                    .firstOrNull { it.mediaHandle == handle }
                assertThat(newTestInfo?.mediaHandle).isEqualTo(handle)
                assertThat(newTestInfo?.mediaType).isEqualTo(MediaType.Audio)
                assertThat(newTestInfo?.currentPosition).isEqualTo(handle * 10L)
                assertThat(newTestInfo?.totalDuration).isEqualTo(handle * 20L)
            }
        }
}