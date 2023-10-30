package mega.privacy.android.data.database.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.database.MegaDatabase
import mega.privacy.android.data.database.entity.OfflineEntity
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class OfflineDaoTest {
    private lateinit var offlineDao: OfflineDao
    private lateinit var db: MegaDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context, MegaDatabase::class.java
        ).build()
        offlineDao = db.offlineDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    @Throws(Exception::class)
    fun `test_that_getByHandle_returns_correctly_when_add_new_offline`() = runTest {
        val offline = OfflineEntity(
            encryptedHandle = "handle",
            encryptedPath = "Some path",
            encryptedName = "Rohit",
            parentId = -1,
            encryptedType = "1",
            incoming = -1,
            encryptedIncomingHandle = "-1",
            lastModifiedTime = 0
        )
        offlineDao.insertOrUpdateOffline(offline)
        val actual = offlineDao.getOfflineByHandle("handle")
        Truth.assertThat(actual?.encryptedHandle).isEqualTo(offline.encryptedHandle)
        Truth.assertThat(actual?.encryptedName).isEqualTo(offline.encryptedName)
    }

    @Test
    @Throws(Exception::class)
    fun `test_that_getAll_returns_correctly_when_add_list_of_contact`() = runTest {
        val contacts = (1..10).map {
            val offline = OfflineEntity(
                encryptedHandle = "handle${it}",
                encryptedPath = "Some path${it}",
                encryptedName = "Rohit${it}",
                parentId = -1,
                encryptedType = "$it",
                incoming = -1,
                encryptedIncomingHandle = "$it",
                lastModifiedTime = 0
            )
            offlineDao.insertOrUpdateOffline(offline)
            offline
        }
        offlineDao.getOfflineFiles()?.forEachIndexed { i, entity ->
            Truth.assertThat(entity.encryptedHandle).isEqualTo(contacts[i].encryptedHandle)
            Truth.assertThat(entity.encryptedName).isEqualTo(contacts[i].encryptedName)
        }
    }

    @Test
    @Throws(Exception::class)
    fun `test_that_table_empty_when_call_deleteAll`() = runTest {
        (1..10).forEach {
            val offline = OfflineEntity(
                encryptedHandle = "handle${it}",
                encryptedPath = "Some path${it}",
                encryptedName = "Rohit${it}",
                parentId = -1,
                encryptedType = "$it",
                incoming = -1,
                encryptedIncomingHandle = "$it",
                lastModifiedTime = 0
            )
            offlineDao.insertOrUpdateOffline(offline)
        }
        offlineDao.deleteAllOffline()
        Truth.assertThat(offlineDao.getOfflineFiles()?.size).isEqualTo(0)
    }
}
