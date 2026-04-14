package mega.privacy.mobile.home.presentation.home.widget.continuewhereleftoff

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
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
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class ContinueWhereLeftOffViewModelTest {

    companion object {
        @JvmField
        @RegisterExtension
        val extension = CoroutineMainDispatcherExtension()
    }

    private val monitorContinueWhereLeftOffItemsUseCase =
        mock<MonitorContinueWhereLeftOffItemsUseCase>()
    private val getNodeByIdUseCase = mock<GetNodeByIdUseCase>()
    private val fakeFlow = MutableSharedFlow<List<ContinueWhereLeftOffItem>>()

    private lateinit var underTest: ContinueWhereLeftOffViewModel

    @BeforeEach
    fun setUp() {
        whenever(monitorContinueWhereLeftOffItemsUseCase(10)).thenReturn(fakeFlow)
        underTest = ContinueWhereLeftOffViewModel(
            monitorContinueWhereLeftOffItemsUseCase = monitorContinueWhereLeftOffItemsUseCase,
            getNodeByIdUseCase = getNodeByIdUseCase,
        )
    }

    @Test
    fun `test that initial state is empty list`() = runTest {
        underTest.items.test {
            assertThat(awaitItem()).isEmpty()
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

        underTest.items.test {
            assertThat(awaitItem()).isEmpty()
            fakeFlow.emit(items)
            assertThat(awaitItem()).isEqualTo(items)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that resolveNode returns TypedFileNode for valid handle`() = runTest {
        val nodeHandle = 123L
        val expectedNode = mock<TypedFileNode>()
        whenever(getNodeByIdUseCase(NodeId(nodeHandle))).thenReturn(expectedNode)

        val result = underTest.resolveNode(nodeHandle)

        assertThat(result).isEqualTo(expectedNode)
    }

    @Test
    fun `test that resolveNode returns null when use case throws`() = runTest {
        val nodeHandle = 456L
        whenever(getNodeByIdUseCase(NodeId(nodeHandle))).thenThrow(RuntimeException("Not found"))

        val result = underTest.resolveNode(nodeHandle)

        assertThat(result).isNull()
    }
}
