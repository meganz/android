package mega.privacy.android.data.database.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.google.common.truth.Correspondence
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.database.MegaDatabase
import mega.privacy.android.data.database.entity.PendingTransferEntity
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.transfer.TransferStage
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.entity.transfer.pending.PendingTransferNodeIdentifier
import mega.privacy.android.domain.entity.transfer.pending.PendingTransferState
import mega.privacy.android.domain.entity.transfer.pending.UpdateAlreadyTransferredFilesCount
import mega.privacy.android.domain.entity.transfer.pending.UpdatePendingTransferState
import mega.privacy.android.domain.entity.transfer.pending.UpdateScanningFoldersData
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PendingTransferDaoTest {
    private lateinit var underTest: PendingTransferDao
    private lateinit var db: MegaDatabase

    @Before
    fun createDb() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context, MegaDatabase::class.java
        ).build()
        underTest = db.pendingTransferDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun test_that_insertOrUpdatePendingTransfers_actually_inserts_the_entity() = runTest {
        val newEntity = createEntity()
        underTest.insertOrUpdatePendingTransfers(listOf(newEntity), 10)

        val actual = underTest.getPendingTransferByTag(newEntity.transferTag ?: -1)
        assertThat(actual?.copy(pendingTransferId = null)).isEqualTo(newEntity)
    }

    @Test
    fun test_that_insertOrUpdatePendingTransfers_actually_inserts_the_entities() = runTest {
        val newEntities = (1..50).map { createEntity(it) }
        underTest.insertOrUpdatePendingTransfers(newEntities, 10)

        val actual = underTest.getPendingTransfersByType(TransferType.DOWNLOAD).first()
        assertThat(actual)
            .comparingElementsUsing(entityCorrespondence)
            .containsExactlyElementsIn(newEntities)
    }

    @Test
    fun test_that_getPendingTransfersByType_returns_the_correct_entities() = runTest {
        val transferType = TransferType.GENERAL_UPLOAD
        val newEntities = (1..50).map {
            createEntity(it, if (it < 5) TransferType.DOWNLOAD else TransferType.GENERAL_UPLOAD)
        }
        underTest.insertOrUpdatePendingTransfers(newEntities, 10)

        val actual = underTest.getPendingTransfersByType(transferType).first()
        assertThat(actual)
            .comparingElementsUsing(entityCorrespondence)
            .containsExactlyElementsIn(newEntities.filter { it.transferType == transferType })
    }

    @Test
    fun test_that_getPendingTransfersByTypeAndState_returns_the_correct_entities() = runTest {
        val state = PendingTransferState.SdkScanning
        val transferType = TransferType.GENERAL_UPLOAD
        val newEntities = (1..50).map {
            createEntity(
                it, if (it < 5) TransferType.DOWNLOAD else TransferType.GENERAL_UPLOAD,
                if (it > 10) PendingTransferState.SdkScanning else PendingTransferState.NotSentToSdk
            )
        }
        underTest.insertOrUpdatePendingTransfers(newEntities, 10)

        val actual =
            underTest.getPendingTransfersByTypeAndState(
                transferType,
                state
            ).first()
        assertThat(actual)
            .comparingElementsUsing(entityCorrespondence)
            .containsExactlyElementsIn(newEntities.filter {
                it.transferType == transferType
                        && it.state == state
            })
    }

    @Test
    fun test_that_update_state_actually_updates_the_entity() = runTest {
        val newEntity = createEntity()
        val id = underTest.insertOrUpdatePendingTransfers(listOf(newEntity)).first()

        val update = UpdatePendingTransferState(id, PendingTransferState.SdkScanning)
        underTest.update(update)

        val actual = underTest.getPendingTransferByTag(newEntity.transferTag ?: -1)
        assertThat(actual).isEqualTo(newEntity.copy(pendingTransferId = id, state = update.state))
    }

    @Test
    fun test_that_update_files_count_actually_updates_the_entity() = runTest {
        val newEntity = createEntity()
        val id = underTest.insertOrUpdatePendingTransfers(listOf(newEntity)).first()

        val update = UpdateAlreadyTransferredFilesCount(id, 10, 5)
        underTest.update(update)

        val actual = underTest.getPendingTransferByTag(newEntity.transferTag ?: -1)
        assertThat(actual).isEqualTo(
            newEntity.copy(
                pendingTransferId = id,
                startedFiles = update.startedFiles,
                alreadyTransferred = update.alreadyTransferred,
                state = update.state,
            )
        )
    }

    @Test
    fun test_that_update_scanning_data_actually_updates_the_entity() = runTest {
        val newEntity = createEntity()
        val id = underTest.insertOrUpdatePendingTransfers(listOf(newEntity)).first()

        val update = UpdateScanningFoldersData(id, TransferStage.STAGE_CREATING_TREE, 50, 2, 1)
        underTest.update(update)

        val actual = underTest.getPendingTransferByTag(newEntity.transferTag ?: -1)
        assertThat(actual).isEqualTo(
            newEntity.copy(
                pendingTransferId = id,
                scanningFoldersData = PendingTransferEntity.ScanningFoldersDataEntity(
                    stage = update.stage,
                    fileCount = update.fileCount,
                    folderCount = update.folderCount,
                    createdFolderCount = update.createdFolderCount,
                )
            )
        )
    }

    @Test
    fun test_that_delete_by_tag_actually_deletes_the_entity() = runTest {
        val newEntities = (1..50).map { createEntity(it) }
        underTest.insertOrUpdatePendingTransfers(newEntities, 10)

        underTest.getPendingTransfersByType(TransferType.DOWNLOAD).test {
            assertThat(awaitItem()).hasSize(newEntities.size)

            underTest.deletePendingTransferByTag(newEntities.first().transferTag ?: -1)

            val actual = awaitItem()
            assertThat(actual)
                .comparingElementsUsing(entityCorrespondence)
                .containsExactlyElementsIn(newEntities.drop(1))
        }
    }

    @Test
    fun test_that_delete_all_actually_deletes_the_entities() = runTest {
        val newEntities = (1..50).map { createEntity(it) }
        underTest.insertOrUpdatePendingTransfers(newEntities, 10)

        underTest.getPendingTransfersByType(TransferType.DOWNLOAD).test {
            assertThat(awaitItem()).isNotEmpty()

            underTest.deleteAllPendingTransfers()

            val actual = awaitItem()
            assertThat(actual).isEmpty()
        }
    }

    private val entityCorrespondence =
        Correspondence.from<PendingTransferEntity, PendingTransferEntity>({ p1, p2 ->
            p1?.copy(pendingTransferId = null) == p2?.copy(pendingTransferId = null)
        }, "equalsExceptId")

    private fun createEntity(
        transferTag: Int = 100,
        transferType: TransferType = TransferType.DOWNLOAD,
        state: PendingTransferState = PendingTransferState.NotSentToSdk,
    ) = PendingTransferEntity(
        transferTag = transferTag,
        transferType = transferType,
        path = "path",
        appData = "appData",
        isHighPriority = true,
        scanningFoldersData = PendingTransferEntity.ScanningFoldersDataEntity(
            stage = TransferStage.STAGE_SCANNING,
            fileCount = 105,
            folderCount = 50,
            createdFolderCount = 2
        ),
        alreadyTransferred = 0,
        state = state,
        nodeIdentifier = PendingTransferNodeIdentifier.CloudDriveNode(NodeId(45L))
    )

}