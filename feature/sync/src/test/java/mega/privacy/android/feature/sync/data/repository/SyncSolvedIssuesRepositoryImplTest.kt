package mega.privacy.android.feature.sync.data.repository

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.database.entity.SyncSolvedIssueEntity
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.feature.sync.data.gateway.SyncSolvedIssuesGateway
import mega.privacy.android.feature.sync.data.mapper.solvedissue.SolvedIssueEntityToSolvedIssueMapper
import mega.privacy.android.feature.sync.data.mapper.solvedissue.SolvedIssueToSolvedIssueEntityMapper
import mega.privacy.android.feature.sync.domain.entity.SolvedIssue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class SyncSolvedIssuesRepositoryImplTest {
    private val syncSolvedIssuesGateway: SyncSolvedIssuesGateway = mock()
    private val solvedIssueToSolvedIssueEntityMapper: SolvedIssueToSolvedIssueEntityMapper = mock()
    private val solvedIssueEntityToSolvedIssueMapper: SolvedIssueEntityToSolvedIssueMapper = mock()

    private val underTest = SyncSolvedIssuesRepositoryImpl(
        syncSolvedIssuesGateway = syncSolvedIssuesGateway,
        solvedIssueToSolvedIssueEntityMapper = solvedIssueToSolvedIssueEntityMapper,
        solvedIssueEntityToSolvedIssueMapper = solvedIssueEntityToSolvedIssueMapper
    )

    private val solvedIssue = SolvedIssue(
        nodeIds = listOf(NodeId(3)),
        localPaths = listOf("/usr/Documents"),
        resolutionExplanation = "Folders merged",
    )

    private val syncSolvedIssueEntity = SyncSolvedIssueEntity(
        entityId = null,
        nodeIds = "[{\"longValue\":3}]",
        localPaths = "[\"usr/Documents\"]",
        resolutionExplanation = "Folders merged",
    )

    @Test
    fun `test that clear invokes gateway clear method`() = runTest {
        whenever(solvedIssueToSolvedIssueEntityMapper(solvedIssue)).thenReturn(syncSolvedIssueEntity)
        underTest.clear()
        verify(syncSolvedIssuesGateway).clear()
    }

    @Test
    fun `test that setting solved issue invokes gateway set method`() = runTest {
        whenever(solvedIssueToSolvedIssueEntityMapper(solvedIssue)).thenReturn(syncSolvedIssueEntity)
        underTest.insertSolvedIssues(solvedIssue)
        verify(syncSolvedIssuesGateway).set(syncSolvedIssueEntity)
    }
}