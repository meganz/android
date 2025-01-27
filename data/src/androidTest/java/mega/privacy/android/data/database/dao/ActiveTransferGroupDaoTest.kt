package mega.privacy.android.data.database.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.database.MegaDatabase
import mega.privacy.android.data.database.entity.ActiveTransferGroupEntity
import mega.privacy.android.domain.entity.transfer.TransferType
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ActiveTransferGroupDaoTest {

    private lateinit var underTest: ActiveTransferGroupDao
    private lateinit var db: MegaDatabase

    @Before
    fun createDb() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context, MegaDatabase::class.java
        ).build()
        underTest = db.activeTransferGroupsDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun test_that_insert_a_new_entity_actually_inserts_the_entity() = runTest {
        val newEntity = ActiveTransferGroupEntity(
            groupId = 6434,
            transferType = TransferType.DOWNLOAD,
            destination = "destination"
        )
        underTest.insertActiveTransferGroup(newEntity)
        val actual = underTest.getActiveTransferGroupById(newEntity.groupId ?: -1)
        assertThat(actual).isEqualTo(newEntity)
    }

    @Test
    fun test_that_delete_an_entity_actually_deletes_the_entity() = runTest {
        val newEntity = ActiveTransferGroupEntity(
            groupId = 6434,
            transferType = TransferType.DOWNLOAD,
            destination = "destination"
        )
        underTest.insertActiveTransferGroup(newEntity)
        underTest.deleteActiveTransfersGroupById(newEntity.groupId ?: -1)
        val actual = underTest.getActiveTransferGroupById(newEntity.groupId ?: -1)
        assertThat(actual).isNull()
    }
}