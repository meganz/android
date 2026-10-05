package mega.privacy.android.data.database.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.database.MegaDatabase
import mega.privacy.android.data.database.entity.UserPausedSyncEntity
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class UserPausedSyncsDaoTest {
    private lateinit var userPausedSyncsDao: UserPausedSyncsDao
    private lateinit var db: MegaDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context, MegaDatabase::class.java
        ).build()
        userPausedSyncsDao = db.userPausedSyncDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun test_that_insertPausedSync_inserts_the_entity() = runTest {
        val entity = generateEntity(syncId = 1)
        insertEntity(entity)

        val retrieved = userPausedSyncsDao.getUserPausedSync(syncId = 1)
        Truth.assertThat(retrieved).isNotNull()
        Truth.assertThat(retrieved?.syncId).isEqualTo(entity.syncId)
    }

    @Test
    fun test_that_getUserPausedSync_retrieves_the_entity() = runTest {
        val entity = generateEntity(syncId = 2)
        insertEntity(entity)

        val retrieved = userPausedSyncsDao.getUserPausedSync(syncId = 2)
        Truth.assertThat(retrieved).isNotNull()
        Truth.assertThat(retrieved?.syncId).isEqualTo(entity.syncId)
    }

    @Test
    fun test_that_deleteUserPausedSync_deletes_the_entity() = runTest {
        val entity = generateEntity(syncId = 3)
        insertEntity(entity)

        userPausedSyncsDao.deleteUserPausedSync(syncId = 3)
        val retrieved = userPausedSyncsDao.getUserPausedSync(syncId = 3)
        Truth.assertThat(retrieved).isNull()
    }

    @Test
    fun test_that_monitorUserPausedSyncIds_emits_the_inserted_ids() = runTest {
        insertEntity(generateEntity(syncId = 1))
        insertEntity(generateEntity(syncId = 2))

        userPausedSyncsDao.monitorUserPausedSyncIds().test {
            Truth.assertThat(awaitItem()).containsExactly(1L, 2L)
        }
    }

    @Test
    fun test_that_monitorUserPausedSyncIds_emits_again_after_a_delete() = runTest {
        insertEntity(generateEntity(syncId = 1))
        insertEntity(generateEntity(syncId = 2))

        userPausedSyncsDao.monitorUserPausedSyncIds().test {
            Truth.assertThat(awaitItem()).containsExactly(1L, 2L)

            userPausedSyncsDao.deleteUserPausedSync(syncId = 1)

            Truth.assertThat(awaitItem()).containsExactly(2L)
        }
    }

    @Test
    fun test_that_monitorUserPausedSyncIds_emits_an_empty_list_when_no_sync_is_paused() = runTest {
        userPausedSyncsDao.monitorUserPausedSyncIds().test {
            Truth.assertThat(awaitItem()).isEmpty()
        }
    }

    private suspend fun insertEntity(entity: UserPausedSyncEntity) {
        userPausedSyncsDao.insertPausedSync(entity)
    }

    private fun generateEntity(syncId: Long) = UserPausedSyncEntity(
        syncId = syncId,
    )
}
