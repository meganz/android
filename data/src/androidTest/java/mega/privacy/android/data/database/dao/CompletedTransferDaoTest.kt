package mega.privacy.android.data.database.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.database.MegaDatabase
import mega.privacy.android.data.database.entity.CompletedTransferEntity
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class CompletedTransferDaoTest {
    private lateinit var completedTransferDao: CompletedTransferDao
    private lateinit var db: MegaDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context, MegaDatabase::class.java
        ).build()
        completedTransferDao = db.completedTransferDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun test_that_getAll_returns_all_items() = runTest {
        val entities = (1..10).map {
            val entity = CompletedTransferEntity(
                fileName = "2023-03-24 00.13.20_1.jpg",
                type = "1",
                state = "6",
                size = "3.57 MB",
                handle = "27169983390750",
                path = "Cloud drive/Camera uploads",
                isOffline = "false",
                timestamp = "1684228012974",
                error = "No error",
                originalPath = "/data/user/0/mega.privacy.android.app/cache/cu/53132573053997.2023-03-24 00.13.20_1.jpg",
                parentHandle = "11622336899311",
            )
            completedTransferDao.insertOrUpdateCompletedTransfer(entity)
            entity
        }

        completedTransferDao.getAllCompletedTransfers().first().forEachIndexed { i, actual ->
            assertThat(actual.fileName).isEqualTo(entities[i].fileName)
            assertThat(actual.type).isEqualTo(entities[i].type)
            assertThat(actual.state).isEqualTo(entities[i].state)
            assertThat(actual.size).isEqualTo(entities[i].size)
            assertThat(actual.handle).isEqualTo(entities[i].handle)
            assertThat(actual.path).isEqualTo(entities[i].path)
            assertThat(actual.isOffline).isEqualTo(entities[i].isOffline)
            assertThat(actual.timestamp).isEqualTo(entities[i].timestamp)
            assertThat(actual.error).isEqualTo(entities[i].error)
            assertThat(actual.originalPath).isEqualTo(entities[i].originalPath)
            assertThat(actual.parentHandle).isEqualTo(entities[i].parentHandle)
        }
    }

    @Test
    fun test_that_getById_returns_the_corresponding_item() = runTest {
        val entity = CompletedTransferEntity(
            fileName = "2023-03-24 00.13.20_1.jpg",
            type = "1",
            state = "6",
            size = "3.57 MB",
            handle = "27169983390750",
            path = "Cloud drive/Camera uploads",
            isOffline = "false",
            timestamp = "1684228012974",
            error = "No error",
            originalPath = "/data/user/0/mega.privacy.android.app/cache/cu/53132573053997.2023-03-24 00.13.20_1.jpg",
            parentHandle = "11622336899311",
        )
        completedTransferDao.insertOrUpdateCompletedTransfer(entity)
        val id = completedTransferDao.getAllCompletedTransfers().first().first().id

        val actual = completedTransferDao.getCompletedTransferById(id ?: return@runTest)

        assertThat(actual?.fileName).isEqualTo(entity.fileName)
        assertThat(actual?.type).isEqualTo(entity.type)
        assertThat(actual?.state).isEqualTo(entity.state)
        assertThat(actual?.size).isEqualTo(entity.size)
        assertThat(actual?.handle).isEqualTo(entity.handle)
        assertThat(actual?.path).isEqualTo(entity.path)
        assertThat(actual?.isOffline).isEqualTo(entity.isOffline)
        assertThat(actual?.timestamp).isEqualTo(entity.timestamp)
        assertThat(actual?.error).isEqualTo(entity.error)
        assertThat(actual?.originalPath).isEqualTo(entity.originalPath)
        assertThat(actual?.parentHandle).isEqualTo(entity.parentHandle)
    }

    @Test
    fun test_that_insertOrUpdate_insert_the_corresponding_item() = runTest {
        val entity = CompletedTransferEntity(
            fileName = "2023-03-24 00.13.20_1.jpg",
            type = "1",
            state = "6",
            size = "3.57 MB",
            handle = "27169983390750",
            path = "Cloud drive/Camera uploads",
            isOffline = "false",
            timestamp = "1684228012974",
            error = "No error",
            originalPath = "/data/user/0/mega.privacy.android.app/cache/cu/53132573053997.2023-03-24 00.13.20_1.jpg",
            parentHandle = "11622336899311",
        )
        completedTransferDao.insertOrUpdateCompletedTransfer(entity)

        assertThat(completedTransferDao.getAllCompletedTransfers().first().size).isEqualTo(1)
    }

    @Test
    fun test_that_deleteAll_delete_all_items() = runTest {
        (1..10).map {
            val entity = CompletedTransferEntity(
                fileName = "2023-03-24 00.13.20_1.jpg",
                type = "1",
                state = "6",
                size = "3.57 MB",
                handle = "27169983390750",
                path = "Cloud drive/Camera uploads",
                isOffline = "false",
                timestamp = "1684228012974",
                error = "No error",
                originalPath = "/data/user/0/mega.privacy.android.app/cache/cu/53132573053997.2023-03-24 00.13.20_1.jpg",
                parentHandle = "11622336899311",
            )
            completedTransferDao.insertOrUpdateCompletedTransfer(entity)
            entity
        }

        completedTransferDao.deleteAllCompletedTransfers()

        assertThat(completedTransferDao.getAllCompletedTransfers().first()).isEmpty()
    }

    @Test
    fun test_that_deleteById_delete_the_corresponding_item() = runTest {
        (1..10).map {
            val entity = CompletedTransferEntity(
                fileName = "2023-03-24 00.13.20_1.jpg",
                type = "1",
                state = "6",
                size = "3.57 MB",
                handle = "27169983390750",
                path = "Cloud drive/Camera uploads",
                isOffline = "false",
                timestamp = "1684228012974",
                error = "No error",
                originalPath = "/data/user/0/mega.privacy.android.app/cache/cu/53132573053997.2023-03-24 00.13.20_1.jpg",
                parentHandle = "11622336899311",
            )
            completedTransferDao.insertOrUpdateCompletedTransfer(entity)
            entity
        }

        val id = completedTransferDao.getAllCompletedTransfers().first().first().id

        val entity = completedTransferDao.getCompletedTransferById(id ?: return@runTest)
        assertThat(entity).isNotNull()

        completedTransferDao.deleteCompletedTransferByIds(listOf(id))
        val actual = completedTransferDao.getCompletedTransferById(id)

        assertThat(actual).isNull()
    }


    @Test
    fun test_that_getCount_returns_the_items_count() = runTest {
        val expected = 10
        (1..expected).map {
            val entity = CompletedTransferEntity(
                fileName = "2023-03-24 00.13.20_1.jpg",
                type = "1",
                state = "6",
                size = "3.57 MB",
                handle = "27169983390750",
                path = "Cloud drive/Camera uploads",
                isOffline = "false",
                timestamp = "1684228012974",
                error = "No error",
                originalPath = "/data/user/0/mega.privacy.android.app/cache/cu/53132573053997.2023-03-24 00.13.20_1.jpg",
                parentHandle = "11622336899311",
            )
            completedTransferDao.insertOrUpdateCompletedTransfer(entity)
            entity
        }

        assertThat(completedTransferDao.getCompletedTransfersCount()).isEqualTo(expected)
    }
}
