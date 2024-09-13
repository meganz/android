package mega.privacy.android.feature.sync.data.gateway

import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.database.dao.SyncSolvedIssuesDao
import mega.privacy.android.data.database.entity.SyncSolvedIssueEntity
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.kotlin.reset

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SyncSolvedIssuesGatewayImplTest {

    private val syncSolvedIssuesDao: SyncSolvedIssuesDao = mock()
    private val underTest = SyncSolvedIssuesGatewayImpl { syncSolvedIssuesDao }

    @AfterEach
    fun tearDown() {
        reset(syncSolvedIssuesDao)
    }

    @Test
    fun `test that clear invokes gateway clear method`() = runTest {
        underTest.clear()

        verify(syncSolvedIssuesDao).deleteAllSolvedIssues()
    }

    @Test
    fun `test that setting solved issue invokes gateway set method`() = runTest {
        val syncSolvedIssueEntity = SyncSolvedIssueEntity(
            syncId = 1L,
            entityId = null,
            nodeIds = "[{\"longValue\":3}]",
            localPaths = "[\"usr/Documents\"]",
            resolutionExplanation = "Folders merged",
        )
        underTest.set(syncSolvedIssueEntity)

        verify(syncSolvedIssuesDao).insertSolvedIssue(syncSolvedIssueEntity)
    }

    @Test
    fun `test that removing solved issue by sync id invokes gateway removeBySyncId method`() =
        runTest {
            underTest.removeBySyncId(1L)

            verify(syncSolvedIssuesDao).removeBySyncId(1L)
        }

    @Test
    fun `test that monitorSyncSolvedIssues invokes gateway monitorSolvedIssues method`() = runTest {
        underTest.monitorSyncSolvedIssues()

        verify(syncSolvedIssuesDao).monitorSolvedIssues()
    }
}