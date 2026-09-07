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
    fun `test_that_insert_and_prune_keeps_only_the_newest_rows`() = runTest {
        (1..10L).forEach { handle ->
            mediaPlaybackInfoDao.insertAndPrune(
                entity = MediaPlaybackInfoEntity(
                    mediaHandle = handle,
                    mediaType = MediaType.Video,
                    currentPosition = handle * 1000L,
                    totalDuration = handle * 2000L,
                    updatedAt = handle,
                ),
                maxItems = 3,
            )
        }

        val remaining = mediaPlaybackInfoDao.getAllPlaybackInfos().first()
        assertThat(remaining.map { it.mediaHandle }).containsExactly(10L, 9L, 8L)
    }

    @Test
    fun `test_that_insert_and_prune_keeps_every_row_when_the_count_is_below_the_limit`() = runTest {
        (1..3L).forEach { handle ->
            mediaPlaybackInfoDao.insertAndPrune(
                entity = MediaPlaybackInfoEntity(
                    mediaHandle = handle,
                    mediaType = MediaType.Video,
                    currentPosition = handle * 1000L,
                    totalDuration = handle * 2000L,
                    updatedAt = handle,
                ),
                maxItems = 3,
            )
        }

        val remaining = mediaPlaybackInfoDao.getAllPlaybackInfos().first()
        assertThat(remaining.map { it.mediaHandle }).containsExactly(1L, 2L, 3L)
    }

    @Test
    fun `test_that_rewriting_an_existing_row_never_prunes`() = runTest {
        // Build three rows under a limit that cannot trigger a prune.
        (1..3L).forEach { handle ->
            mediaPlaybackInfoDao.insertAndPrune(
                entity = MediaPlaybackInfoEntity(
                    mediaHandle = handle,
                    mediaType = MediaType.Video,
                    currentPosition = handle * 1000L,
                    totalDuration = handle * 2000L,
                    updatedAt = handle,
                ),
                maxItems = 3,
            )
        }
        assertThat(mediaPlaybackInfoDao.getAllPlaybackInfos().first()).hasSize(3)

        // Rewriting an existing row under a tighter limit: pruning here would drop a row, so all
        // three surviving proves the write skipped the prune rather than running a no-op one.
        mediaPlaybackInfoDao.insertAndPrune(
            entity = MediaPlaybackInfoEntity(
                mediaHandle = 1L,
                mediaType = MediaType.Video,
                currentPosition = 9999L,
                totalDuration = 4000L,
                updatedAt = 1L,
            ),
            maxItems = 2,
        )

        val remaining = mediaPlaybackInfoDao.getAllPlaybackInfos().first()
        assertThat(remaining.map { it.mediaHandle }).containsExactly(1L, 2L, 3L)
        assertThat(remaining.first { it.mediaHandle == 1L }.currentPosition).isEqualTo(9999L)
    }

    @Test
    fun `test_that_adding_a_new_row_prunes`() = runTest {
        (1..3L).forEach { handle ->
            mediaPlaybackInfoDao.insertAndPrune(
                entity = MediaPlaybackInfoEntity(
                    mediaHandle = handle,
                    mediaType = MediaType.Video,
                    currentPosition = handle * 1000L,
                    totalDuration = handle * 2000L,
                    updatedAt = handle,
                ),
                maxItems = 3,
            )
        }

        mediaPlaybackInfoDao.insertAndPrune(
            entity = MediaPlaybackInfoEntity(
                mediaHandle = 4L,
                mediaType = MediaType.Video,
                currentPosition = 4000L,
                totalDuration = 8000L,
                updatedAt = 4L,
            ),
            maxItems = 2,
        )

        val remaining = mediaPlaybackInfoDao.getAllPlaybackInfos().first()
        assertThat(remaining.map { it.mediaHandle }).containsExactly(4L, 3L)
    }

    @Test
    fun `test_that_prune_breaks_timestamp_ties_by_media_handle`() = runTest {
        (1..4L).forEach { handle ->
            mediaPlaybackInfoDao.insertAndPrune(
                entity = MediaPlaybackInfoEntity(
                    mediaHandle = handle,
                    mediaType = MediaType.Video,
                    currentPosition = handle * 1000L,
                    totalDuration = handle * 2000L,
                    updatedAt = 100L,
                ),
                maxItems = 2,
            )
        }

        val remaining = mediaPlaybackInfoDao.getAllPlaybackInfos().first()
        assertThat(remaining.map { it.mediaHandle }).containsExactly(4L, 3L)
    }

    @Test
    fun `test_that_migrated_rows_never_evict_a_row_written_from_live_playback`() = runTest {
        // A position saved while playing carries a real timestamp.
        mediaPlaybackInfoDao.insertAndPrune(
            entity = MediaPlaybackInfoEntity(
                mediaHandle = LIVE_HANDLE,
                mediaType = MediaType.Audio,
                currentPosition = 5_000L,
                totalDuration = 600_000L,
                updatedAt = LIVE_UPDATED_AT,
            ),
            maxItems = 2,
        )

        // The migration writes its whole batch with the zero timestamp the facade stamps on it,
        // because the legacy record carries no age of its own.
        mediaPlaybackInfoDao.insertMigratedAndPrune(
            entities = (1..3L).map { handle ->
                MediaPlaybackInfoEntity(
                    mediaHandle = handle,
                    mediaType = MediaType.Video,
                    currentPosition = handle * 1000L,
                    totalDuration = handle * 2000L,
                    updatedAt = MIGRATED_UPDATED_AT,
                )
            },
            maxItems = 2,
        )

        val remaining = mediaPlaybackInfoDao.getAllPlaybackInfos().first()
        assertThat(remaining.map { it.mediaHandle }).containsExactly(LIVE_HANDLE, 3L)
    }

    @Test
    fun `test_that_a_migrated_row_never_overwrites_one_already_in_the_store`() = runTest {
        // The dual write can land before the migration finishes reading the legacy record.
        mediaPlaybackInfoDao.insertAndPrune(
            entity = MediaPlaybackInfoEntity(
                mediaHandle = 1L,
                mediaType = MediaType.Video,
                currentPosition = 90_000L,
                totalDuration = 120_000L,
                updatedAt = LIVE_UPDATED_AT,
            ),
            maxItems = 4,
        )

        // The migration then arrives with a stale position for the same item.
        mediaPlaybackInfoDao.insertMigratedAndPrune(
            entities = listOf(
                MediaPlaybackInfoEntity(
                    mediaHandle = 1L,
                    mediaType = MediaType.Video,
                    currentPosition = 1_000L,
                    totalDuration = 120_000L,
                    updatedAt = MIGRATED_UPDATED_AT,
                ),
                MediaPlaybackInfoEntity(
                    mediaHandle = 2L,
                    mediaType = MediaType.Video,
                    currentPosition = 2_000L,
                    totalDuration = 120_000L,
                    updatedAt = MIGRATED_UPDATED_AT,
                ),
            ),
            maxItems = 4,
        )

        val stored = mediaPlaybackInfoDao.getAllPlaybackInfos().first().associateBy { it.mediaHandle }
        // The live position and its timestamp survive untouched...
        assertThat(stored[1L]?.currentPosition).isEqualTo(90_000L)
        assertThat(stored[1L]?.updatedAt).isEqualTo(LIVE_UPDATED_AT)
        // ...while an item the store did not have yet is still carried over.
        assertThat(stored[2L]?.currentPosition).isEqualTo(2_000L)
        assertThat(stored[2L]?.updatedAt).isEqualTo(MIGRATED_UPDATED_AT)
    }

    @Test
    fun `test_that_replaying_a_migrated_item_restores_its_place_in_the_retention_order`() =
        runTest {
            // Two migrated items and one position saved while playing, none of them pruned yet.
            mediaPlaybackInfoDao.insertMigratedAndPrune(
                entities = (1..2L).map { handle ->
                    MediaPlaybackInfoEntity(
                        mediaHandle = handle,
                        mediaType = MediaType.Video,
                        currentPosition = handle * 1000L,
                        totalDuration = handle * 2000L,
                        updatedAt = MIGRATED_UPDATED_AT,
                    )
                },
                maxItems = 4,
            )
            mediaPlaybackInfoDao.insertAndPrune(
                entity = MediaPlaybackInfoEntity(
                    mediaHandle = LIVE_HANDLE,
                    mediaType = MediaType.Audio,
                    currentPosition = 5_000L,
                    totalDuration = 600_000L,
                    updatedAt = LIVE_UPDATED_AT,
                ),
                maxItems = 4,
            )

            // Playing migrated item 1 again rewrites its row with a real timestamp. Rewriting an
            // existing row cannot push the table over the limit, so this write does not prune.
            mediaPlaybackInfoDao.insertAndPrune(
                entity = MediaPlaybackInfoEntity(
                    mediaHandle = 1L,
                    mediaType = MediaType.Video,
                    currentPosition = 20_000L,
                    totalDuration = 2_000L,
                    updatedAt = LIVE_UPDATED_AT + 1_000L,
                ),
                maxItems = 4,
            )

            // Opening a new item is what trims the store, and item 2 — still carrying the
            // migration timestamp — is the one that loses its place, not the replayed item 1.
            mediaPlaybackInfoDao.insertAndPrune(
                entity = MediaPlaybackInfoEntity(
                    mediaHandle = 3L,
                    mediaType = MediaType.Video,
                    currentPosition = 3_000L,
                    totalDuration = 6_000L,
                    updatedAt = LIVE_UPDATED_AT + 2_000L,
                ),
                maxItems = 3,
            )

            val remaining = mediaPlaybackInfoDao.getAllPlaybackInfos().first()
            assertThat(remaining.map { it.mediaHandle }).containsExactly(3L, 1L, LIVE_HANDLE)
        }

    private companion object {
        const val MIGRATED_UPDATED_AT = 0L
        const val LIVE_UPDATED_AT = 1_700_000_000_000L
        const val LIVE_HANDLE = 100L
    }
}
