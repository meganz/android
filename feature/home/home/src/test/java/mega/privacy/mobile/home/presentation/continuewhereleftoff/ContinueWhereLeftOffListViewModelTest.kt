package mega.privacy.mobile.home.presentation.continuewhereleftoff

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.StateEventWithContentConsumed
import de.palm.composestateevents.StateEventWithContentTriggered
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.continuewhereleftoff.ContinueWhereLeftOffItem
import mega.privacy.android.domain.entity.continuewhereleftoff.RecentlyUsedType
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.continuewhereleftoff.MonitorContinueWhereLeftOffItemsUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ContinueWhereLeftOffListViewModelTest {

    companion object {
        @JvmField
        @RegisterExtension
        val extension = CoroutineMainDispatcherExtension()
    }

    private val monitorContinueWhereLeftOffItemsUseCase =
        mock<MonitorContinueWhereLeftOffItemsUseCase>()
    private val getNodeByIdUseCase = mock<GetNodeByIdUseCase>()
    private val fakeFlow = MutableSharedFlow<List<ContinueWhereLeftOffItem>>()

    private lateinit var underTest: ContinueWhereLeftOffListViewModel

    @BeforeEach
    fun setUp() {
        whenever(monitorContinueWhereLeftOffItemsUseCase(50)).thenReturn(fakeFlow)
        underTest = ContinueWhereLeftOffListViewModel(
            monitorContinueWhereLeftOffItemsUseCase = monitorContinueWhereLeftOffItemsUseCase,
            getNodeByIdUseCase = getNodeByIdUseCase,
        )
    }

    @Test
    fun `test that isLoading is initially true`() = runTest {
        underTest.uiState.test {
            assertThat(awaitItem().isLoading).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that isLoading becomes false after first emission`() = runTest {
        assertThat(underTest.uiState.value.isLoading).isTrue()
        fakeFlow.emit(emptyList())
        assertThat(underTest.uiState.value.isLoading).isFalse()
    }

    @Test
    fun `test that initial items is empty list`() = runTest {
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
            ),
            ContinueWhereLeftOffItem(
                nodeHandle = 2L,
                type = RecentlyUsedType.Video,
                title = "video.mp4",
                lastAccessedTimestamp = 2000L,
            ),
        )

        underTest.uiState.test {
            assertThat(awaitItem().items).isEmpty()
            fakeFlow.emit(items)
            assertThat(awaitItem().items).isEqualTo(items)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that onItemClicked triggers openNodeEvent when node is resolved`() = runTest {
        val nodeHandle = 123L
        val expectedNode = mock<TypedFileNode>()
        whenever(getNodeByIdUseCase(NodeId(nodeHandle))).thenReturn(expectedNode)

        underTest.uiState.test {
            awaitItem() // initial state
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
        val nodeHandle = 456L
        whenever(getNodeByIdUseCase(NodeId(nodeHandle))).thenThrow(RuntimeException("Not found"))

        underTest.onItemClicked(nodeHandle)

        assertThat(underTest.uiState.value.openNodeEvent)
            .isInstanceOf(StateEventWithContentConsumed::class.java)
    }

    @Test
    fun `test that onOpenNodeEventConsumed resets the event`() = runTest {
        val nodeHandle = 123L
        val expectedNode = mock<TypedFileNode>()
        whenever(getNodeByIdUseCase(NodeId(nodeHandle))).thenReturn(expectedNode)

        underTest.uiState.test {
            awaitItem() // initial state
            underTest.onItemClicked(nodeHandle)
            awaitItem() // triggered state
            underTest.onOpenNodeEventConsumed()
            val state = awaitItem()
            assertThat(state.openNodeEvent)
                .isInstanceOf(StateEventWithContentConsumed::class.java)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
