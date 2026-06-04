package mega.privacy.android.domain.usecase.continuewhereleftoff

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.continuewhereleftoff.ContinueWhereLeftOffItem
import mega.privacy.android.domain.entity.continuewhereleftoff.ContinueWhereLeftOffSortField
import mega.privacy.android.domain.entity.continuewhereleftoff.RecentlyUsedType
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.SortDirection
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.repository.ContinueWhereLeftOffRepository
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.node.hiddennode.MonitorHiddenNodesEnabledUseCase
import mega.privacy.android.domain.usecase.setting.MonitorShowHiddenItemsUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MonitorContinueWhereLeftOffItemsUseCaseTest {

    private lateinit var underTest: MonitorContinueWhereLeftOffItemsUseCase

    private val repository = mock<ContinueWhereLeftOffRepository>()
    private val monitorHiddenNodesEnabledUseCase = mock<MonitorHiddenNodesEnabledUseCase>()
    private val monitorShowHiddenItemsUseCase = mock<MonitorShowHiddenItemsUseCase>()
    private val monitorNodeUpdatesUseCase = mock<MonitorNodeUpdatesUseCase>()
    private val getNodeByIdUseCase = mock<GetNodeByIdUseCase>()

    @BeforeAll
    fun setUp() {
        underTest = MonitorContinueWhereLeftOffItemsUseCase(
            repository = repository,
            monitorHiddenNodesEnabledUseCase = monitorHiddenNodesEnabledUseCase,
            monitorShowHiddenItemsUseCase = monitorShowHiddenItemsUseCase,
            monitorNodeUpdatesUseCase = monitorNodeUpdatesUseCase,
            getNodeByIdUseCase = getNodeByIdUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            repository,
            monitorHiddenNodesEnabledUseCase,
            monitorShowHiddenItemsUseCase,
            monitorNodeUpdatesUseCase,
            getNodeByIdUseCase,
        )
        // Defaults: hidden-nodes feature off, no node updates. Combined with the use case's
        // fail-open onStart values, this leaves items untouched unless a test opts in.
        whenever(monitorHiddenNodesEnabledUseCase()).thenReturn(emptyFlow())
        whenever(monitorShowHiddenItemsUseCase()).thenReturn(emptyFlow())
        whenever(monitorNodeUpdatesUseCase()).thenReturn(emptyFlow())
    }

    private fun item(nodeHandle: Long) = ContinueWhereLeftOffItem(
        nodeHandle = nodeHandle,
        type = RecentlyUsedType.PDF,
        title = "file-$nodeHandle.pdf",
        lastAccessedTimestamp = 1000L + nodeHandle,
    )

    private fun nodeMock(marked: Boolean, inherited: Boolean) = mock<TypedNode> {
        on { isMarkedSensitive } doReturn marked
        on { isSensitiveInherited } doReturn inherited
    }

    @Test
    fun `test that invoke returns items from repository`() = runTest {
        val items = listOf(item(1L))
        whenever(repository.monitorContinueWhereLeftOffItems(10, null, null))
            .thenReturn(flowOf(items))

        underTest(10).test {
            assertThat(awaitItem()).isEqualTo(items)
            cancelAndIgnoreRemainingEvents()
        }
        verify(repository).monitorContinueWhereLeftOffItems(10, null, null)
    }

    @Test
    fun `test that invoke returns empty list when no items`() = runTest {
        whenever(repository.monitorContinueWhereLeftOffItems(10, null, null))
            .thenReturn(flowOf(emptyList()))

        underTest(10).test {
            assertThat(awaitItem()).isEmpty()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that invoke forwards limit to repository`() = runTest {
        whenever(repository.monitorContinueWhereLeftOffItems(20, null, null))
            .thenReturn(flowOf(emptyList()))

        underTest(20).test {
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }
        verify(repository).monitorContinueWhereLeftOffItems(20, null, null)
    }

    @Test
    fun `test that invoke forwards explicit sort field and direction to repository`() = runTest {
        whenever(
            repository.monitorContinueWhereLeftOffItems(
                10,
                ContinueWhereLeftOffSortField.Timestamp,
                SortDirection.Descending,
            )
        ).thenReturn(flowOf(emptyList()))

        underTest(
            limit = 10,
            sortField = ContinueWhereLeftOffSortField.Timestamp,
            sortDirection = SortDirection.Descending,
        ).test {
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }
        verify(repository).monitorContinueWhereLeftOffItems(
            10,
            ContinueWhereLeftOffSortField.Timestamp,
            SortDirection.Descending,
        )
    }

    @Test
    fun `test that marked sensitive item is removed when hidden nodes enabled and not showing hidden items`() =
        runTest {
            val items = listOf(item(1L), item(2L))
            val sensitiveNode = nodeMock(marked = true, inherited = false)
            val plainNode = nodeMock(marked = false, inherited = false)
            whenever(repository.monitorContinueWhereLeftOffItems(10, null, null))
                .thenReturn(flowOf(items))
            whenever(monitorHiddenNodesEnabledUseCase()).thenReturn(flowOf(true))
            whenever(monitorShowHiddenItemsUseCase()).thenReturn(flowOf(false))
            whenever(getNodeByIdUseCase(NodeId(1L))).thenReturn(sensitiveNode)
            whenever(getNodeByIdUseCase(NodeId(2L))).thenReturn(plainNode)

            underTest(10).test {
                assertThat(awaitItem().map { it.nodeHandle }).doesNotContain(1L)
                cancelAndIgnoreRemainingEvents()
            }
            verify(repository).removeRecentlyUsedItem(1L)
        }

    @Test
    fun `test that inherited sensitive item is removed when hidden nodes enabled and not showing hidden items`() =
        runTest {
            val items = listOf(item(1L))
            val inheritedSensitiveNode = nodeMock(marked = false, inherited = true)
            whenever(repository.monitorContinueWhereLeftOffItems(10, null, null))
                .thenReturn(flowOf(items))
            whenever(monitorHiddenNodesEnabledUseCase()).thenReturn(flowOf(true))
            whenever(monitorShowHiddenItemsUseCase()).thenReturn(flowOf(false))
            whenever(getNodeByIdUseCase(NodeId(1L))).thenReturn(inheritedSensitiveNode)

            underTest(10).test {
                assertThat(awaitItem()).isEmpty()
                cancelAndIgnoreRemainingEvents()
            }
            verify(repository).removeRecentlyUsedItem(1L)
        }

    @Test
    fun `test that sensitive item is kept when hidden nodes feature is not enabled`() = runTest {
        val items = listOf(item(1L))
        val sensitiveNode = nodeMock(marked = true, inherited = false)
        whenever(repository.monitorContinueWhereLeftOffItems(10, null, null))
            .thenReturn(flowOf(items))
        whenever(monitorHiddenNodesEnabledUseCase()).thenReturn(flowOf(false))
        whenever(monitorShowHiddenItemsUseCase()).thenReturn(flowOf(false))
        whenever(getNodeByIdUseCase(NodeId(1L))).thenReturn(sensitiveNode)

        underTest(10).test {
            assertThat(awaitItem().map { it.nodeHandle }).containsExactly(1L)
            cancelAndIgnoreRemainingEvents()
        }
        verify(repository, never()).removeRecentlyUsedItem(any())
    }

    @Test
    fun `test that sensitive item is kept when showing hidden items`() = runTest {
        val items = listOf(item(1L))
        val sensitiveNode = nodeMock(marked = true, inherited = false)
        whenever(repository.monitorContinueWhereLeftOffItems(10, null, null))
            .thenReturn(flowOf(items))
        whenever(monitorHiddenNodesEnabledUseCase()).thenReturn(flowOf(true))
        whenever(monitorShowHiddenItemsUseCase()).thenReturn(flowOf(true))
        whenever(getNodeByIdUseCase(NodeId(1L))).thenReturn(sensitiveNode)

        underTest(10).test {
            assertThat(awaitItem().map { it.nodeHandle }).containsExactly(1L)
            cancelAndIgnoreRemainingEvents()
        }
        verify(repository, never()).removeRecentlyUsedItem(any())
    }

    @Test
    fun `test that non sensitive items are kept when hidden nodes enabled and not showing hidden items`() =
        runTest {
            val items = listOf(item(1L), item(2L))
            val plainNode = nodeMock(marked = false, inherited = false)
            whenever(repository.monitorContinueWhereLeftOffItems(10, null, null))
                .thenReturn(flowOf(items))
            whenever(monitorHiddenNodesEnabledUseCase()).thenReturn(flowOf(true))
            whenever(monitorShowHiddenItemsUseCase()).thenReturn(flowOf(false))
            whenever(getNodeByIdUseCase(any())).thenReturn(plainNode)

            underTest(10).test {
                assertThat(awaitItem().map { it.nodeHandle }).containsExactly(1L, 2L).inOrder()
                cancelAndIgnoreRemainingEvents()
            }
            verify(repository, never()).removeRecentlyUsedItem(any())
        }
}
