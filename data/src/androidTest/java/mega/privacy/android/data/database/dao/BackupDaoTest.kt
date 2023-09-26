package mega.privacy.android.data.database.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.database.MegaDatabase
import mega.privacy.android.data.database.entity.BackupEntity
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class BackupDaoTest {
    private lateinit var backupDao: BackupDao
    private lateinit var db: MegaDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context, MegaDatabase::class.java
        ).build()
        backupDao = db.backupDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun test_that_insertOrUpdateBackup_insert_the_corresponding_item() = runTest {
        insertEntities()
        Truth.assertThat(backupDao.getAllBackups().size).isEqualTo(10)
    }

    @Test
    fun test_that_getBackupByType_returns_the_corresponding_items() = runTest {
        insertEntities()
        Truth.assertThat(backupDao.getBackupByType(backupType = 3, "false").size).isEqualTo(5)
    }

    @Test
    fun test_that_getBackupIdByType_returns_the_corresponding_items() = runTest {
        insertEntities()
        Truth.assertThat(backupDao.getBackupIdByType(backupType = 3, "false").size).isEqualTo(5)
    }

    @Test
    fun test_that_getBackupById_returns_the_corresponding_item() = runTest {
        insertEntities()
        val actual = backupDao.getBackupById(encryptedBackupId = "2")
        Truth.assertThat(actual.encryptedBackupId).isEqualTo("2")
    }

    @Test
    fun test_that_updateBackupAsOutdated_updates_the_corresponding_item() = runTest {
        insertEntities()
        backupDao.updateBackupAsOutdated(encryptedBackupId = "2", encryptedIsOutdated = "true")
        val actual = backupDao.getBackupById(encryptedBackupId = "2")
        Truth.assertThat(actual.encryptedIsOutdated).isEqualTo("true")
    }

    @Test
    fun test_that_insertOrUpdateBackup_updates_the_corresponding_item() = runTest {
        insertEntities()
        val newPath = "/path/to/new/folder"
        val backup =
            backupDao.getBackupById(encryptedBackupId = "2").copy(encryptedLocalFolder = newPath)
        backupDao.insertOrUpdateBackup(backup)
        val actual = backupDao.getBackupById(encryptedBackupId = "2")
        Truth.assertThat(actual.encryptedLocalFolder).isEqualTo(newPath)
    }

    @Test
    fun test_that_deleteAllBackups_deletes_all_items() = runTest {
        insertEntities()
        backupDao.deleteAllBackups()
        Truth.assertThat(backupDao.getAllBackups().size).isEqualTo(0)
    }

    @Test
    fun test_that_deleteBackupByBackupId_deletes_the_corresponding_item() = runTest {
        insertEntities()
        backupDao.deleteBackupByBackupId("2")
        Truth.assertThat(backupDao.getAllBackups().size).isEqualTo(9)
    }


    private fun generateEntities() = (1..10).map {
        val entity = BackupEntity(
            id = it,
            encryptedBackupId = it.toString(),
            backupType = if (it % 2 == 0) 3 else 4,
            encryptedTargetNode = "1234$it",
            encryptedLocalFolder = "/data/user/0/$it",
            encryptedBackupName = if (it % 2 == 0) "Camera Uploads" else "Media Uploads",
            state = it,
            subState = it,
            encryptedExtraData = "asdfg$it",
            encryptedStartTimestamp = "12345$it",
            encryptedLastFinishTimestamp = "6789$it",
            encryptedTargetFolderPath = "/data/user/0/target/$it",
            encryptedShouldExcludeSubFolders = if (it % 2 == 0) "true" else "false",
            encryptedShouldDeleteEmptySubFolders = if (it % 2 == 0) "false" else "true",
            encryptedIsOutdated = if (it % 2 == 0) "false" else "true",
        )
        entity
    }

    private suspend fun insertEntities() {
        val entities = generateEntities()
        entities.forEach { backupDao.insertOrUpdateBackup(it) }
    }
}
