package mega.privacy.android.shared.nodes.model

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.SortDirection
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.usecase.GetOthersSortOrder
import mega.privacy.android.domain.usecase.SetCloudSortOrder
import mega.privacy.android.domain.usecase.SetOthersSortOrder
import mega.privacy.android.domain.usecase.node.sort.MonitorSortCloudOrderUseCase
import mega.privacy.android.domain.usecase.viewtype.MonitorViewType
import mega.privacy.android.domain.usecase.viewtype.SetViewType
import mega.privacy.android.shared.nodes.mapper.NodeSortConfigurationUiMapper
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@ExtendWith(CoroutineMainDispatcherExtension::class)
class NodeHeaderItemViewModelTest {

    private lateinit var underTest: NodeHeaderItemViewModel

    private val monitorViewTypeUseCase = mock<MonitorViewType>()
    private val setViewTypeUseCase = mock<SetViewType>()
    private val monitorSortCloudOrderUseCase = mock<MonitorSortCloudOrderUseCase>()
    private val setCloudSortOrderUseCase = mock<SetCloudSortOrder>()
    private val getOthersSortOrder = mock<GetOthersSortOrder>()
    private val setOthersSortOrder = mock<SetOthersSortOrder>()
    private val nodeSortConfigurationUiMapper = NodeSortConfigurationUiMapper()

    @AfterEach
    fun tearDown() {
        reset(
            monitorViewTypeUseCase,
            setViewTypeUseCase,
            monitorSortCloudOrderUseCase,
            setCloudSortOrderUseCase,
            getOthersSortOrder,
            setOthersSortOrder,
        )
    }

    private fun initTest(nodeSourceType: NodeSourceType) {
        underTest = NodeHeaderItemViewModel(
            monitorViewTypeUseCase = monitorViewTypeUseCase,
            setViewTypeUseCase = setViewTypeUseCase,
            monitorSortCloudOrderUseCase = monitorSortCloudOrderUseCase,
            setCloudSortOrderUseCase = setCloudSortOrderUseCase,
            getOthersSortOrder = getOthersSortOrder,
            setOthersSortOrder = setOthersSortOrder,
            nodeSortConfigurationUiMapper = nodeSortConfigurationUiMapper,
            nodeSourceType = nodeSourceType,
        )
    }

    @Nested
    inner class CloudDriveTests {

        @BeforeEach
        fun setUp() {
            initTest(NodeSourceType.CLOUD_DRIVE)
        }

        @Test
        fun `test that initial state is Loading`() = runTest {
            assertThat(underTest.uiState.value).isEqualTo(NodeHeaderItemUiState.Loading)
        }

        @Test
        fun `test that data state is emitted when cloud sort order monitor emits`() = runTest {
            whenever(monitorViewTypeUseCase()) doReturn flowOf(ViewType.LIST)
            whenever(monitorSortCloudOrderUseCase()) doReturn flowOf(SortOrder.ORDER_DEFAULT_ASC)

            underTest.uiState.test {
                assertThat(awaitDataState()).isEqualTo(
                    NodeHeaderItemUiState.Data(
                        viewType = ViewType.LIST,
                        nodeSortConfiguration = NodeSortConfiguration.default,
                    )
                )
            }
        }

        @Test
        fun `test that view type is updated when monitor emits`() = runTest {
            val viewTypeFlow = MutableStateFlow(ViewType.LIST)
            whenever(monitorViewTypeUseCase()) doReturn viewTypeFlow
            whenever(monitorSortCloudOrderUseCase()) doReturn flowOf(SortOrder.ORDER_DEFAULT_ASC)

            underTest.uiState.test {
                assertThat(awaitDataState()).isEqualTo(
                    NodeHeaderItemUiState.Data(
                        viewType = ViewType.LIST,
                        nodeSortConfiguration = NodeSortConfiguration.default,
                    )
                )

                viewTypeFlow.value = ViewType.GRID

                assertThat(awaitDataStateWithViewType(ViewType.GRID)).isEqualTo(
                    NodeHeaderItemUiState.Data(
                        viewType = ViewType.GRID,
                        nodeSortConfiguration = NodeSortConfiguration.default,
                    )
                )
            }
        }

        @Test
        fun `test that updateViewType toggles and calls setViewTypeUseCase when state is Data`() =
            runTest {
                whenever(monitorViewTypeUseCase()) doReturn flowOf(ViewType.LIST)
                whenever(monitorSortCloudOrderUseCase()) doReturn flowOf(SortOrder.ORDER_DEFAULT_ASC)

                underTest.uiState.test {
                    awaitDataState()

                    underTest.updateViewType()
                    advanceUntilIdle()

                    verify(setViewTypeUseCase).invoke(ViewType.GRID)
                }
            }

        @Test
        fun `test that updateNodeSortConfiguration calls setCloudSortOrderUseCase`() = runTest {
            val config = NodeSortConfiguration(NodeSortOption.Modified, SortDirection.Descending)

            underTest.updateNodeSortConfiguration(config)
            advanceUntilIdle()

            verify(setCloudSortOrderUseCase).invoke(SortOrder.ORDER_MODIFICATION_DESC)
        }

        @Test
        fun `test that sort configuration is updated when monitor emits`() = runTest {
            whenever(monitorViewTypeUseCase()) doReturn flowOf(ViewType.LIST)
            whenever(monitorSortCloudOrderUseCase()) doReturn flowOf(SortOrder.ORDER_MODIFICATION_DESC)
            val expected = NodeHeaderItemUiState.Data(
                viewType = ViewType.LIST,
                nodeSortConfiguration = NodeSortConfiguration(
                    sortOption = NodeSortOption.Modified,
                    sortDirection = SortDirection.Descending,
                ),
            )

            underTest.uiState.test {
                assertThat(awaitDataStateWithSortConfiguration(expected.nodeSortConfiguration))
                    .isEqualTo(expected)
            }
        }
    }

