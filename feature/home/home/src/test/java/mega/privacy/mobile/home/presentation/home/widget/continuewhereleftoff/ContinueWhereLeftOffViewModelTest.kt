package mega.privacy.mobile.home.presentation.home.widget.continuewhereleftoff

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.StateEventWithContentConsumed
import de.palm.composestateevents.StateEventWithContentTriggered
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.continuewhereleftoff.ContinueWhereLeftOffItem
import mega.privacy.android.domain.entity.continuewhereleftoff.RecentlyUsedType
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.continuewhereleftoff.MonitorContinueWhereLeftOffItemsUseCase
import mega.privacy.mobile.home.presentation.continuewhereleftoff.ContinueWhereLeftOffNameResolver
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(CoroutineMainDispatcherExtension::class)
class ContinueWhereLeftOffViewModelTest {

    private lateinit var underTest: ContinueWhereLeftOffViewModel

    private val monitorContinueWhereLeftOffItemsUseCase =
        mock<MonitorContinueWhereLeftOffItemsUseCase>()
    private val getNodeByIdUseCase = mock<GetNodeByIdUseCase>()

    @BeforeEach
    fun setUp() {
        underTest = ContinueWhereLeftOffViewModel(
            monitorContinueWhereLeftOffItemsUseCase = monitorContinueWhereLeftOffItemsUseCase,
            getNodeByIdUseCase = getNodeByIdUseCase,
            nameResolver = ContinueWhereLeftOffNameResolver(getNodeByIdUseCase),
        )
    }

    @AfterEach
    fun tearDown() {
        reset(monitorContinueWhereLeftOffItemsUseCase, getNodeByIdUseCase)
    }

    @Test
    fun `test that initial items is empty list`() = runTest {
        stubEmptyItems()

        underTest.uiState.test {
            assertThat(awaitItem().items).isEmpty()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that items are emitted from use case`() = runTest {
        val items = listOf(
            ContinueWhereLeftOffItem(
                nodeHandle = 1L,
                type = RecentlyUsedType.PDF,
                title = "test.pdf",
                lastAccessedTimestamp = 1000L,
            )
        )
        monitorContinueWhereLeftOffItemsUseCase.stub {
            on { invoke(10) } doReturn flow {
                emit(items)
                awaitCancellation()
            }
        }

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.items).isEqualTo(items)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that blank title is resolved from node when available`() = runTest {
        val items = listOf(
            ContinueWhereLeftOffItem(
                nodeHandle = 10L,
                type = RecentlyUsedType.PDF,
                title = "",
                lastAccessedTimestamp = 1000L,
            )
        )
        val typedNode = mock<TypedFileNode> {
            on { name }.thenReturn("actual_name.pdf")
        }
        whenever(getNodeByIdUseCase(NodeId(10L))).thenReturn(typedNode)
        monitorContinueWhereLeftOffItemsUseCase.stub {
            on { invoke(10) } doReturn flow {
                emit(items)
                awaitCancellation()
            }
        }

        underTest.uiState.test {
            // UnconfinedTestDispatcher resolves synchronously — no blank intermediate
            val state = awaitItem()
            assertThat(state.items).hasSize(1)
            assertThat(state.items[0].title).isEqualTo("actual_name.pdf")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that non-blank title is not resolved from node`() = runTest {
        val items = listOf(
            ContinueWhereLeftOffItem(
                nodeHandle = 20L,
                type = RecentlyUsedType.Video,
                title = "stored_name.mp4",
                lastAccessedTimestamp = 2000L,
            )
        )
        monitorContinueWhereLeftOffItemsUseCase.stub {
            on { invoke(10) } doReturn flow {
                emit(items)
                awaitCancellation()
            }
        }

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.items).hasSize(1)
            assertThat(state.items[0].title).isEqualTo("stored_name.mp4")
            verify(getNodeByIdUseCase, never()).invoke(NodeId(20L))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that resolved name is cached and not fetched again`() = runTest {
        val fakeFlow = MutableSharedFlow<List<ContinueWhereLeftOffItem>>()
        monitorContinueWhereLeftOffItemsUseCase.stub {
            on { invoke(10) } doReturn fakeFlow
        }
        val items = listOf(
            ContinueWhereLeftOffItem(
                nodeHandle = 30L,
                type = RecentlyUsedType.PDF,
                title = "",
                lastAccessedTimestamp = 1000L,
            )
        )
        val typedNode = mock<TypedFileNode> {
            on { name }.thenReturn("resolved.pdf")
        }
        whenever(getNodeByIdUseCase(NodeId(30L))).thenReturn(typedNode)

        underTest.uiState.test {
            awaitItem() // initial empty
            fakeFlow.emit(items)
            awaitItem() // blank name (first transformLatest emit)
            assertThat(awaitItem().items[0].title).isEqualTo("resolved.pdf")
            // re-emit with different timestamp to force new state
            fakeFlow.emit(listOf(items[0].copy(lastAccessedTimestamp = 2000L)))
            // cached name applied immediately in first transformLatest emit
            assertThat(awaitItem().items[0].title).isEqualTo("resolved.pdf")
            verify(getNodeByIdUseCase, times(1)).invoke(NodeId(30L))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that onItemClicked triggers openNodeEvent when node is resolved`() = runTest {
        stubEmptyItems()
        val nodeHandle = 123L
        val expectedNode = mock<TypedFileNode>()
        whenever(getNodeByIdUseCase(NodeId(nodeHandle))).thenReturn(expectedNode)

        underTest.uiState.test {
            awaitItem() // initial
            underTest.onItemClicked(nodeHandle)
            val state = awaitItem()
            assertThat(state.openNodeEvent).isInstanceOf(StateEventWithContentTriggered::class.java)
            assertThat((state.openNodeEvent as StateEventWithContentTriggered).content)
                .isEqualTo(expectedNode)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that onItemClicked does not trigger event when use case throws`() = runTest {
        stubEmptyItems()
        val nodeHandle = 456L
        whenever(getNodeByIdUseCase(NodeId(nodeHandle))).thenThrow(RuntimeException("Not found"))

        underTest.uiState.test {
            val state = awaitItem()
            underTest.onItemClicked(nodeHandle)
            assertThat(state.openNodeEvent)
                .isInstanceOf(StateEventWithContentConsumed::class.java)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that onOpenNodeEventConsumed resets the event`() = runTest {
        stubEmptyItems()
        val nodeHandle = 123L
        val expectedNode = mock<TypedFileNode>()
        whenever(getNodeByIdUseCase(NodeId(nodeHandle))).thenReturn(expectedNode)

        underTest.uiState.test {
            awaitItem() // initial
            underTest.onItemClicked(nodeHandle)
            awaitItem() // triggered
            underTest.onOpenNodeEventConsumed()
            val state = awaitItem()
            assertThat(state.openNodeEvent)
                .isInstanceOf(StateEventWithContentConsumed::class.java)
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun stubEmptyItems() {
        monitorContinueWhereLeftOffItemsUseCase.stub {
            on { invoke(10) } doReturn flow {
                emit(emptyList<ContinueWhereLeftOffItem>())
                awaitCancellation()
            }
        }
    }
}
