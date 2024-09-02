package mega.privacy.android.data.database.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.database.MegaDatabase
import mega.privacy.android.data.database.entity.ActiveTransferEntity
import mega.privacy.android.domain.entity.transfer.TransferType
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ActiveTransferDaoTest {
    private lateinit var activeTransferDao: ActiveTransferDao
    private lateinit var db: MegaDatabase

    private val entities = TransferType.entries.flatMap { transferType: TransferType ->
        (1..4).map { index ->
            val tag = index + (10 * transferType.ordinal)
            ActiveTransferEntity(
                tag = tag,
                transferType = transferType,
                totalBytes = 1024 * (tag.toLong() % 5 + 1),
                isFinished = index.rem(3) == 0,
                isPaused = false,
                isFolderTransfer = false,
                isAlreadyTransferred = false,
            )
        }
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

    fun test_that_insert_a_new_entity_actually_inserts_the_entity() = runTest {
        val newEntity = ActiveTransferEntity(
            tag = 100,
            transferType = TransferType.GENERAL_UPLOAD,
            totalBytes = 1024,
            isFinished = true,
            isFolderTransfer = false,
            isPaused = false,
            isAlreadyTransferred = false,
        )
        activeTransferDao.insertOrUpdateActiveTransfer(newEntity)
        val actual = activeTransferDao.getActiveTransferByTag(newEntity.tag)
        assertThat(actual).isEqualTo(newEntity)
    }

    fun test_that_insert_a_duplicated_transfer_replaces_original_one() = runTest {
        val firstEntity = entities.first()
        val modified = firstEntity.copy(isFinished = !firstEntity.isFinished)
        activeTransferDao.insertOrUpdateActiveTransfer(modified)
        val result = activeTransferDao.getCurrentActiveTransfersByType(firstEntity.transferType)
        assertThat(result).contains(modified)
        assertThat(result).doesNotContain(firstEntity)
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
        TransferType.entries.forEach { type ->
            val expected = entities.filter { it.transferType == type }
            val actual = activeTransferDao.getActiveTransfersByType(type).first()
            assertThat(actual).containsExactlyElementsIn(expected)
        }
    }

    @Test
    fun test_that_getCurrentActiveTransfersByType_returns_all_transfers_of_that_type() = runTest {
        TransferType.entries.forEach { type ->
            val expected = entities.filter { it.transferType == type }
            val actual = activeTransferDao.getCurrentActiveTransfersByType(type)
            assertThat(actual).containsExactlyElementsIn(expected)
        }
    }

    @Test
    fun test_that_getCurrentActiveTransfers_returns_all_transfers() = runTest {
        val expected = entities
        val actual = activeTransferDao.getCurrentActiveTransfers()
        assertThat(actual).containsExactlyElementsIn(expected)
    }

    @Test
    fun test_that_deleteAllActiveTransfersByType_deletes_all_transfers_of_that_type() = runTest {
        TransferType.entries.forEach { type ->
            val initial = activeTransferDao.getCurrentActiveTransfersByType(type)
            assertThat(initial).isNotEmpty()
            activeTransferDao.deleteAllActiveTransfersByType(type)
            val actual = activeTransferDao.getCurrentActiveTransfersByType(type)
            assertThat(actual).isEmpty()
        }
    }

    @Test
    fun test_that_deleteAllActiveTransfers_deletes_all_transfers() = runTest {
        val initial = activeTransferDao.getCurrentActiveTransfers()
        assertThat(initial).isNotEmpty()
        activeTransferDao.deleteAllActiveTransfers()
        val actual = activeTransferDao.getCurrentActiveTransfers()
        assertThat(actual).isEmpty()
    }

    @Test
    fun test_that_setActiveTransferAsFinishedByTag_set_as_finished_all_transfers_with_given_tags() =
        runTest {
            TransferType.entries.forEach { type ->
                val initial = activeTransferDao.getCurrentActiveTransfersByType(type)
                val toFinish = initial.take(initial.size / 2).filter { !it.isFinished }
                assertThat(initial).isNotEmpty()
                assertThat(toFinish).isNotEmpty()
                activeTransferDao.setActiveTransferAsFinishedByTag(toFinish.map { it.tag })
                val actual = activeTransferDao.getCurrentActiveTransfersByType(type)
                toFinish.forEach { finished ->
                    assertThat(actual.first { it.tag == finished.tag }.isFinished).isTrue()
                }
            }
        }
}
