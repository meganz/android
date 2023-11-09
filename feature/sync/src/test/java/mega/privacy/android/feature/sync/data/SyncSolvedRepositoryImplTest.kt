package mega.privacy.android.feature.sync.data

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.database.entity.SyncSolvedIssueEntity
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.feature.sync.data.gateway.SyncSolvedIssuesGateway
import mega.privacy.android.feature.sync.data.mapper.SyncSolvedIssueMapper
import mega.privacy.android.feature.sync.data.repository.SyncSolvedIssuesRepositoryImpl
import mega.privacy.android.feature.sync.domain.entity.SolvedIssue
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SyncSolvedRepositoryImplTest {

    private val syncSolvedIssuesGateway: SyncSolvedIssuesGateway = mock()
    private val syncSolvedIssuesMapper: SyncSolvedIssueMapper = mock()

    private val underTest = SyncSolvedIssuesRepositoryImpl(
        syncSolvedIssuesGateway, syncSolvedIssuesMapper
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

    @AfterEach
    fun resetAndTearDown() {
        Mockito.reset(
            syncSolvedIssuesGateway,
            syncSolvedIssuesMapper,
        )
    }

    @Test
    fun `test that get solved issues returns correct list`() = runTest {
        whenever(syncSolvedIssuesMapper(syncSolvedIssueEntity)).thenReturn(solvedIssue)
        whenever(syncSolvedIssuesGateway.getAll()).thenReturn(listOf(syncSolvedIssueEntity))

        val expected = listOf(solvedIssue)
        val actual = underTest.getAll()

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test that setting solved issue invokes gateway set method`() = runTest {
        whenever(syncSolvedIssuesMapper(solvedIssue)).thenReturn(syncSolvedIssueEntity)

        underTest.set(solvedIssue)

        verify(syncSolvedIssuesGateway).set(syncSolvedIssueEntity)
    }

    @Test
    fun `test that clear invokes gateway clear method`() = runTest {
        whenever(syncSolvedIssuesMapper(solvedIssue)).thenReturn(syncSolvedIssueEntity)

        underTest.clear()

        verify(syncSolvedIssuesGateway).clear()
    }

    @Test
    fun `test that every time solved issue is set monitorSolvedIssuesCountChanged gets triggered`() =
        runTest {
            whenever(syncSolvedIssuesMapper(solvedIssue)).thenReturn(syncSolvedIssueEntity)

            underTest.set(solvedIssue)

            underTest
                .monitorSolvedIssuesCountChanged()
                .test {
                    awaitItem()
                    cancelAndIgnoreRemainingEvents()
                }
        }
}