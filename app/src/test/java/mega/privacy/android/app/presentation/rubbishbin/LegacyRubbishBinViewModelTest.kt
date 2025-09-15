package mega.privacy.android.app.presentation.rubbishbin

import app.cash.turbine.test
import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.presentation.data.NodeUIItem
import mega.privacy.android.app.presentation.rubbishbin.model.RestoreType
import mega.privacy.android.core.formatter.mapper.DurationInSecondsTextMapper
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.data.mapper.FileDurationMapper
import mega.privacy.android.domain.entity.AccountSubscriptionCycle
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.account.AccountDetail
import mega.privacy.android.domain.entity.account.AccountLevelDetail
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeUpdate
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.domain.usecase.GetBusinessStatusUseCase
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import mega.privacy.android.domain.usecase.GetParentNodeUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.node.IsNodeDeletedFromBackupsUseCase
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.rubbishbin.GetRubbishBinFolderUseCase
import mega.privacy.android.domain.usecase.rubbishbin.GetRubbishBinNodeChildrenUseCase
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
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.time.Duration.Companion.seconds

@ExtendWith(CoroutineMainDispatcherExtension::class)
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LegacyRubbishBinViewModelTest {

    private lateinit var underTest: LegacyRubbishBinViewModel

    private val monitorNodeUpdatesFakeFlow = MutableSharedFlow<NodeUpdate>()
    private val monitorNodeUpdatesUseCase = mock<MonitorNodeUpdatesUseCase>()
    private val getParentNodeUseCase = mock<GetParentNodeUseCase>()
    private val isNodeDeletedFromBackupsUseCase = mock<IsNodeDeletedFromBackupsUseCase>()
    private val setViewType = mock<SetViewType>()
    private val monitorViewType = mock<MonitorViewType>()
    private val getCloudSortOrder = mock<GetCloudSortOrder>()
    private val getRubbishBinFolderUseCase = mock<GetRubbishBinFolderUseCase>()
    private val getRubbishBinNodeChildrenUseCase = mock<GetRubbishBinNodeChildrenUseCase>()
    private val fileDurationMapper: FileDurationMapper = mock()
    private val durationInSecondsTextMapper = mock<DurationInSecondsTextMapper>()
    private val monitorAccountDetailUseCase = mock<MonitorAccountDetailUseCase>()
    private val accountDetailFakeFlow = MutableSharedFlow<AccountDetail>()
    private val getBusinessStatusUseCase = mock<GetBusinessStatusUseCase>()
    private val getFeatureFlagValueUseCase = mock<GetFeatureFlagValueUseCase>()

    @BeforeEach
    fun setUp() {
        runBlocking {
            stubCommon()
        }
        initViewModel()
    }

    private fun initViewModel() {
        underTest = LegacyRubbishBinViewModel(
            monitorNodeUpdatesUseCase = monitorNodeUpdatesUseCase,
            getParentNodeUseCase = getParentNodeUseCase,
            getRubbishBinNodeChildrenUseCase = getRubbishBinNodeChildrenUseCase,
            isNodeDeletedFromBackupsUseCase = isNodeDeletedFromBackupsUseCase,
            setViewType = setViewType,
            monitorViewType = monitorViewType,
            getCloudSortOrder = getCloudSortOrder,
            getRubbishBinFolderUseCase = getRubbishBinFolderUseCase,
            fileDurationMapper = fileDurationMapper,
            durationInSecondsTextMapper = durationInSecondsTextMapper,
            monitorAccountDetailUseCase = monitorAccountDetailUseCase,
            getBusinessStatusUseCase = getBusinessStatusUseCase,
            getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
        )
    }

    @Test
    fun `test that initial state is returned`() = runTest {
        underTest.state.test {
            val initial = awaitItem()
            Truth.assertThat(initial.currentHandle).isEqualTo(-1L)
            Truth.assertThat(initial.parentHandle).isNull()
            Truth.assertThat(initial.nodeList).isEmpty()
            Truth.assertThat(initial.selectedFileNodes).isEqualTo(0)
            Truth.assertThat(initial.selectedFolderNodes).isEqualTo(0)
            Truth.assertThat(initial.isInSelection).isFalse()
            Truth.assertThat(initial.currentViewType).isEqualTo(ViewType.LIST)
            Truth.assertThat(initial.sortOrder).isEqualTo(SortOrder.ORDER_NONE)
            Truth.assertThat(initial.isPendingRefresh).isFalse()
            Truth.assertThat(initial.restoreType).isNull()
            Truth.assertThat(initial.accountType).isNull()
            Truth.assertThat(initial.isBusinessAccountExpired).isFalse()
            Truth.assertThat(initial.hiddenNodeEnabled).isFalse()
        }
    }

    @Test
    fun `test that rubbish bin handle is updated if new value provided`() = runTest {
        underTest.state.map { it.currentHandle }.distinctUntilChanged()
            .test {
                val newValue = 123456789L
                Truth.assertThat(awaitItem()).isEqualTo(-1L)
                underTest.setRubbishBinHandle(newValue)
                Truth.assertThat(awaitItem()).isEqualTo(newValue)
            }
    }

    @Test
    fun `test that on setting rubbish bin handle rubbish bin node returns empty list`() =
        runTest {
            val newValue = 123456789L
            whenever(getRubbishBinNodeChildrenUseCase.invoke(newValue)).thenReturn(ArrayList())
            monitorNodeUpdatesFakeFlow.emit(NodeUpdate(emptyMap()))
            underTest.setRubbishBinHandle(newValue)
            Truth.assertThat(underTest.state.value.nodeList.size).isEqualTo(0)
        }

    @Test
    fun `test that on setting rubbish bin handle rubbish bin node returns null`() = runTest {
        val newValue = 123456789L
        whenever(getRubbishBinNodeChildrenUseCase(newValue)).thenReturn(emptyList())
        underTest.setRubbishBinHandle(newValue)
        Truth.assertThat(underTest.state.value.nodeList.size).isEqualTo(0)
        verify(getRubbishBinNodeChildrenUseCase, times(1)).invoke(newValue)
    }

    @Test
    fun `test that when handle on back pressed and parent handle is null, then getRubbishBinChildrenNode is not invoked`() =
        runTest {
            val newValue = 123456789L
            underTest.onBackPressed()
            verify(getRubbishBinNodeChildrenUseCase, times(0)).invoke(newValue)
        }

    @Test
    fun `test that when handle on back pressed and parent handle is not null, then getRubbishBinChildrenNode is invoked once`() =
        runTest {
            val newValue = 123456789L
            // to update handles rubbishBinHandle
            whenever(getRubbishBinNodeChildrenUseCase(newValue)).thenReturn(emptyList())
            underTest.setRubbishBinHandle(newValue)
            underTest.onBackPressed()
            verify(getRubbishBinNodeChildrenUseCase, times(1)).invoke(newValue)
        }

    @Test
    fun `test that item is popped from openedFolderNodeHandles when back pressed`() =
        runTest {
            val newValue = 123456789L
            val parentHandle = 987654321L
            whenever(getRubbishBinNodeChildrenUseCase(newValue)).thenReturn(emptyList())
            val mockParentNode = mock<FolderNode>().apply {
                whenever(id).thenReturn(NodeId(parentHandle))
            }
            whenever(getParentNodeUseCase(NodeId(newValue))).thenReturn(mockParentNode)
            underTest.setRubbishBinHandle(newValue)
            underTest.onBackPressed()
            underTest.state.test {
                val state = awaitItem()
                assertThat(state.openedFolderNodeHandles).containsExactly(-1L)
            }
        }

    @Test
    fun `test that currentNodeHandle is replaced with -1L if matched with rubbish bin folder handle`() =
        runTest {
            val newValue = 123456789L
            val rubbishBinHandle = 987654321L

            val mockRubbishBinFolder = mock<FolderNode>()
            whenever(mockRubbishBinFolder.id.longValue).thenReturn(rubbishBinHandle)
            whenever(getRubbishBinFolderUseCase()).thenReturn(mockRubbishBinFolder)

            // Re-initialize the ViewModel with the new mock
            initViewModel()

            whenever(getRubbishBinNodeChildrenUseCase(newValue)).thenReturn(emptyList())
            whenever(getRubbishBinNodeChildrenUseCase(-1L)).thenReturn(emptyList())

            val mockParentNode = mock<FolderNode>()
            whenever(mockParentNode.id).thenReturn(NodeId(rubbishBinHandle))
            whenever(getParentNodeUseCase(NodeId(newValue))).thenReturn(mockParentNode)

            underTest.setRubbishBinHandle(newValue)
            underTest.onBackPressed()

            underTest.state.test {
                val state = awaitItem()
                assertThat(state.currentHandle).isEqualTo(-1L)
            }
        }

    @Test
    fun `test when when nodeUIItem is long clicked, then it updates selected item by 1`() =
        runTest {
            val nodesListItem1 = mock<TypedFolderNode>()
            val nodesListItem2 = mock<TypedFileNode>()
            whenever(nodesListItem1.id.longValue).thenReturn(1L)
            whenever(nodesListItem2.id.longValue).thenReturn(2L)
            whenever(getRubbishBinNodeChildrenUseCase(underTest.state.value.currentHandle)).thenReturn(
                listOf(nodesListItem1, nodesListItem2)
            )
            whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_NONE)
            underTest.refreshNodes()
            underTest.onLongItemClicked(
                NodeUIItem(
                    nodesListItem1,
                    isSelected = false,
                    isInvisible = false
                )
            )
            underTest.state.test {
                val state = awaitItem()
                Truth.assertThat(state.selectedFolderNodes).isEqualTo(1)
                Truth.assertThat(state.selectedFileNodes).isEqualTo(0)
                Truth.assertThat(state.selectedNodes.size).isEqualTo(1)
            }
        }

    @Test
    fun `test that when item is clicked and some items are already selected on list then checked index gets decremented by 1`() =
        runTest {
            val nodesListItem1 = mock<TypedFolderNode>()
            val nodesListItem2 = mock<TypedFileNode>()
            whenever(nodesListItem1.id.longValue).thenReturn(1L)
            whenever(nodesListItem2.id.longValue).thenReturn(2L)
            whenever(getRubbishBinNodeChildrenUseCase(underTest.state.value.currentHandle)).thenReturn(
                listOf(nodesListItem1, nodesListItem2)
            )
            whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_NONE)

            underTest.refreshNodes()
            underTest.onLongItemClicked(
                NodeUIItem(
                    nodesListItem1,
                    isSelected = false,
                    isInvisible = false
                )
            )
            underTest.onItemClicked(
                NodeUIItem(
                    nodesListItem1,
                    isSelected = true,
                    isInvisible = false
                )
            )
            underTest.state.test {
                val state = awaitItem()
                Truth.assertThat(state.selectedFolderNodes).isEqualTo(0)
                Truth.assertThat(state.selectedFileNodes).isEqualTo(0)
                Truth.assertThat(state.selectedNodes.size).isEqualTo(0)
            }
        }

    @Test
    fun `test that when selected item gets clicked then checked index gets incremented by 1`() =
        runTest {
            val nodesListItem1 = mock<TypedFileNode>()
            val nodesListItem2 = mock<TypedFolderNode>()
            whenever(nodesListItem1.id.longValue).thenReturn(1L)
            whenever(nodesListItem2.id.longValue).thenReturn(2L)
            whenever(getRubbishBinNodeChildrenUseCase(underTest.state.value.currentHandle)).thenReturn(
                listOf(nodesListItem1, nodesListItem2)
            )
            whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_NONE)

            underTest.refreshNodes()
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
                Truth.assertThat(state.selectedFolderNodes).isEqualTo(1)
                Truth.assertThat(state.selectedFileNodes).isEqualTo(1)
                Truth.assertThat(state.selectedNodes.size).isEqualTo(2)
            }
        }

    @Test
    fun `test that on clicking on change view type to Grid it calls setViewType atleast once`() =
        runTest {
            underTest.onChangeViewTypeClicked()
            verify(setViewType, times(1)).invoke(ViewType.GRID)
        }

    @Test
    fun `test when user selects all nodes then sum of selected items is equal to size of list`() =
        runTest {
            val nodesListItem1 = mock<TypedFolderNode>()
            val nodesListItem2 = mock<TypedFileNode>()
            whenever(nodesListItem1.id.longValue).thenReturn(1L)
            whenever(nodesListItem2.id.longValue).thenReturn(2L)
            whenever(getRubbishBinNodeChildrenUseCase(underTest.state.value.currentHandle)).thenReturn(
                listOf(nodesListItem1, nodesListItem2)
            )
            whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_NONE)

            underTest.refreshNodes()
            underTest.selectAllNodes()
            val totalSelectedNodes =
                underTest.state.value.selectedFileNodes + underTest.state.value.selectedFileNodes
            Truth.assertThat(totalSelectedNodes).isEqualTo(underTest.state.value.nodeList.size)
            Truth.assertThat(totalSelectedNodes)
                .isEqualTo(underTest.state.value.selectedNodes.size)
            Truth.assertThat(underTest.state.value.isInSelection).isTrue()
        }

    @Test
    fun `test when user clears all selected nodes then sum of selected items is 0`() =
        runTest {
            val nodesListItem1 = mock<TypedFolderNode>()
            val nodesListItem2 = mock<TypedFileNode>()
            whenever(nodesListItem1.id.longValue).thenReturn(1L)
            whenever(nodesListItem2.id.longValue).thenReturn(2L)
            whenever(getRubbishBinNodeChildrenUseCase(underTest.state.value.currentHandle)).thenReturn(
                listOf(nodesListItem1, nodesListItem2)
            )
            whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_NONE)

            underTest.refreshNodes()
            underTest.clearAllSelectedNodes()
            val totalSelectedNodes =
                underTest.state.value.selectedFileNodes + underTest.state.value.selectedFileNodes
            Truth.assertThat(totalSelectedNodes).isEqualTo(0)
            Truth.assertThat(totalSelectedNodes)
                .isEqualTo(0)
            Truth.assertThat(underTest.state.value.isInSelection).isFalse()
            Truth.assertThat(underTest.state.value.selectedNodes).isEmpty()
        }

    @Test
    fun `test that when folder is selected it calls update handle`() = runTest {
        val handle = 123456L
        underTest.onFolderItemClicked(handle)
        verify(getRubbishBinNodeChildrenUseCase).invoke(handle)
    }

    @Test
    fun `test that restoring nodes will execute the move functionality when backup nodes are selected`() =
        runTest {
            val nodesListItem1 = mock<TypedFolderNode>()
            val nodesListItem2 = mock<TypedFileNode>()
            whenever(nodesListItem1.id.longValue).thenReturn(1L)
            whenever(nodesListItem2.id.longValue).thenReturn(2L)

            whenever(getRubbishBinNodeChildrenUseCase(underTest.state.value.currentHandle)).thenReturn(
                listOf(nodesListItem1, nodesListItem2)
            )
            whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_NONE)
            whenever(isNodeDeletedFromBackupsUseCase(NodeId(any()))).thenReturn(true)


            underTest.refreshNodes()
            underTest.selectAllNodes()
            underTest.onRestoreClicked()

            underTest.state.test {
                Truth.assertThat(awaitItem().restoreType).isEqualTo(RestoreType.MOVE)
            }
        }

    @Test
    fun `test that restoring nodes will execute the move functionality when backup and non backup nodes are selected`() =
        runTest {
            val nodesListItem1 = mock<TypedFolderNode>()
            val nodesListItem2 = mock<TypedFileNode>()
            whenever(nodesListItem1.id.longValue).thenReturn(1L)
            whenever(nodesListItem2.id.longValue).thenReturn(2L)

            whenever(getRubbishBinNodeChildrenUseCase(underTest.state.value.currentHandle)).thenReturn(
                listOf(nodesListItem1, nodesListItem2)
            )
            whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_NONE)
            whenever(isNodeDeletedFromBackupsUseCase(NodeId(1L))).thenReturn(true)
            whenever(isNodeDeletedFromBackupsUseCase(NodeId(2L))).thenReturn(false)

            underTest.refreshNodes()
            underTest.selectAllNodes()
            underTest.onRestoreClicked()

            underTest.state.test {
                Truth.assertThat(awaitItem().restoreType).isEqualTo(RestoreType.MOVE)
            }
        }

    @Test
    fun `test that restoring nodes will execute the restore functionality when non backup nodes are selected`() =
        runTest {
            val nodesListItem1 = mock<TypedFolderNode>()
            val nodesListItem2 = mock<TypedFileNode>()
            whenever(nodesListItem1.id.longValue).thenReturn(1L)
            whenever(nodesListItem2.id.longValue).thenReturn(2L)
            whenever(getRubbishBinNodeChildrenUseCase(underTest.state.value.currentHandle)).thenReturn(
                listOf(nodesListItem1, nodesListItem2)
            )
            whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_NONE)
            whenever(isNodeDeletedFromBackupsUseCase(NodeId(any()))).thenReturn(false)

            underTest.refreshNodes()
            underTest.selectAllNodes()
            underTest.onRestoreClicked()

            underTest.state.test {
                Truth.assertThat(awaitItem().restoreType).isEqualTo(RestoreType.RESTORE)
            }
        }

    @Test
    fun `test that acknowledging the restore functionality will reset the restore type`() =
        runTest {
            underTest.onRestoreHandled()
            underTest.state.test {
                Truth.assertThat(awaitItem().restoreType).isNull()
            }
        }

    @Test
    fun `test that account type is updated when monitorAccountDetailUseCase emits`() = runTest {
        stubCommon()
        whenever(getFeatureFlagValueUseCase(ApiFeatures.HiddenNodesInternalRelease)).thenReturn(true)
        initViewModel()
        val newAccountDetail = AccountDetail(
            levelDetail = AccountLevelDetail(
                accountType = AccountType.PRO_I,
                subscriptionStatus = null,
                subscriptionRenewTime = 0,
                accountSubscriptionCycle = AccountSubscriptionCycle.YEARLY,
                proExpirationTime = 0L,
                accountPlanDetail = null,
                accountSubscriptionDetailList = listOf(),
            )
        )
        accountDetailFakeFlow.emit(newAccountDetail)

        underTest.state.test {
            Truth.assertThat(awaitItem().accountType)
                .isEqualTo(newAccountDetail.levelDetail?.accountType)
        }
    }

    @Test
    fun `test that resetScrollPosition sets resetScrollPositionEvent to triggered`() = runTest {
        underTest.resetScrollPosition()
        underTest.state.test {
            Truth.assertThat(awaitItem().resetScrollPositionEvent).isEqualTo(triggered)
        }
    }

    @Test
    fun `test that onResetScrollPositionEventConsumed sets resetScrollPositionEvent to consumed`() =
        runTest {
            underTest.onResetScrollPositionEventConsumed()
            underTest.state.test {
                Truth.assertThat(awaitItem().resetScrollPositionEvent).isEqualTo(consumed)
            }
        }

    private suspend fun stubCommon() {
        whenever(monitorNodeUpdatesUseCase()).thenReturn(monitorNodeUpdatesFakeFlow)
        whenever(monitorViewType()).thenReturn(emptyFlow())
        whenever(getRubbishBinNodeChildrenUseCase(any())).thenReturn(emptyList())
        whenever(getParentNodeUseCase(NodeId(any()))).thenReturn(null)
        whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_NONE)
        whenever(getRubbishBinFolderUseCase()).thenReturn(null)
        whenever(fileDurationMapper(any())).thenReturn(1.seconds)
        whenever(monitorAccountDetailUseCase()).thenReturn(accountDetailFakeFlow)
        whenever(getFeatureFlagValueUseCase(any())).thenReturn(false)
    }

    @AfterEach
    fun resetMocks() {
        reset(
            monitorNodeUpdatesUseCase,
            getParentNodeUseCase,
            isNodeDeletedFromBackupsUseCase,
            setViewType,
            monitorViewType,
            getCloudSortOrder,
            getRubbishBinFolderUseCase,
            getRubbishBinNodeChildrenUseCase,
            fileDurationMapper
        )
    }
}
