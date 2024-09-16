package mega.privacy.android.app.presentation.search.model

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.data.NodeUIItem
import mega.privacy.android.app.presentation.search.SearchActivity
import mega.privacy.android.app.presentation.search.SearchActivityViewModel
import mega.privacy.android.app.presentation.search.mapper.DateFilterOptionStringResMapper
import mega.privacy.android.app.presentation.search.mapper.EmptySearchViewMapper
import mega.privacy.android.app.presentation.search.mapper.NodeSourceTypeToSearchTargetMapper
import mega.privacy.android.app.presentation.search.mapper.SearchFilterMapper
import mega.privacy.android.app.presentation.search.mapper.TypeFilterOptionStringResMapper
import mega.privacy.android.app.presentation.search.mapper.TypeFilterToSearchMapper
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.AccountSubscriptionCycle
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.account.AccountDetail
import mega.privacy.android.domain.entity.account.AccountLevelDetail
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.NodeUpdate
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.entity.search.DateFilterOption
import mega.privacy.android.domain.entity.search.SearchParameters
import mega.privacy.android.domain.entity.search.SearchTarget
import mega.privacy.android.domain.entity.search.TypeFilterOption
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.canceltoken.CancelCancelTokenUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.offline.MonitorOfflineNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.search.SearchUseCase
import mega.privacy.android.domain.usecase.setting.MonitorShowHiddenItemsUseCase
import mega.privacy.android.domain.usecase.viewtype.MonitorViewType
import mega.privacy.android.domain.usecase.viewtype.SetViewType
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SearchActivityViewModelTest {
    private lateinit var underTest: SearchActivityViewModel
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase = mock()
    private val monitorNodeUpdatesFakeFlow = MutableSharedFlow<NodeUpdate>()
    private val monitorNodeUpdatesUseCase: MonitorNodeUpdatesUseCase = mock()
    private val cancelCancelTokenUseCase: CancelCancelTokenUseCase = mock()
    private val searchFilterMapper: SearchFilterMapper = mock()
    private val nodeSourceTypeToSearchTargetMapper: NodeSourceTypeToSearchTargetMapper = mock()
    private val typeFilterToSearchMapper: TypeFilterToSearchMapper = mock()
    private val emptySearchViewMapper: EmptySearchViewMapper = mock()
    private val stateHandle: SavedStateHandle = mock()
    private val setViewType: SetViewType = mock()
    private val monitorViewType: MonitorViewType = mock()
    private val getCloudSortOrder: GetCloudSortOrder = mock()
    private val typeFilterStringMapper: TypeFilterOptionStringResMapper = mock()
    private val dateFilterStringMapper: DateFilterOptionStringResMapper = mock()
    private val monitorOfflineNodeUpdatesUseCase: MonitorOfflineNodeUpdatesUseCase = mock()
    private val searchUseCase: SearchUseCase = mock()
    private val monitorAccountDetailUseCase: MonitorAccountDetailUseCase = mock()
    private val monitorShowHiddenItemsUseCase: MonitorShowHiddenItemsUseCase = mock()
    private val accountDetailFakeFlow = MutableSharedFlow<AccountDetail>()


    private val nodeList = mutableListOf<TypedNode>()

    private val parentHandle = 123456L
    private val nodeSourceType = NodeSourceType.CLOUD_DRIVE

    @BeforeEach
    fun setUp() {
        runBlocking {
            stubCommon()
        }
        initViewModel()
    }

    private fun initViewModel() {
        underTest = SearchActivityViewModel(
            getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
            monitorNodeUpdatesUseCase = monitorNodeUpdatesUseCase,
            setViewType = setViewType,
            monitorViewType = monitorViewType,
            stateHandle = stateHandle,
            getCloudSortOrder = getCloudSortOrder,
            cancelCancelTokenUseCase = cancelCancelTokenUseCase,
            searchFilterMapper = searchFilterMapper,
            nodeSourceTypeToSearchTargetMapper = nodeSourceTypeToSearchTargetMapper,
            typeFilterToSearchMapper = typeFilterToSearchMapper,
            emptySearchViewMapper = emptySearchViewMapper,
            monitorOfflineNodeUpdatesUseCase = monitorOfflineNodeUpdatesUseCase,
            typeFilterOptionStringResMapper = typeFilterStringMapper,
            dateFilterOptionStringResMapper = dateFilterStringMapper,
            searchUseCase = searchUseCase,
            monitorAccountDetailUseCase = monitorAccountDetailUseCase,
            monitorShowHiddenItemsUseCase = monitorShowHiddenItemsUseCase,
        )
    }

    private suspend fun stubCommon() {
        whenever(monitorNodeUpdatesUseCase()).thenReturn(monitorNodeUpdatesFakeFlow)
        whenever(monitorOfflineNodeUpdatesUseCase()).thenReturn(emptyFlow())
        whenever(monitorViewType()).thenReturn(emptyFlow())
        whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_NONE)

        whenever(stateHandle.get<NodeSourceType>(SearchActivity.SEARCH_TYPE)).thenReturn(
            NodeSourceType.CLOUD_DRIVE
        )
        whenever(stateHandle.get<Long>(SearchActivity.PARENT_HANDLE)).thenReturn(123456L)
        whenever(stateHandle.get<Boolean>(SearchActivity.IS_FIRST_LEVEL)).thenReturn(
            false
        )
        whenever(monitorShowHiddenItemsUseCase()).thenReturn(flowOf(false))
        whenever(monitorAccountDetailUseCase()).thenReturn(accountDetailFakeFlow)
        whenever(nodeSourceTypeToSearchTargetMapper(any())).thenReturn(SearchTarget.ROOT_NODES)
    }

    @AfterEach
    fun tearDown() {
        nodeList.clear()
        reset(
            getFeatureFlagValueUseCase,
            monitorNodeUpdatesUseCase,
            setViewType,
            monitorViewType,
            stateHandle,
            getCloudSortOrder,
            cancelCancelTokenUseCase,
            searchFilterMapper,
            nodeSourceTypeToSearchTargetMapper,
            typeFilterToSearchMapper,
            emptySearchViewMapper,
            monitorOfflineNodeUpdatesUseCase,
            typeFilterStringMapper,
            dateFilterStringMapper,
            searchUseCase,
            monitorAccountDetailUseCase,
            monitorShowHiddenItemsUseCase,
        )
    }

    @Test
    fun `test that the selected item is updated by 1 when nodeUIItem is long clicked`() =
        runTest {
            val nodesListItem1 = mock<TypedFolderNode>()
            val nodesListItem2 = mock<TypedFileNode>()
            whenever(nodesListItem1.id.longValue).thenReturn(1L)
            whenever(nodesListItem2.id.longValue).thenReturn(2L)
            whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_NONE)
            whenever(monitorViewType()).thenReturn(flowOf(ViewType.LIST))
            underTest.onSortOrderChanged()
            underTest.onLongItemClicked(
                NodeUIItem(
                    nodesListItem1,
                    isSelected = false,
                    isInvisible = false
                )
            )
            underTest.state.test {
                val state = awaitItem()
                assertThat(state.selectedNodes.size).isEqualTo(1)
                assertThat(state.selectedNodes.filter { it is FileNode }.size).isEqualTo(0)
            }
        }

    @Test
    fun `test that the checked index gets decremented by 1 when the item is clicked and some items are already selected in the list`() =
        runTest {
            val nodesListItem1 = mock<TypedFolderNode>()
            val nodesListItem2 = mock<TypedFileNode>()
            whenever(nodesListItem1.id.longValue).thenReturn(1L)
            whenever(nodesListItem2.id.longValue).thenReturn(2L)
            whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_NONE)
            whenever(monitorViewType()).thenReturn(flowOf(ViewType.LIST))
            underTest.onSortOrderChanged()
            underTest.onItemClicked(
                NodeUIItem(
                    nodesListItem1,
                    isSelected = true,
                    isInvisible = false
                )
            )
            underTest.state.test {
                val state = awaitItem()
                assertThat(state.selectedNodes.size).isEqualTo(0)
            }
            underTest.onItemClicked(
                NodeUIItem(
                    nodesListItem2,
                    isSelected = true,
                    isInvisible = false
                )
            )
            underTest.state.test {
                val state = awaitItem()
                assertThat(state.selectedNodes.size).isEqualTo(0)
            }
        }

    @Test
    fun `test that the checked index gets incremented by 1 when the selected item gets clicked`() =
        runTest {
            val nodesListItem1 = mock<TypedFileNode>()
            val nodesListItem2 = mock<TypedFolderNode>()
            whenever(nodesListItem1.id.longValue).thenReturn(1L)
            whenever(nodesListItem2.id.longValue).thenReturn(2L)
            whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_NONE)
            whenever(monitorViewType()).thenReturn(flowOf(ViewType.LIST))
            underTest.onSortOrderChanged()
            underTest.onLongItemClicked(
                NodeUIItem(
                    nodesListItem1,
                    isSelected = false,
                    isInvisible = false
                )
            )
            underTest.onItemClicked(
                NodeUIItem(
                    nodesListItem2,
                    isSelected = false,
                    isInvisible = false
                )
            )
            underTest.state.test {
                val state = awaitItem()
                assertThat(state.selectedNodes.size).isEqualTo(2)
                assertThat(state.selectedNodes.filter { it is FileNode }.size).isEqualTo(1)
            }
            underTest.onItemClicked(
                NodeUIItem(
                    nodesListItem1,
                    isSelected = true,
                    isInvisible = false
                )
            )
            underTest.state.test {
                val state = awaitItem()
                assertThat(state.selectedNodes.size).isEqualTo(1)
                assertThat(state.selectedNodes.any { it is FileNode }).isFalse()
            }
        }

    @Test
    fun `test that setting a type filter will change the filter state`() =
        runTest {
            val type = TypeFilterWithName(
                TypeFilterOption.Images,
                R.string.search_dropdown_chip_filter_type_file_type_images
            )
            underTest.setTypeSelectedFilterOption(type)
            underTest.state.test {
                val state = awaitItem()
                assertThat(state.typeSelectedFilterOption).isEqualTo(type)
                assertThat(state.dateModifiedSelectedFilterOption).isEqualTo(null)
                assertThat(state.dateAddedSelectedFilterOption).isEqualTo(null)
            }
        }

    @Test
    fun `test that setting a date modified filter will change the filter state`() =
        runTest {
            val date = DateFilterWithName(
                DateFilterOption.Today,
                R.string.search_dropdown_chip_filter_type_date_today
            )
            underTest.setDateModifiedSelectedFilterOption(date)
            underTest.state.test {
                val state = awaitItem()
                assertThat(state.typeSelectedFilterOption).isEqualTo(null)
                assertThat(state.dateModifiedSelectedFilterOption).isEqualTo(date)
                assertThat(state.dateAddedSelectedFilterOption).isEqualTo(null)
            }
        }

    @Test
    fun `test that setting a date added filter will change the filter state`() =
        runTest {
            val date = DateFilterWithName(
                DateFilterOption.Older,
                R.string.search_dropdown_chip_filter_type_date_older
            )
            underTest.setDateAddedSelectedFilterOption(date)
            underTest.state.test {
                val state = awaitItem()
                assertThat(state.typeSelectedFilterOption).isEqualTo(null)
                assertThat(state.dateModifiedSelectedFilterOption).isEqualTo(null)
                assertThat(state.dateAddedSelectedFilterOption).isEqualTo(date)
            }
        }

    @Test
    fun `test that setViewType is called at least once when changing the view type to grid`() =
        runTest {
            underTest.onChangeViewTypeClicked()
            verify(setViewType).invoke(ViewType.GRID)
        }

    @Test
    fun `test that the search functionality is performed with an updated filter when update search query is called`() =
        runTest {
            val query = "query"

            val typedFolderNode = mock<TypedFolderNode> {
                on { id }.thenReturn(NodeId(345L))
                on { name }.thenReturn("folder node")
            }
            val typedFileNode = mock<TypedFileNode> {
                on { id }.thenReturn(NodeId(123L))
                on { name }.thenReturn("file node")
            }

            val parentHandle = 123456L
            val nodeSourceType = NodeSourceType.CLOUD_DRIVE
            whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_NONE)
            whenever(monitorViewType()).thenReturn(flowOf(ViewType.LIST))

            nodeList.add(typedFileNode)
            nodeList.add(typedFolderNode)

            whenever(
                searchUseCase(
                    parentHandle = NodeId(parentHandle),
                    nodeSourceType = nodeSourceType,
                    searchParameters = SearchParameters(
                        query = query,
                    ),
                )
            ).thenReturn(nodeList)
            underTest.updateSearchQuery(query)
            underTest.state.test {
                val state = awaitItem()
                assertThat(state.searchQuery).isEqualTo(query)
                assertThat(state.searchItemList.size).isEqualTo(nodeList.size)
            }
        }

    @Test
    fun `test that the error message id is updated when show error message is called`() =
        runTest {
            val errorMessageId = 123
            underTest.showShowErrorMessage(errorMessageId)
            underTest.state.test {
                val state = awaitItem()
                assertThat(state.errorMessageId).isEqualTo(errorMessageId)
            }
            underTest.errorMessageShown()
            underTest.state.test {
                val state = awaitItem()
                assertThat(state.errorMessageId).isNull()
            }
        }

    @Test
    fun `test that selected list updates when select all and clear selection performed`() =
        runTest {
            val query = "query"
            val typedFolderNode = mock<TypedFolderNode> {
                on { id }.thenReturn(NodeId(345L))
                on { name }.thenReturn("folder node")
            }
            val typedFileNode = mock<TypedFileNode> {
                on { id }.thenReturn(NodeId(123L))
                on { name }.thenReturn("file node")
            }

            whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_NONE)
            whenever(monitorViewType()).thenReturn(flowOf(ViewType.LIST))
            whenever(
                searchUseCase(
                    parentHandle = NodeId(parentHandle),
                    nodeSourceType = nodeSourceType,
                    searchParameters = SearchParameters(
                        query = query,
                    ),
                )
            ).thenReturn(listOf(typedFileNode, typedFolderNode))
            underTest.updateSearchQuery(query)
            underTest.state.test {
                val state = awaitItem()
                assertThat(state.searchQuery).isEqualTo(query)
                assertThat(state.searchItemList.size).isEqualTo(2)
                assertThat(state.selectedNodes).isEmpty()
                underTest.selectAll()
                val selectAllState = awaitItem()
                assertThat(selectAllState.selectedNodes).contains(typedFileNode)
                assertThat(selectAllState.selectedNodes).contains(typedFolderNode)
                assertThat(selectAllState.selectedNodes.size).isEqualTo(2)
                underTest.clearSelection()
                val clearedState = awaitItem()
                assertThat(clearedState.selectedNodes.size).isEqualTo(0)
            }
        }

    @Test
    fun `test that sensitive nodes should be filtered with subscribed account`() = runTest {
        // given
        val accountDetail = AccountDetail(
            levelDetail = AccountLevelDetail(
                accountType = AccountType.BUSINESS,
                subscriptionStatus = null,
                subscriptionRenewTime = 0L,
                accountSubscriptionCycle = AccountSubscriptionCycle.UNKNOWN,
                proExpirationTime = 0L,
                accountPlanDetail = null,
                accountSubscriptionDetailList = listOf(),
            )
        )
        accountDetailFakeFlow.emit(accountDetail)

        val query = "query"
        val typedFolderNode = mock<TypedFolderNode> {
            on { id }.thenReturn(NodeId(345L))
            on { name }.thenReturn("folder node")
        }
        val typedFileNode = mock<TypedFileNode> {
            on { id }.thenReturn(NodeId(123L))
            on { name }.thenReturn("file node")
            on { isMarkedSensitive }.thenReturn(true)
            on { isSensitiveInherited }.thenReturn(true)
        }

        whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_NONE)
        whenever(monitorViewType()).thenReturn(flowOf(ViewType.LIST))
        whenever(
            searchUseCase(
                parentHandle = NodeId(parentHandle),
                nodeSourceType = nodeSourceType,
                searchParameters = SearchParameters(
                    query = query,
                ),
            )
        ).thenReturn(listOf(typedFileNode, typedFolderNode))

        // when
        underTest.updateSearchQuery(query)

        // then
        underTest.state.test {
            val state = awaitItem()
            assertThat(state.searchItemList.size).isEqualTo(1)
        }
    }

    @Test
    fun `test that sensitive nodes should not be filtered with non-subscribed account`() = runTest {
        // given
        val accountDetail = AccountDetail(
            levelDetail = AccountLevelDetail(
                accountType = AccountType.FREE,
                subscriptionStatus = null,
                subscriptionRenewTime = 0L,
                accountSubscriptionCycle = AccountSubscriptionCycle.UNKNOWN,
                proExpirationTime = 0L,
                accountPlanDetail = null,
                accountSubscriptionDetailList = listOf(),
            )
        )
        accountDetailFakeFlow.emit(accountDetail)

        val query = "query"
        val typedFolderNode = mock<TypedFolderNode> {
            on { id }.thenReturn(NodeId(345L))
            on { name }.thenReturn("folder node")
            on { isMarkedSensitive }.thenReturn(true)
            on { isSensitiveInherited }.thenReturn(true)
        }
        val typedFileNode = mock<TypedFileNode> {
            on { id }.thenReturn(NodeId(123L))
            on { name }.thenReturn("file node")
            on { isMarkedSensitive }.thenReturn(true)
            on { isSensitiveInherited }.thenReturn(true)
        }

        whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_NONE)
        whenever(monitorViewType()).thenReturn(flowOf(ViewType.LIST))
        whenever(
            searchUseCase(
                parentHandle = NodeId(parentHandle),
                nodeSourceType = nodeSourceType,
                searchParameters = SearchParameters(
                    query = query,
                ),
            )
        ).thenReturn(listOf(typedFileNode, typedFolderNode))

        // when
        underTest.updateSearchQuery(query)

        // then
        underTest.state.test {
            val state = awaitItem()
            assertThat(state.searchItemList.size).isEqualTo(2)
        }
    }

    @Test
    fun `test that navigation level is updated when open folder and navigate back is clicked`() =
        runTest {
            val typedFolderNode = mock<TypedFolderNode> {
                on { id }.thenReturn(NodeId(345L))
                on { name }.thenReturn("folder node")
            }
            whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_NONE)
            whenever(monitorViewType()).thenReturn(flowOf(ViewType.LIST))
            underTest.onSortOrderChanged()
            underTest.openFolder(typedFolderNode.id.longValue, typedFolderNode.name)
            underTest.state.test {
                val state = awaitItem()
                assertThat(state.navigationLevel.size).isEqualTo(1)
            }

            underTest.navigateBack()
            underTest.state.test {
                val state = awaitItem()
                assertThat(state.navigationLevel.size).isEqualTo(0)
            }
        }
}
