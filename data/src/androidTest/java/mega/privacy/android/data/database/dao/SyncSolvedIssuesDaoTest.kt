package mega.privacy.android.data.database.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.database.MegaDatabase
import mega.privacy.android.data.database.entity.SyncSolvedIssueEntity
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SyncSolvedIssuesDaoTest {
    private lateinit var syncSolvedIssuesDao: SyncSolvedIssuesDao
    private lateinit var db: MegaDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context, MegaDatabase::class.java
        ).build()
        syncSolvedIssuesDao = db.syncSolvedIssuesDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun test_that_insertSolvedIssue_inserts_the_entity() = runTest {
        val entity = generateEntity("node1", "/local/path/1", "Issue resolved")
        insertEntity(entity)

        val result = syncSolvedIssuesDao.monitorSolvedIssues().first()
        Truth.assertThat(result).isNotEmpty()
        Truth.assertThat(result[0].nodeIds).isEqualTo(entity.nodeIds)
        Truth.assertThat(result[0].localPaths).isEqualTo(entity.localPaths)
        Truth.assertThat(result[0].resolutionExplanation).isEqualTo(entity.resolutionExplanation)
    }

    @Test
    fun test_that_monitorSolvedIssues_emits_all_elements() = runTest {
        val entity1 = generateEntity("node1", "/local/path/1", "Issue resolved")
        val entity2 = generateEntity("node2", "/local/path/2", "Issue resolved again")

        insertEntity(entity1)
        insertEntity(entity2)

        Truth.assertThat(syncSolvedIssuesDao.monitorSolvedIssues().first())
            .isEqualTo(listOf(entity1.copy(entityId = 1), entity2.copy(entityId = 2)))
    }

    @Test
    fun test_that_deleteAllSolvedIssues_deletes_all_entities() = runTest {
        val entity1 = generateEntity("node1", "/local/path/1", "Issue resolved")
        val entity2 = generateEntity("node2", "/local/path/2", "Issue resolved again")

        insertEntity(entity1)
        insertEntity(entity2)

        syncSolvedIssuesDao.deleteAllSolvedIssues()

        val result = syncSolvedIssuesDao.monitorSolvedIssues().first()
        Truth.assertThat(result).isEmpty()
    }

    @Test
    fun test_that_removeBySyncId_removes_entity_by_syncId() = runTest {
        val entity1 = generateEntity("node1", "/local/path/1", "Issue resolved", syncId = 1L)
        val entity2 = generateEntity("node2", "/local/path/2", "Issue resolved again", syncId = 2L)

        insertEntity(entity1)
        insertEntity(entity2)
        syncSolvedIssuesDao.removeBySyncId(1L)

        val result = syncSolvedIssuesDao.monitorSolvedIssues().first()
        Truth.assertThat(result).isEqualTo(listOf(entity2.copy(entityId = 2)))
    }

    private fun generateEntity(
        nodeIds: String,
        localPaths: String,
        resolutionExplanation: String,
        syncId: Long = 1L,
    ) = SyncSolvedIssueEntity(
        syncId = syncId,
        nodeIds = nodeIds,
        localPaths = localPaths,
        resolutionExplanation = resolutionExplanation
    )

    private suspend fun insertEntity(entity: SyncSolvedIssueEntity) {
        syncSolvedIssuesDao.insertSolvedIssue(entity)
    }
}
