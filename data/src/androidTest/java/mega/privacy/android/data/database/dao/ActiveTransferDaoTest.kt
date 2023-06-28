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
import mega.privacy.android.data.database.entity.ActiveTransferEntity
import mega.privacy.android.domain.entity.transfer.TransferType
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class ActiveTransferDaoTest {
    private lateinit var activeTransferDao: ActiveTransferDao
    private lateinit var db: MegaDatabase

    private val entities = (0..10).map { tag ->
        ActiveTransferEntity(
            tag = tag,
            transferType = if (tag.rem(2) == 0) TransferType.TYPE_DOWNLOAD else TransferType.TYPE_UPLOAD,
            totalBytes = 1024,
            transferredBytes = 512,
            isFinished = true,
        )
    }

    @Before
    fun createDb() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context, MegaDatabase::class.java
        ).build()
        activeTransferDao = db.activeTransfersDao()
        entities.forEach {
            activeTransferDao.insertOrUpdateActiveTransfer(it)
        }
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun test_that_getActiveTransferByTag_returns_the_correct_active_transfer() = runTest {
        entities.forEach { entity ->
            val actual = activeTransferDao.getActiveTransferByTag(entity.tag)
            assertThat(actual).isEqualTo(entity)
        }
    }

    @Test
    fun test_that_getActiveTransfersByType_returns_all_transfers_of_that_type() = runTest {
        TransferType.values().forEach { type ->
            val expected = entities.filter { it.transferType == type }
            val actual = activeTransferDao.getActiveTransfersByType(type).first()
            assertThat(actual).containsExactlyElementsIn(expected)
        }
    }
}