    @Nested
    inner class IncomingSharesTests {

        @BeforeEach
        fun setUp() {
            initTest(NodeSourceType.INCOMING_SHARES)
        }

        @Test
        fun `test that initial state is Loading`() = runTest {
            assertThat(underTest.uiState.value).isEqualTo(NodeHeaderItemUiState.Loading)
        }

        @Test
        fun `test that initial state loads others sort order`() = runTest {
            whenever(monitorViewTypeUseCase()) doReturn flowOf(ViewType.LIST)
            whenever(getOthersSortOrder()) doReturn SortOrder.ORDER_MODIFICATION_DESC
            val expected = NodeHeaderItemUiState.Data(
                viewType = ViewType.LIST,
                nodeSortConfiguration = NodeSortConfiguration(
                    sortOption = NodeSortOption.Modified,
                    sortDirection = SortDirection.Descending,
                ),
            )

            underTest.uiState.test {
                assertThat(awaitDataStateWithSortConfiguration(expected.nodeSortConfiguration))
                    .isEqualTo(expected)
            }
        }

        @Test
        fun `test that updateNodeSortConfiguration calls setOthersSortOrder and updates state`() =
            runTest {
                whenever(monitorViewTypeUseCase()) doReturn flowOf(ViewType.LIST)
                whenever(getOthersSortOrder()) doReturn SortOrder.ORDER_DEFAULT_ASC
                val config = NodeSortConfiguration(NodeSortOption.Size, SortDirection.Ascending)

                underTest.uiState.test {
                    assertThat(awaitDataState()).isEqualTo(
                        NodeHeaderItemUiState.Data(
                            viewType = ViewType.LIST,
                            nodeSortConfiguration = NodeSortConfiguration.default,
                        )
                    )

                    underTest.updateNodeSortConfiguration(config)
                    advanceUntilIdle()

                    verify(setOthersSortOrder).invoke(SortOrder.ORDER_SIZE_ASC)
                    assertThat(awaitDataStateWithSortConfiguration(config)).isEqualTo(
                        NodeHeaderItemUiState.Data(
                            viewType = ViewType.LIST,
                            nodeSortConfiguration = config,
                        )
                    )
                }
            }
    }

    private suspend fun ReceiveTurbine<NodeHeaderItemUiState>.awaitDataState(): NodeHeaderItemUiState.Data {
        var item = awaitItem()
        while (item !is NodeHeaderItemUiState.Data) {
            item = awaitItem()
        }
        return item
    }

    private suspend fun ReceiveTurbine<NodeHeaderItemUiState>.awaitDataStateWithSortConfiguration(
        expected: NodeSortConfiguration,
    ): NodeHeaderItemUiState.Data {
        var item = awaitDataState()
        while (item.nodeSortConfiguration != expected) {
            item = awaitDataState()
        }
        return item
    }

    private suspend fun ReceiveTurbine<NodeHeaderItemUiState>.awaitDataStateWithViewType(
        expected: ViewType,
    ): NodeHeaderItemUiState.Data {
        var item = awaitDataState()
        while (item.viewType != expected) {
            item = awaitDataState()
        }
        return item
    }
}
