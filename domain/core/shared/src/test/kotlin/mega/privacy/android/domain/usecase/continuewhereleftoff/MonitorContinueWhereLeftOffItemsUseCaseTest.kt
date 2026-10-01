package mega.privacy.android.domain.usecase.continuewhereleftoff

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.continuewhereleftoff.ContinueWhereLeftOffItem
import mega.privacy.android.domain.entity.continuewhereleftoff.ContinueWhereLeftOffSortField
import mega.privacy.android.domain.entity.continuewhereleftoff.RecentlyUsedType
import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.entity.node.NodeChanges
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeUpdate
import mega.privacy.android.domain.entity.node.SortDirection
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.repository.ContinueWhereLeftOffRepository
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.node.IsNodeInRubbishOrDeletedUseCase
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
    private val isNodeInRubbishOrDeletedUseCase = mock<IsNodeInRubbishOrDeletedUseCase>()

    @BeforeAll
    fun setUp() {
        underTest = MonitorContinueWhereLeftOffItemsUseCase(
            repository = repository,
            monitorHiddenNodesEnabledUseCase = monitorHiddenNodesEnabledUseCase,
            monitorShowHiddenItemsUseCase = monitorShowHiddenItemsUseCase,
            monitorNodeUpdatesUseCase = monitorNodeUpdatesUseCase,
            getNodeByIdUseCase = getNodeByIdUseCase,
            isNodeInRubbishOrDeletedUseCase = isNodeInRubbishOrDeletedUseCase,
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
            isNodeInRubbishOrDeletedUseCase,
        )
        // Defaults: eligibility unresolved (emptyFlow -> the use case's null placeholder), no node
        // updates, nothing trashed. This leaves items untouched unless a test opts in.
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
            assertThat(awaitItem().items).isEqualTo(items)
            cancelAndIgnoreRemainingEvents()
        }
        verify(repository).monitorContinueWhereLeftOffItems(10, null, null)
    }

    @Test
    fun `test that isHiddenResolved is false until eligibility resolves and true afterwards`() =
        runTest {
            whenever(repository.monitorContinueWhereLeftOffItems(10, null, null))
                .thenReturn(flowOf(listOf(item(1L))))
            // Unresolved eligibility (emptyFlow -> null placeholder) must be reported as not resolved
            // so consumers keep showing their loading state instead of the unblurred placeholder.
            whenever(monitorHiddenNodesEnabledUseCase()).thenReturn(emptyFlow())

            underTest(10).test {
                assertThat(awaitItem().isHiddenResolved).isFalse()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that isHiddenResolved is true once the real eligibility value arrives`() = runTest {
        whenever(repository.monitorContinueWhereLeftOffItems(10, null, null))
            .thenReturn(flowOf(listOf(item(1L))))
        whenever(monitorHiddenNodesEnabledUseCase()).thenReturn(flowOf(false))

        underTest(10).test {
            assertThat(awaitItem().isHiddenResolved).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that invoke returns empty list when no items`() = runTest {
        whenever(repository.monitorContinueWhereLeftOffItems(10, null, null))
            .thenReturn(flowOf(emptyList()))

        underTest(10).test {
            assertThat(awaitItem().items).isEmpty()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that invoke forwards limit to repository`() = runTest {
        whenever(repository.monitorContinueWhereLeftOffItems(20, null, null))
            .thenReturn(flowOf(emptyList()))

        underTest(20).test {
            awaitItem().items
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
            awaitItem().items
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
                assertThat(awaitItem().items.map { it.nodeHandle }).doesNotContain(1L)
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
                assertThat(awaitItem().items).isEmpty()
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
            assertThat(awaitItem().items.map { it.nodeHandle }).containsExactly(1L)
            cancelAndIgnoreRemainingEvents()
        }
        verify(repository, never()).removeRecentlyUsedItem(any())
    }

    @Test
    fun `test that sensitive item is kept and flagged sensitive when showing hidden items`() =
        runTest {
            val items = listOf(item(1L))
            val sensitiveNode = nodeMock(marked = true, inherited = false)
            whenever(repository.monitorContinueWhereLeftOffItems(10, null, null))
                .thenReturn(flowOf(items))
            whenever(monitorHiddenNodesEnabledUseCase()).thenReturn(flowOf(true))
            whenever(monitorShowHiddenItemsUseCase()).thenReturn(flowOf(true))
            whenever(getNodeByIdUseCase(NodeId(1L))).thenReturn(sensitiveNode)

            underTest(10).test {
                val result = awaitItem().items
                assertThat(result.map { it.nodeHandle }).containsExactly(1L)
                assertThat(result.single().isSensitive).isTrue()
                cancelAndIgnoreRemainingEvents()
            }
            verify(repository, never()).removeRecentlyUsedItem(any())
        }

    @Test
    fun `test that inherited sensitive item is flagged sensitive when showing hidden items`() =
        runTest {
            val items = listOf(item(1L))
            val inheritedSensitiveNode = nodeMock(marked = false, inherited = true)
            whenever(repository.monitorContinueWhereLeftOffItems(10, null, null))
                .thenReturn(flowOf(items))
            whenever(monitorHiddenNodesEnabledUseCase()).thenReturn(flowOf(true))
            whenever(monitorShowHiddenItemsUseCase()).thenReturn(flowOf(true))
            whenever(getNodeByIdUseCase(NodeId(1L))).thenReturn(inheritedSensitiveNode)

            underTest(10).test {
                assertThat(awaitItem().items.single().isSensitive).isTrue()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that non sensitive item is not flagged sensitive when showing hidden items`() =
        runTest {
            val items = listOf(item(1L))
            val plainNode = nodeMock(marked = false, inherited = false)
            whenever(repository.monitorContinueWhereLeftOffItems(10, null, null))
                .thenReturn(flowOf(items))
            whenever(monitorHiddenNodesEnabledUseCase()).thenReturn(flowOf(true))
            whenever(monitorShowHiddenItemsUseCase()).thenReturn(flowOf(true))
            whenever(getNodeByIdUseCase(NodeId(1L))).thenReturn(plainNode)

            underTest(10).test {
                assertThat(awaitItem().items.single().isSensitive).isFalse()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that sensitive item is not flagged sensitive when hidden nodes feature is not enabled`() =
        runTest {
            val items = listOf(item(1L))
            val sensitiveNode = nodeMock(marked = true, inherited = false)
            whenever(repository.monitorContinueWhereLeftOffItems(10, null, null))
                .thenReturn(flowOf(items))
            whenever(monitorHiddenNodesEnabledUseCase()).thenReturn(flowOf(false))
            whenever(monitorShowHiddenItemsUseCase()).thenReturn(flowOf(true))

            underTest(10).test {
                assertThat(awaitItem().items.single().isSensitive).isFalse()
                cancelAndIgnoreRemainingEvents()
            }
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
                assertThat(awaitItem().items.map { it.nodeHandle }).containsExactly(1L, 2L).inOrder()
                cancelAndIgnoreRemainingEvents()
            }
            verify(repository, never()).removeRecentlyUsedItem(any())
        }

    @Test
    fun `test that item moved to rubbish bin is removed regardless of hidden settings`() = runTest {
        val items = listOf(item(1L), item(2L))
        whenever(repository.monitorContinueWhereLeftOffItems(10, null, null))
            .thenReturn(flowOf(items))
        whenever(isNodeInRubbishOrDeletedUseCase(1L)).thenReturn(true)
        whenever(isNodeInRubbishOrDeletedUseCase(2L)).thenReturn(false)

        underTest(10).test {
            assertThat(awaitItem().items.map { it.nodeHandle }).containsExactly(2L)
            cancelAndIgnoreRemainingEvents()
        }
        verify(repository).removeRecentlyUsedItem(1L)
    }

    @Test
    fun `test that item moved to rubbish bin is removed even when showing hidden items`() = runTest {
        val items = listOf(item(1L))
        whenever(repository.monitorContinueWhereLeftOffItems(10, null, null))
            .thenReturn(flowOf(items))
        whenever(monitorHiddenNodesEnabledUseCase()).thenReturn(flowOf(true))
        whenever(monitorShowHiddenItemsUseCase()).thenReturn(flowOf(true))
        whenever(isNodeInRubbishOrDeletedUseCase(1L)).thenReturn(true)

        underTest(10).test {
            assertThat(awaitItem().items).isEmpty()
            cancelAndIgnoreRemainingEvents()
        }
        verify(repository).removeRecentlyUsedItem(1L)
    }

    @Test
    fun `test that items not in rubbish bin are kept`() = runTest {
        val items = listOf(item(1L), item(2L))
        whenever(repository.monitorContinueWhereLeftOffItems(10, null, null))
            .thenReturn(flowOf(items))
        whenever(isNodeInRubbishOrDeletedUseCase(any())).thenReturn(false)

        underTest(10).test {
            assertThat(awaitItem().items.map { it.nodeHandle }).containsExactly(1L, 2L).inOrder()
            cancelAndIgnoreRemainingEvents()
        }
        verify(repository, never()).removeRecentlyUsedItem(any())
    }

    @Test
    fun `test that trashed and hidden items are both removed when hidden nodes enabled and not showing hidden items`() =
        runTest {
            val items = listOf(item(1L), item(2L), item(3L))
            val sensitiveNode = nodeMock(marked = true, inherited = false)
            val plainNode = nodeMock(marked = false, inherited = false)
            whenever(repository.monitorContinueWhereLeftOffItems(10, null, null))
                .thenReturn(flowOf(items))
            whenever(monitorHiddenNodesEnabledUseCase()).thenReturn(flowOf(true))
            whenever(monitorShowHiddenItemsUseCase()).thenReturn(flowOf(false))
            whenever(isNodeInRubbishOrDeletedUseCase(1L)).thenReturn(true)
            whenever(isNodeInRubbishOrDeletedUseCase(2L)).thenReturn(false)
            whenever(isNodeInRubbishOrDeletedUseCase(3L)).thenReturn(false)
            whenever(getNodeByIdUseCase(NodeId(2L))).thenReturn(sensitiveNode)
            whenever(getNodeByIdUseCase(NodeId(3L))).thenReturn(plainNode)

            underTest(10).test {
                assertThat(awaitItem().items.map { it.nodeHandle }).containsExactly(3L)
                cancelAndIgnoreRemainingEvents()
            }
            verify(repository).removeRecentlyUsedItem(1L)
            verify(repository).removeRecentlyUsedItem(2L)
        }

    @Test
    fun `test that item title is refreshed from the current node name`() = runTest {
        val items = listOf(item(1L))
        whenever(repository.monitorContinueWhereLeftOffItems(10, null, null))
            .thenReturn(flowOf(items))
        val renamedNode = mock<TypedNode> { on { name } doReturn "renamed.pdf" }
        whenever(getNodeByIdUseCase(NodeId(1L))).thenReturn(renamedNode)

        underTest(10).test {
            assertThat(awaitItem().items.single().title).isEqualTo("renamed.pdf")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that stored title is kept when node lookup returns null`() = runTest {
        val items = listOf(item(1L))
        whenever(repository.monitorContinueWhereLeftOffItems(10, null, null))
            .thenReturn(flowOf(items))
        whenever(getNodeByIdUseCase(NodeId(1L))).thenReturn(null)

        underTest(10).test {
            assertThat(awaitItem().items.single().title).isEqualTo("file-1.pdf")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that stored title is kept when current node name is blank`() = runTest {
        val items = listOf(item(1L))
        whenever(repository.monitorContinueWhereLeftOffItems(10, null, null))
            .thenReturn(flowOf(items))
        val blankNameNode = mock<TypedNode> { on { name } doReturn "" }
        whenever(getNodeByIdUseCase(NodeId(1L))).thenReturn(blankNameNode)

        underTest(10).test {
            assertThat(awaitItem().items.single().title).isEqualTo("file-1.pdf")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that a renamed node is not refreshed once it has been removed`() = runTest {
        val items = listOf(item(1L))
        whenever(repository.monitorContinueWhereLeftOffItems(10, null, null))
            .thenReturn(flowOf(items))
        whenever(isNodeInRubbishOrDeletedUseCase(1L)).thenReturn(true)

        underTest(10).test {
            assertThat(awaitItem().items).isEmpty()
            cancelAndIgnoreRemainingEvents()
        }
        verify(getNodeByIdUseCase, never()).invoke(NodeId(1L))
    }

    @Test
    fun `test that item is flagged taken down from the current node`() = runTest {
        val items = listOf(item(1L))
        whenever(repository.monitorContinueWhereLeftOffItems(10, null, null))
            .thenReturn(flowOf(items))
        val takenDownNode = mock<TypedNode> { on { isTakenDown } doReturn true }
        whenever(getNodeByIdUseCase(NodeId(1L))).thenReturn(takenDownNode)

        underTest(10).test {
            assertThat(awaitItem().items.single().isTakenDown).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that item is not flagged taken down when node is not taken down`() = runTest {
        val items = listOf(item(1L))
        whenever(repository.monitorContinueWhereLeftOffItems(10, null, null))
            .thenReturn(flowOf(items))
        val plainNode = mock<TypedNode> { on { isTakenDown } doReturn false }
        whenever(getNodeByIdUseCase(NodeId(1L))).thenReturn(plainNode)

        underTest(10).test {
            assertThat(awaitItem().items.single().isTakenDown).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that taken down flag is refreshed when an update touches a carousel item`() = runTest {
        whenever(repository.monitorContinueWhereLeftOffItems(10, null, null))
            .thenReturn(flowOf(listOf(item(1L))))
        // A takedown (or restore) has no dedicated SDK change flag; it arrives as a public-link
        // change on the item's own node. An update touching a carousel item must trigger a re-read
        // of the live node so the flag flips without the recently-used table itself changing.
        val nodeUpdates = MutableSharedFlow<NodeUpdate>()
        whenever(monitorNodeUpdatesUseCase()).thenReturn(nodeUpdates)
        val node = mock<TypedNode>()
        whenever(node.isTakenDown).thenReturn(false, true)
        whenever(getNodeByIdUseCase(NodeId(1L))).thenReturn(node)
        val changedNode = mock<Node> { on { id } doReturn NodeId(1L) }

        underTest(10).test {
            assertThat(awaitItem().items.single().isTakenDown).isFalse()
            nodeUpdates.emit(NodeUpdate(mapOf(changedNode to listOf(NodeChanges.Public_link))))
            assertThat(awaitItem().items.single().isTakenDown).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that a node update for an unrelated node does not refresh items`() = runTest {
        whenever(repository.monitorContinueWhereLeftOffItems(10, null, null))
            .thenReturn(flowOf(listOf(item(1L))))
        // The node-update stream is global; an update for a node that is not in the carousel must
        // NOT trigger a re-read. If it did, the sequential stub below would flip the flag to true.
        val nodeUpdates = MutableSharedFlow<NodeUpdate>()
        whenever(monitorNodeUpdatesUseCase()).thenReturn(nodeUpdates)
        val node = mock<TypedNode>()
        whenever(node.isTakenDown).thenReturn(false, true)
        whenever(getNodeByIdUseCase(NodeId(1L))).thenReturn(node)
        val unrelatedNode = mock<Node> { on { id } doReturn NodeId(999L) }

        underTest(10).test {
            assertThat(awaitItem().items.single().isTakenDown).isFalse()
            nodeUpdates.emit(NodeUpdate(mapOf(unrelatedNode to listOf(NodeChanges.Public_link))))
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that stored taken down flag is kept when node lookup returns null`() = runTest {
        val items = listOf(item(1L))
        whenever(repository.monitorContinueWhereLeftOffItems(10, null, null))
            .thenReturn(flowOf(items))
        whenever(getNodeByIdUseCase(NodeId(1L))).thenReturn(null)

        underTest(10).test {
            assertThat(awaitItem().items.single().isTakenDown).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that trashed node is not looked up for sensitivity`() = runTest {
        val items = listOf(item(1L))
        whenever(repository.monitorContinueWhereLeftOffItems(10, null, null))
            .thenReturn(flowOf(items))
        whenever(monitorHiddenNodesEnabledUseCase()).thenReturn(flowOf(true))
        whenever(monitorShowHiddenItemsUseCase()).thenReturn(flowOf(false))
        whenever(isNodeInRubbishOrDeletedUseCase(1L)).thenReturn(true)

        underTest(10).test {
            assertThat(awaitItem().items).isEmpty()
            cancelAndIgnoreRemainingEvents()
        }
        verify(getNodeByIdUseCase, never()).invoke(NodeId(1L))
    }
}
