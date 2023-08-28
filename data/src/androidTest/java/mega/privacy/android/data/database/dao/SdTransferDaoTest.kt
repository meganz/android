package mega.privacy.android.data.database.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.database.MegaDatabase
import mega.privacy.android.data.database.entity.SdTransferEntity
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class SdTransferDaoTest {
    private lateinit var sdTransferDao: SdTransferDao
    private lateinit var db: MegaDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context, MegaDatabase::class.java
        ).build()
        sdTransferDao = db.sdTransferDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun test_that_insert_SDTransferEntity_correctly() = runTest {
        val sdTransfer = SdTransferEntity(
            tag = 1,
            encryptedName = "encryptedName",
            encryptedAppData = "encryptedAppData",
            encryptedHandle = "encryptedHandle",
            encryptedPath = "encryptedPath",
            encryptedSize = "encryptedSize",
        )
        sdTransferDao.insertSdTransfer(sdTransfer)
        val actual = sdTransferDao.getAllSdTransfers().first().first()
        Truth.assertThat(actual.tag).isEqualTo(sdTransfer.tag)
        Truth.assertThat(actual.encryptedName).isEqualTo(sdTransfer.encryptedName)
        Truth.assertThat(actual.encryptedAppData).isEqualTo(sdTransfer.encryptedAppData)
        Truth.assertThat(actual.encryptedHandle).isEqualTo(sdTransfer.encryptedHandle)
        Truth.assertThat(actual.encryptedPath).isEqualTo(sdTransfer.encryptedPath)
        Truth.assertThat(actual.encryptedSize).isEqualTo(sdTransfer.encryptedSize)
    }

    @Test
    fun test_that_delete_SDTransferEntity_correctly() = runTest {
        val sdTransfer = SdTransferEntity(
            tag = 1,
            encryptedName = "encryptedName",
            encryptedAppData = "encryptedAppData",
            encryptedHandle = "encryptedHandle",
            encryptedPath = "encryptedPath",
            encryptedSize = "encryptedSize",
        )
        sdTransferDao.insertSdTransfer(sdTransfer)
        val totalRecordBeforeDelete = sdTransferDao.getAllSdTransfers().first().size
        Truth.assertThat(totalRecordBeforeDelete).isEqualTo(1)
        sdTransferDao.deleteSdTransfer(sdTransfer.tag ?: 0)
        val totalRecordAfterDelete = sdTransferDao.getAllSdTransfers().first().size
        Truth.assertThat(totalRecordAfterDelete).isEqualTo(0)
    }
}