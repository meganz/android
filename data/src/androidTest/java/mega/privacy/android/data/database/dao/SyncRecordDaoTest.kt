package mega.privacy.android.data.database.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.database.MegaDatabase
import mega.privacy.android.data.database.entity.SyncRecordEntity
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class SyncRecordDaoTest {

    private lateinit var syncRecordDao: SyncRecordDao
    private lateinit var db: MegaDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context, MegaDatabase::class.java
        ).build()
        syncRecordDao = db.syncRecordDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun test_that_insertOrUpdate_insert_the_corresponding_item() = runTest {
        val entities = generateEntities()
        entities.forEach { syncRecordDao.insertOrUpdateSyncRecord(it) }
        Truth.assertThat(syncRecordDao.getAllSyncRecords().size).isEqualTo(entities.size)
    }

    @Test
    fun test_that_updateVideoState_update_the_corresponding_items() = runTest {
        generateEntities().forEach { syncRecordDao.insertOrUpdateSyncRecord(it) }
        syncRecordDao.updateVideoState(3)
        Truth.assertThat(
            syncRecordDao.getSyncRecordsBySyncStateAndType(
                syncState = 3,
                syncType = 2
            ).size
        ).isEqualTo(5)
    }

    @Test
    fun test_that_getSyncRecordCountByFileName_returns_the_items_count() = runTest {
        generateEntities().forEach { syncRecordDao.insertOrUpdateSyncRecord(it) }
        Truth.assertThat(
            syncRecordDao.getSyncRecordCountByFileName(
                fileName = "2023-07-25 00.13.20_1_2.jpg",
                secondary = "true"
            )
        ).isEqualTo(1)
        Truth.assertThat(
            syncRecordDao.getSyncRecordCountByFileName(
                fileName = "2023-07-25 00.13.20_1_2.jpg",
                secondary = "false"
            )
        ).isEqualTo(0)
    }

    @Test
    fun test_that_getSyncRecordCountByOriginalPath_returns_the_items_count() = runTest {
        generateEntities().forEach { syncRecordDao.insertOrUpdateSyncRecord(it) }
        Truth.assertThat(
            syncRecordDao.getSyncRecordCountByOriginalPath(
                originalPath = "Cloud drive/Camera uploads/53132573053997.2023-03-24 00.13.20_1.jpg",
                secondary = "false"
            )
        ).isEqualTo(1)
    }

    @Test
    fun test_that_getSyncRecordByOriginalFingerprint_returns_the_items_count() = runTest {
        val entities = generateEntities()
        entities.forEach { syncRecordDao.insertOrUpdateSyncRecord(it) }
        Truth.assertThat(
            syncRecordDao.getSyncRecordByOriginalFingerprint(
                originalFingerprint = "adlkfjalsdkfj10",
                secondary = "true",
                copyOnly = "false",
            )
        ).isEqualTo(entities[9].copy(id = 10))
    }

    @Test
    fun test_that_getSyncRecordByOriginalPathAndIsSecondary_returns_the_corresponding_item() =
        runTest {
            val entities = generateEntities()
            entities.forEach { syncRecordDao.insertOrUpdateSyncRecord(it) }
            Truth.assertThat(
                syncRecordDao.getSyncRecordByOriginalPathAndIsSecondary(
                    originalPath = "Cloud drive/Camera uploads/53132573053997.2023-03-24 00.13.20_5.jpg",
                    secondary = "false",
                )
            ).isEqualTo(entities[4].copy(id = 5))
        }

    @Test
    fun test_that_getSyncRecordByNewPath_returns_the_corresponding_item() =
        runTest {
            val entities = generateEntities()
            entities.forEach { syncRecordDao.insertOrUpdateSyncRecord(it) }
            Truth.assertThat(
                syncRecordDao.getSyncRecordByNewPath(
                    newPath = "/data/user/0/mega.privacy.android.app/cache/cu/53132573053997.2023-03-24 00.13.20_3.jpg",
                )
            ).isEqualTo(entities[2].copy(id = 3))
        }

    @Test
    fun test_that_getSyncRecordsBySyncState_returns_the_corresponding_item() =
        runTest {
            val entities = generateEntities(3)
            entities.forEach { syncRecordDao.insertOrUpdateSyncRecord(it) }
            Truth.assertThat(syncRecordDao.getSyncRecordsBySyncState(syncState = 3).size)
                .isEqualTo(10)
        }

    @Test
    fun test_that_getSyncRecordsBySyncStateAndType_returns_the_corresponding_items() =
        runTest {
            val entities = generateEntities(3)
            entities.forEach { syncRecordDao.insertOrUpdateSyncRecord(it) }
            Truth.assertThat(
                syncRecordDao.getSyncRecordsBySyncStateAndType(
                    syncState = 3,
                    syncType = 2
                ).size
            ).isEqualTo(5)
        }

    @Test
    fun test_that_deleteAllSyncRecords_deletes_the_corresponding_items() =
        runTest {
            val entities = generateEntities()
            entities.forEach { syncRecordDao.insertOrUpdateSyncRecord(it) }
            syncRecordDao.deleteAllSyncRecords()
            Truth.assertThat(syncRecordDao.getAllSyncRecords().size)
                .isEqualTo(0)
        }

    @Test
    fun test_that_deleteSyncRecordsByType_deletes_the_corresponding_items() =
        runTest {
            val entities = generateEntities(0)
            entities.forEach { syncRecordDao.insertOrUpdateSyncRecord(it) }
            syncRecordDao.deleteSyncRecordsByType(syncType = 2)
            Truth.assertThat(
                syncRecordDao.getSyncRecordsBySyncStateAndType(
                    syncState = 0,
                    syncType = 2
                ).size
            ).isEqualTo(0)
        }

    @Test
    fun test_that_deleteSyncRecordsByIsSecondary_deletes_the_corresponding_items() =
        runTest {
            val entities = generateEntities(0)
            entities.forEach { syncRecordDao.insertOrUpdateSyncRecord(it) }
            syncRecordDao.deleteSyncRecordsByIsSecondary(secondary = "false")
            syncRecordDao.getAllSyncRecords().forEach {
                Truth.assertThat(it.isSecondary).isEqualTo("true")
            }
        }

    @Test
    fun test_that_deleteSyncRecordByOriginalPathOrNewPathAndIsSecondary_deletes_the_corresponding_items() =
        runTest {
            val entities = generateEntities(0)
            entities.forEach { syncRecordDao.insertOrUpdateSyncRecord(it) }
            syncRecordDao.deleteSyncRecordByOriginalPathOrNewPathAndIsSecondary(
                path = "Cloud drive/Camera uploads/53132573053997.2023-03-24 00.13.20_1.jpg",
                secondary = "false"
            )
            syncRecordDao.deleteSyncRecordByOriginalPathOrNewPathAndIsSecondary(
                path = "/data/user/0/mega.privacy.android.app/cache/cu/53132573053997.2023-03-24 00.13.20_2.jpg",
                secondary = "true"
            )
            Truth.assertThat(syncRecordDao.getAllSyncRecords().size).isEqualTo(8)
        }

    @Test
    fun test_that_deleteSyncRecordByOriginalPathAndIsSecondary_deletes_the_corresponding_items() =
        runTest {
            val entities = generateEntities(0)
            entities.forEach { syncRecordDao.insertOrUpdateSyncRecord(it) }
            syncRecordDao.deleteSyncRecordByOriginalPathAndIsSecondary(
                originalPath = "Cloud drive/Camera uploads/53132573053997.2023-03-24 00.13.20_1.jpg",
                secondary = "false"
            )
            Truth.assertThat(syncRecordDao.getAllSyncRecords().size).isEqualTo(9)
        }

    @Test
    fun test_that_deleteSyncRecordByNewPath_deletes_the_corresponding_items() =
        runTest {
            val entities = generateEntities(0)
            entities.forEach { syncRecordDao.insertOrUpdateSyncRecord(it) }
            syncRecordDao.deleteSyncRecordByNewPath(
                newPath = "/data/user/0/mega.privacy.android.app/cache/cu/53132573053997.2023-03-24 00.13.20_2.jpg",
            )
            Truth.assertThat(syncRecordDao.getAllSyncRecords().size).isEqualTo(9)
        }

    @Test
    fun test_that_deleteSyncRecordByFileName_deletes_the_corresponding_items() =
        runTest {
            val entities = generateEntities(0)
            entities.forEach { syncRecordDao.insertOrUpdateSyncRecord(it) }
            syncRecordDao.deleteSyncRecordByFileName(fileName = "2023-07-25 00.13.20_1_1.jpg")
            Truth.assertThat(syncRecordDao.getAllSyncRecords().size).isEqualTo(9)
        }

    @Test
    fun test_that_deleteSyncRecordByFingerprintsAndIsSecondary_deletes_the_corresponding_items() =
        runTest {
            val entities = generateEntities(0)
            entities.forEach { syncRecordDao.insertOrUpdateSyncRecord(it) }
            syncRecordDao.deleteSyncRecordByFingerprintsAndIsSecondary(
                originalFingerPrint = "adlkfjalsdkfj1",
                newFingerprint = "adlkfjalsdkfj1",
                secondary = "false"
            )
            Truth.assertThat(syncRecordDao.getAllSyncRecords().size).isEqualTo(9)
        }

    @Test
    fun test_that_getAllTimestampsByIsSecondaryAndSyncType_returns_the_corresponding_items() =
        runTest {
            val entities = generateEntities(0)
            entities.forEach { syncRecordDao.insertOrUpdateSyncRecord(it) }
            Truth.assertThat(
                syncRecordDao.getAllTimestampsByIsSecondaryAndSyncType(
                    secondary = "false",
                    syncType = 2
                ).size
            ).isEqualTo(5)
        }


    private fun generateEntities(state: Int = 0) = (1..10).map {
        val entity = SyncRecordEntity(
            originalPath = "Cloud drive/Camera uploads/53132573053997.2023-03-24 00.13.20_$it.jpg",
            newPath = "/data/user/0/mega.privacy.android.app/cache/cu/53132573053997.2023-03-24 00.13.20_$it.jpg",
            originalFingerPrint = "adlkfjalsdkfj$it",
            newFingerprint = "adlkfjalsdkfjsdf$it",
            timestamp = "1684228012974$it",
            fileName = "2023-07-25 00.13.20_1_$it.jpg",
            longitude = "1.684228E7$it",
            latitude = "1.684228E7$it",
            state = state,
            type = if (it % 2 == 0) 1 else 2,
            nodeHandle = "11622336899311$it",
            isCopyOnly = if (it % 2 != 0) "true" else "false",
            isSecondary = if (it % 2 == 0) "true" else "false",
        )
        entity
    }
}
