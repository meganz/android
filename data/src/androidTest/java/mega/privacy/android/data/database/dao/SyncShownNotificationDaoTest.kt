package mega.privacy.android.data.database.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.database.MegaDatabase
import mega.privacy.android.data.database.entity.SyncShownNotificationEntity
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SyncShownNotificationDaoTest {
    private lateinit var syncShownNotificationDao: SyncShownNotificationDao
    private lateinit var db: MegaDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context, MegaDatabase::class.java
        ).build()
        syncShownNotificationDao = db.syncShownNotificationDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun test_that_insertSolvedIssue_inserts_the_entity() = runTest {
        val entity = generateEntity()
        insertEntity(entity)

        val result =
            syncShownNotificationDao.getSyncNotificationByType(entity.notificationType).first()

        assertThat(result).isEqualTo(entity)
    }

    @Test
    fun test_that_deleteSyncNotificationByType_deletes_the_entity() = runTest {
        val entity1 = generateEntity(id = 321, notificationType = "error")
        val entity2 = generateEntity(id = 123, notificationType = "stalled issue")
        insertEntity(entity1)
        insertEntity(entity2)

        syncShownNotificationDao.deleteSyncNotificationByType("error")

        assertThat(syncShownNotificationDao.getSyncNotificationByType("error")).isEmpty()
    }

    private fun generateEntity(
        id: Int = 321,
        notificationType: String = "error",
    ) = SyncShownNotificationEntity(
        id = id,
        notificationType = notificationType,
    )

    private suspend fun insertEntity(entity: SyncShownNotificationEntity) {
        syncShownNotificationDao.insertSyncNotification(entity)
    }
}