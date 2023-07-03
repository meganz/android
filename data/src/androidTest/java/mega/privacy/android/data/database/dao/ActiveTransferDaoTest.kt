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

    private val entities = (0..20).map { tag ->
        ActiveTransferEntity(
            tag = tag,
            transferType = TransferType.values()[tag.rem(TransferType.values().size)],
            totalBytes = 1024 * (tag.toLong() % 5 + 1),
            transferredBytes = 512 * (tag.toLong() % 5 + 1),
            isFinished = tag.rem(5) == 0
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

    @Test
    fun test_that_getTotals_returns_correct_totalBytes() = runTest {
        TransferType.values().forEach { type ->
            val expectedTotal = entities.filter { it.transferType == type }.sumOf { it.totalBytes }
            val actual = activeTransferDao.getTotalsByType(type).first()
            assertThat(actual.totalBytes).isEqualTo(expectedTotal)
        }
    }

    @Test
    fun test_that_getTotals_returns_correct_transferredBytes() = runTest {
        TransferType.values().forEach { type ->
            val expectedTransferredBytes =
                entities.filter { it.transferType == type }.sumOf { it.transferredBytes }
            val actual = activeTransferDao.getTotalsByType(type).first()
            assertThat(actual.transferredBytes).isEqualTo(expectedTransferredBytes)
        }
    }

    @Test
    fun test_that_getTotals_returns_correct_totalTransfers() = runTest {
        TransferType.values().forEach { type ->
            val expected = entities.count { it.transferType == type }
            val actual = activeTransferDao.getTotalsByType(type).first()
            assertThat(actual.totalTransfers).isEqualTo(expected)
        }
    }

    @Test
    fun test_that_getTotals_returns_correct_totalFinishedTransfers() = runTest {
        TransferType.values().forEach { type ->
            val expected = entities.count { it.transferType == type && it.isFinished }
            val actual = activeTransferDao.getTotalsByType(type).first()
            assertThat(actual.totalFinishedTransfers).isEqualTo(expected)
        }
    }
}
