@file:OptIn(ExperimentalCoroutinesApi::class)

package mega.privacy.android.app.presentation.documentsection

import androidx.annotation.DrawableRes
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.TimberJUnit5Extension
import mega.privacy.android.app.domain.usecase.GetNodeByHandle
import mega.privacy.android.app.presentation.documentsection.model.DocumentUiEntity
import mega.privacy.android.app.presentation.documentsection.model.DocumentUiEntityMapper
import mega.privacy.android.app.presentation.validator.toolbaractions.model.modifier.DocumentSectionHiddenNodeActionModifierItem
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.AccountSubscriptionCycle
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.FileTypeInfo
import mega.privacy.android.domain.entity.Offline
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.UnknownFileTypeInfo
import mega.privacy.android.domain.entity.account.AccountDetail
import mega.privacy.android.domain.entity.account.AccountLevelDetail
import mega.privacy.android.domain.entity.account.business.BusinessAccountStatus
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeUpdate
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.usecase.CheckNodeCanBeMovedToTargetNode
import mega.privacy.android.domain.usecase.GetBusinessStatusUseCase
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import mega.privacy.android.domain.usecase.GetFileUrlByNodeHandleUseCase
import mega.privacy.android.domain.usecase.IsHiddenNodesOnboardedUseCase
import mega.privacy.android.domain.usecase.UpdateNodeSensitiveUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.documentsection.GetAllDocumentsUseCase
import mega.privacy.android.domain.usecase.file.GetFingerprintUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiHttpServerIsRunningUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiHttpServerStartUseCase
import mega.privacy.android.domain.usecase.mediaplayer.videoplayer.GetNodeAccessUseCase
import mega.privacy.android.domain.usecase.network.IsConnectedToInternetUseCase
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.offline.MonitorOfflineNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.rubbishbin.GetRubbishBinFolderUseCase
import mega.privacy.android.domain.usecase.setting.MonitorShowHiddenItemsUseCase
import mega.privacy.android.domain.usecase.viewtype.MonitorViewType
import mega.privacy.android.domain.usecase.viewtype.SetViewType
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.mockito.kotlin.wheneverBlocking
import java.io.File
import java.util.stream.Stream

@ExtendWith(CoroutineMainDispatcherExtension::class)
@ExtendWith(TimberJUnit5Extension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DocumentSectionViewModelTest {
    private lateinit var underTest: DocumentSectionViewModel

    private val getAllDocumentsUseCase = mock<GetAllDocumentsUseCase>()
    private val documentUiEntityMapper = mock<DocumentUiEntityMapper>()
    private val getCloudSortOrder = mock<GetCloudSortOrder>()
    private val monitorNodeUpdatesUseCase = mock<MonitorNodeUpdatesUseCase>()
    private val monitorOfflineNodeUpdatesUseCase = mock<MonitorOfflineNodeUpdatesUseCase>()
    private val monitorViewType = mock<MonitorViewType>()
    private val fakeMonitorNodeUpdatesFlow = MutableSharedFlow<NodeUpdate>()
    private val fakeMonitorOfflineNodeUpdatesFlow = MutableSharedFlow<List<Offline>>()
    private val fakeMonitorViewTypeFlow = MutableSharedFlow<ViewType>()
    private val setViewType = mock<SetViewType>()
    private val getNodeByHandle = mock<GetNodeByHandle>()
    private val getFingerprintUseCase = mock<GetFingerprintUseCase>()
    private val megaApiHttpServerIsRunningUseCase = mock<MegaApiHttpServerIsRunningUseCase>()
    private val megaApiHttpServerStartUseCase = mock<MegaApiHttpServerStartUseCase>()
    private val getFileUrlByNodeHandleUseCase = mock<GetFileUrlByNodeHandleUseCase>()
    private val isConnectedToInternetUseCase = mock<IsConnectedToInternetUseCase>()
    private val updateNodeSensitiveUseCase = mock<UpdateNodeSensitiveUseCase>()
    private val monitorShowHiddenItemsUseCase = mock<MonitorShowHiddenItemsUseCase>()

    private val monitorAccountDetailUseCase = mock<MonitorAccountDetailUseCase>()
    private var accountDetailFlow = MutableSharedFlow<AccountDetail>()
    private val isHiddenNodesOnboardedUseCase = mock<IsHiddenNodesOnboardedUseCase> {
        onBlocking {
            invoke()
        }.thenReturn(false)
    }

    private val firstDefaultId = NodeId(123L)
    private val firstDefaultDocumentFileNode = mock<TypedFileNode> {
        on { id } doReturn firstDefaultId
        on { isIncomingShare } doReturn false
    }
    private val expectedFirstDocumentUiEntity = newDocumentUiEntity(
        id = firstDefaultId
    )
    private val secondDefaultId = NodeId(312L)
    private val secondDefaultDocumentFileNode = mock<TypedFileNode> {
        on { id } doReturn secondDefaultId
        on { isIncomingShare } doReturn false
    }
    private val expectedSecondDocumentUiEntity = newDocumentUiEntity(
        id = secondDefaultId
    )
    private val getBusinessStatusUseCase = mock<GetBusinessStatusUseCase>()
    private val checkNodeCanBeMovedToTargetNode = mock<CheckNodeCanBeMovedToTargetNode>()
    private val getRubbishBinFolderUseCase = mock<GetRubbishBinFolderUseCase>()
    private val getNodeAccessUseCase = mock<GetNodeAccessUseCase>()
    private var showHiddenItemsFlow = MutableSharedFlow<Boolean>()

    private val accountLevelDetail = mock<AccountLevelDetail> {
        on { accountType }.thenReturn(AccountType.PRO_III)
    }
    private val accountDetail = mock<AccountDetail> {
        on { levelDetail }.thenReturn(accountLevelDetail)
    }

    @BeforeEach
    fun setUp() {
        wheneverBlocking { monitorNodeUpdatesUseCase() }.thenReturn(fakeMonitorNodeUpdatesFlow)
        wheneverBlocking { monitorOfflineNodeUpdatesUseCase() }.thenReturn(
            fakeMonitorOfflineNodeUpdatesFlow
        )
        wheneverBlocking { monitorViewType() }.thenReturn(fakeMonitorViewTypeFlow)
        wheneverBlocking { getCloudSortOrder() }.thenReturn(SortOrder.ORDER_NONE)
        wheneverBlocking { monitorAccountDetailUseCase() }.thenReturn(accountDetailFlow)
        wheneverBlocking { monitorShowHiddenItemsUseCase() }.thenReturn(showHiddenItemsFlow)
        initUnderTest()
    }

    private fun initUnderTest() {
        underTest = DocumentSectionViewModel(
            getAllDocumentsUseCase = getAllDocumentsUseCase,
            documentUiEntityMapper = documentUiEntityMapper,
            getCloudSortOrder = getCloudSortOrder,
            monitorNodeUpdatesUseCase = monitorNodeUpdatesUseCase,
            monitorOfflineNodeUpdatesUseCase = monitorOfflineNodeUpdatesUseCase,
            monitorViewType = monitorViewType,
            setViewType = setViewType,
            getNodeByHandle = getNodeByHandle,
            getFingerprintUseCase = getFingerprintUseCase,
            megaApiHttpServerIsRunningUseCase = megaApiHttpServerIsRunningUseCase,
            megaApiHttpServerStartUseCase = megaApiHttpServerStartUseCase,
            getFileUrlByNodeHandleUseCase = getFileUrlByNodeHandleUseCase,
            isConnectedToInternetUseCase = isConnectedToInternetUseCase,
            updateNodeSensitiveUseCase = updateNodeSensitiveUseCase,
            monitorAccountDetailUseCase = monitorAccountDetailUseCase,
            isHiddenNodesOnboardedUseCase = isHiddenNodesOnboardedUseCase,
            monitorShowHiddenItemsUseCase = monitorShowHiddenItemsUseCase,
            defaultDispatcher = UnconfinedTestDispatcher(),
            getBusinessStatusUseCase = getBusinessStatusUseCase,
            checkNodeCanBeMovedToTargetNode = checkNodeCanBeMovedToTargetNode,
            getRubbishBinFolderUseCase = getRubbishBinFolderUseCase,
            getNodeAccessUseCase = getNodeAccessUseCase
        )
    }

    @AfterEach
    fun resetMocks() {
        accountDetailFlow = MutableSharedFlow()
        showHiddenItemsFlow = MutableSharedFlow()
        reset(
            getAllDocumentsUseCase,
            documentUiEntityMapper,
            getCloudSortOrder,
            monitorNodeUpdatesUseCase,
            monitorOfflineNodeUpdatesUseCase,
            monitorViewType,
            setViewType,
            getNodeByHandle,
            getFingerprintUseCase,
            megaApiHttpServerIsRunningUseCase,
            megaApiHttpServerStartUseCase,
            getFileUrlByNodeHandleUseCase,
            isConnectedToInternetUseCase,
            checkNodeCanBeMovedToTargetNode,
            getRubbishBinFolderUseCase,
            getNodeAccessUseCase
        )
    }

    @Test
    fun `test that the initial state is returned`() = runTest {
        underTest.uiState.test {
            val initial = awaitItem()
            assertThat(initial.allDocuments).isEmpty()
            assertThat(initial.sortOrder).isEqualTo(SortOrder.ORDER_NONE)
            assertThat(initial.isLoading).isEqualTo(true)
            assertThat(initial.scrollToTop).isEqualTo(false)
            assertThat(initial.currentViewType).isEqualTo(ViewType.LIST)
            assertThat(initial.actionMode).isFalse()
            assertThat(initial.selectedNodes).isEmpty()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that the audios are retrieved after the nodes are refreshed`() = runTest {
        initDocumentNodeListReturned()

        initUnderTest()
        underTest.refreshDocumentNodes()

        underTest.uiState.test {
            val actual = awaitItem()
            assertThat(actual.sortOrder).isEqualTo(SortOrder.ORDER_MODIFICATION_DESC)
            assertThat(actual.allDocuments.size).isEqualTo(2)
            assertThat(actual.isLoading).isEqualTo(false)
            assertThat(actual.scrollToTop).isEqualTo(false)
            cancelAndIgnoreRemainingEvents()
        }
    }

    private suspend fun initDocumentNodeListReturned() {
        whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_MODIFICATION_DESC)
        whenever(getRubbishBinFolderUseCase()) doReturn null
        whenever(getAllDocumentsUseCase()).thenReturn(
            listOf(
                firstDefaultDocumentFileNode,
                secondDefaultDocumentFileNode
            )
        )
        whenever(
            documentUiEntityMapper(
                eq(firstDefaultDocumentFileNode),
                any(),
                any()
            )
        ).thenReturn(expectedFirstDocumentUiEntity)
        whenever(
            documentUiEntityMapper(
                eq(secondDefaultDocumentFileNode),
                any(),
                any()
            )
        ).thenReturn(expectedSecondDocumentUiEntity)
        whenever(
            getNodeAccessUseCase(nodeId = firstDefaultId)
        ) doReturn AccessPermission.UNKNOWN
        whenever(
            getNodeAccessUseCase(nodeId = secondDefaultId)
        ) doReturn AccessPermission.UNKNOWN
    }

    @Test
    fun `test that the currentViewType is correctly updated when monitorViewType is triggered`() =
        runTest {
            underTest.uiState.drop(1).test {
                fakeMonitorViewTypeFlow.emit(ViewType.GRID)
                assertThat(awaitItem().currentViewType).isEqualTo(ViewType.GRID)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that the state is correctly updated when monitorNodeUpdatesUseCase is triggered`() =
        runTest {
            initDocumentNodeListReturned()

            initUnderTest()
            accountDetailFlow.emit(accountDetail)
            showHiddenItemsFlow.emit(false)
            fakeMonitorNodeUpdatesFlow.emit(NodeUpdate(emptyMap()))
            advanceUntilIdle()

            underTest.uiState.test {
                val actual = awaitItem()
                assertThat(actual.sortOrder).isEqualTo(SortOrder.ORDER_MODIFICATION_DESC)
                assertThat(actual.allDocuments.size).isEqualTo(2)
                assertThat(actual.isLoading).isEqualTo(false)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that the state is correctly updated when monitorOfflineNodeUpdatesUseCase is triggered`() =
        runTest {
            initDocumentNodeListReturned()

            initUnderTest()
            accountDetailFlow.emit(accountDetail)
            showHiddenItemsFlow.emit(false)
            fakeMonitorOfflineNodeUpdatesFlow.emit(emptyList())
            advanceUntilIdle()

            underTest.uiState.test {
                val actual = awaitItem()
                assertThat(actual.sortOrder).isEqualTo(SortOrder.ORDER_MODIFICATION_DESC)
                assertThat(actual.allDocuments.size).isEqualTo(2)
                assertThat(actual.isLoading).isEqualTo(false)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that the setViewType is invoked when onChangeViewTypeClicked is triggered and currentViewType is List`() =
        runTest {
            underTest.onChangeViewTypeClicked()
            verify(setViewType).invoke(ViewType.GRID)
        }

    @Test
    fun `test that the setViewType is invoked when onChangeViewTypeClicked is triggered and currentViewType is Grid`() =
        runTest {
            underTest.uiState.drop(1).test {
                fakeMonitorViewTypeFlow.emit(ViewType.GRID)
                assertThat(awaitItem().currentViewType).isEqualTo(ViewType.GRID)
                underTest.onChangeViewTypeClicked()
                verify(setViewType).invoke(ViewType.LIST)
            }
        }

    @Test
    fun `test that the uiState is correctly updated when sort order is changed`() = runTest {
        val order = SortOrder.ORDER_DEFAULT_DESC
        whenever(getCloudSortOrder()).thenReturn(order)

        underTest.uiState.test {
            assertThat(awaitItem().sortOrder).isEqualTo(SortOrder.ORDER_NONE)
            underTest.refreshWhenOrderChanged()
            val actual = awaitItem()
            assertThat(actual.sortOrder).isEqualTo(order)
            assertThat(actual.isLoading).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that getLocalFilePath return null when getNodeByHandle return null`() = runTest {
        whenever(getNodeByHandle(any())).thenReturn(null)
        assertThat(underTest.getLocalFilePath(1)).isNull()
    }

    @Test
    fun `test that getDocumentNodeByHandle return not null`() = runTest {
        whenever(getNodeByHandle(any())).thenReturn(mock())
        assertThat(underTest.getDocumentNodeByHandle(1)).isNotNull()
    }

    @Test
    fun `test that getDocumentNodeByHandle return null`() = runTest {
        whenever(getNodeByHandle(any())).thenReturn(null)
        assertThat(underTest.getDocumentNodeByHandle(1)).isNull()
    }

    private val nodeList = (0..2).map {
        getTypedFileNode(NodeId((it).toLong()))
    }
    private val documentList = nodeList.map {
        newDocumentUiEntity(it.id)
    }

    @Test
    fun `test that the selected item is updated by 1 when long clicked`() = runTest {
        initDocumentReturnWithSpecificDocument()
        initUnderTest()

        underTest.refreshDocumentNodes()
        underTest.onItemSelected(documentList[0], 0)

        underTest.uiState.test {
            val actual = awaitItem()
            assertThat(actual.selectedNodes.size).isEqualTo(1)
            assertThat(actual.actionMode).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    private suspend fun initDocumentReturnWithSpecificDocument() {
        whenever(getAllDocumentsUseCase()).thenReturn(nodeList)
        whenever(
            documentUiEntityMapper(
                typedFileNode = nodeList[0],
                accessPermission = AccessPermission.UNKNOWN,
                canBeMovedToRubbishBin = false
            )
        ).thenReturn(documentList[0])
        whenever(
            documentUiEntityMapper(
                typedFileNode = nodeList[1],
                accessPermission = AccessPermission.UNKNOWN,
                canBeMovedToRubbishBin = false
            )
        ).thenReturn(documentList[1])
        whenever(
            documentUiEntityMapper(
                typedFileNode = nodeList[2],
                accessPermission = AccessPermission.UNKNOWN,
                canBeMovedToRubbishBin = false
            )
        ).thenReturn(documentList[2])
        whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_MODIFICATION_DESC)
    }

    private fun getTypedFileNode(nodeId: NodeId) = mock<TypedFileNode> {
        on { id }.thenReturn(nodeId)
        on { name }.thenReturn("name")
    }

    @Test
    fun `test that the checked index is incremented by 1 when the other item gets clicked`() =
        runTest {
            initDocumentReturnWithSpecificDocument()
            initUnderTest()

            underTest.refreshDocumentNodes()
            underTest.onItemSelected(documentList[0], 0)

            underTest.uiState.test {
                awaitItem().let {
                    assertThat(it.selectedNodes.size).isEqualTo(1)
                    assertThat(it.actionMode).isTrue()
                }
                underTest.onItemSelected(documentList[1], 1)
                assertThat(awaitItem().selectedNodes.size).isEqualTo(2)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that uiState is correctly updated after selectAllNodes is invoked`() = runTest {
        initDocumentReturnWithSpecificDocument()
        initUnderTest()

        underTest.refreshDocumentNodes()
        underTest.selectAllNodes()

        underTest.uiState.test {
            val actual = awaitItem()
            assertThat(actual.allDocuments.size).isEqualTo(3)
            assertThat(actual.selectedNodes.size).isEqualTo(3)
            assertThat(actual.actionMode).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that uiState is correctly updated after clearAllSelectedDocuments is invoked`() =
        runTest {
            initDocumentReturnWithSpecificDocument()
            initUnderTest()

            underTest.refreshDocumentNodes()
            underTest.clearAllSelectedDocuments()

            underTest.uiState.test {
                awaitItem().let {
                    assertThat(it.allDocuments.size).isEqualTo(3)
                    assertThat(it.selectedNodes).isEmpty()
                    assertThat(it.actionMode).isFalse()
                }
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that getSelectedMegaNode returns correctly by the item is clicked`() = runTest {
        initDocumentReturnWithSpecificDocument()
        whenever(getNodeByHandle(any())).thenReturn(mock())
        initUnderTest()

        underTest.refreshDocumentNodes()
        underTest.onItemSelected(documentList[0], 0)

        underTest.uiState.test {
            val actual = awaitItem()
            assertThat(actual.selectedNodes.size).isEqualTo(1)
            assertThat(actual.actionMode).isTrue()
            assertThat(underTest.getSelectedMegaNode().size).isEqualTo(1)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that actionMode is correctly updated`() = runTest {
        initUnderTest()

        underTest.uiState.test {
            assertThat(awaitItem().actionMode).isFalse()
            underTest.setActionMode(true)
            assertThat(awaitItem().actionMode).isTrue()
            underTest.setActionMode(false)
            assertThat(awaitItem().actionMode).isFalse()
        }
    }

    @ParameterizedTest
    @EnumSource(AccessPermission::class)
    fun `test that the correct access permission for an account is set`(
        accessPermission: AccessPermission,
    ) = runTest {
        whenever(getCloudSortOrder()) doReturn SortOrder.ORDER_MODIFICATION_DESC
        whenever(getAllDocumentsUseCase()) doReturn listOf(firstDefaultDocumentFileNode)
        whenever(
            documentUiEntityMapper(
                any(),
                any(),
                any()
            )
        ) doReturn expectedFirstDocumentUiEntity.copy(accessPermission = accessPermission)
        whenever(getRubbishBinFolderUseCase()) doReturn null
        whenever(
            getNodeAccessUseCase(nodeId = firstDefaultId)
        ) doReturn accessPermission

        underTest.refreshDocumentNodes()

        underTest.uiState.test {
            assertThat(
                expectMostRecentItem().allDocuments.first().accessPermission
            ).isEqualTo(accessPermission)
        }
    }

    private fun provideNodeAccessPermissions() = Stream.of(
        Arguments.of(true, true),
        Arguments.of(false, false),
        Arguments.of(true, false),
        Arguments.of(false, true),
    )

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that the correct value is set indicating whether the node can be moved to the rubbish bin`(
        canBeMovedToRubbishBin: Boolean,
    ) = runTest {
        whenever(getCloudSortOrder()) doReturn SortOrder.ORDER_MODIFICATION_DESC
        whenever(getAllDocumentsUseCase()) doReturn listOf(firstDefaultDocumentFileNode)
        val accessPermission = AccessPermission.UNKNOWN
        whenever(getNodeAccessUseCase(nodeId = firstDefaultId)) doReturn accessPermission
        whenever(
            documentUiEntityMapper(
                firstDefaultDocumentFileNode,
                accessPermission,
                canBeMovedToRubbishBin
            )
        ) doReturn expectedFirstDocumentUiEntity.copy(canBeMovedToRubbishBin = canBeMovedToRubbishBin)
        val rubbishBinNodeId = NodeId(4321L)
        val rubbishBinNode = if (canBeMovedToRubbishBin) {
            mock<FileNode> {
                on { id } doReturn rubbishBinNodeId
            }
        } else null
        whenever(getRubbishBinFolderUseCase()) doReturn rubbishBinNode
        whenever(
            checkNodeCanBeMovedToTargetNode(
                nodeId = firstDefaultId,
                targetNodeId = rubbishBinNodeId
            )
        ) doReturn canBeMovedToRubbishBin

        underTest.refreshDocumentNodes()

        underTest.uiState.test {
            assertThat(
                expectMostRecentItem().allDocuments.first().canBeMovedToRubbishBin
            ).isEqualTo(canBeMovedToRubbishBin)
        }
    }

    @Test
    fun `test that the hidden node toolbar item can be hidden when a selection is made for free account`() =
        runTest {
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
            accountDetailFlow.emit(accountDetail)
            initDocumentReturnWithSpecificDocument()
            whenever(getRubbishBinFolderUseCase()) doReturn null

            underTest.refreshDocumentNodes()
            underTest.onItemSelected(documentList[0], 0)

            underTest.uiState.test {
                assertThat(
                    expectMostRecentItem().toolbarActionsModifierItem!!.item.hiddenNodeItem
                ).isEqualTo(
                    DocumentSectionHiddenNodeActionModifierItem(
                        isEnabled = true,
                        canBeHidden = true
                    )
                )
            }
        }

    @Test
    fun `test that the hidden node toolbar item can be hidden when a selection is made for an expired business account`() =
        runTest {
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
            whenever(getBusinessStatusUseCase()) doReturn BusinessAccountStatus.Expired
            accountDetailFlow.emit(accountDetail)
            initDocumentReturnWithSpecificDocument()
            whenever(getRubbishBinFolderUseCase()) doReturn null

            underTest.refreshDocumentNodes()
            underTest.onItemSelected(documentList[1], 1)

            underTest.uiState.test {
                assertThat(
                    expectMostRecentItem().toolbarActionsModifierItem!!.item.hiddenNodeItem
                ).isEqualTo(
                    DocumentSectionHiddenNodeActionModifierItem(
                        isEnabled = true,
                        canBeHidden = true
                    )
                )
            }
        }

    @Test
    fun `test that the hidden node toolbar item can be hidden when a selection is made for a not sensitive node`() =
        runTest {
            whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_MODIFICATION_DESC)
            whenever(getRubbishBinFolderUseCase()) doReturn null
            val nodeId = NodeId(123L)
            val documentFileNode = mock<TypedFileNode> {
                on { id } doReturn nodeId
                on { isSensitiveInherited } doReturn false
                on { isMarkedSensitive } doReturn false
            }
            whenever(getAllDocumentsUseCase()) doReturn listOf(documentFileNode)
            val uiEntity = newDocumentUiEntity()
            whenever(
                documentUiEntityMapper(
                    eq(documentFileNode),
                    any(),
                    any()
                )
            ) doReturn uiEntity
            whenever(
                getNodeAccessUseCase(nodeId = nodeId)
            ) doReturn AccessPermission.UNKNOWN

            underTest.refreshDocumentNodes()
            underTest.onItemSelected(uiEntity, 0)

            underTest.uiState.test {
                assertThat(
                    expectMostRecentItem().toolbarActionsModifierItem!!.item.hiddenNodeItem
                ).isEqualTo(
                    DocumentSectionHiddenNodeActionModifierItem(
                        isEnabled = true,
                        canBeHidden = true
                    )
                )
            }
        }

    @Test
    fun `test that the hidden node toolbar item cannot be hidden when a selection is made with non-hidden criteria`() =
        runTest {
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
            whenever(monitorNodeUpdatesUseCase()) doReturn flowOf(NodeUpdate(emptyMap()))
            whenever(monitorAccountDetailUseCase()) doReturn flowOf(accountDetail)
            whenever(getBusinessStatusUseCase()) doReturn BusinessAccountStatus.Active
            whenever(monitorShowHiddenItemsUseCase()) doReturn flowOf(true)
            whenever(isHiddenNodesOnboardedUseCase()) doReturn false
            whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_MODIFICATION_DESC)
            whenever(getRubbishBinFolderUseCase()) doReturn null
            val nodeId = NodeId(123L)
            val documentFileNode = mock<TypedFileNode> {
                on { id } doReturn nodeId
                on { isSensitiveInherited } doReturn false
                on { isMarkedSensitive } doReturn true
            }
            whenever(getAllDocumentsUseCase()) doReturn listOf(documentFileNode)
            val uiEntity = newDocumentUiEntity(
                isMarkedSensitive = true,
                isSensitiveInherited = false
            )
            whenever(
                documentUiEntityMapper(
                    eq(documentFileNode),
                    any(),
                    any()
                )
            ) doReturn uiEntity
            whenever(
                getNodeAccessUseCase(nodeId = nodeId)
            ) doReturn AccessPermission.UNKNOWN

            initUnderTest()
            underTest.refreshDocumentNodes()
            underTest.onItemSelected(uiEntity, 0)

            underTest.uiState.test {
                assertThat(
                    expectMostRecentItem().toolbarActionsModifierItem!!.item.hiddenNodeItem
                ).isEqualTo(
                    DocumentSectionHiddenNodeActionModifierItem(
                        isEnabled = true,
                        canBeHidden = false
                    )
                )
            }
        }

    private fun newDocumentUiEntity(
        id: NodeId = NodeId(1),
        name: String = "",
        size: Long = 0L,
        thumbnail: File? = null,
        @DrawableRes icon: Int = 0,
        fileTypeInfo: FileTypeInfo = UnknownFileTypeInfo(mimeType = "", extension = ""),
        isFavourite: Boolean = false,
        isExported: Boolean = false,
        isTakenDown: Boolean = false,
        hasVersions: Boolean = false,
        modificationTime: Long = 0L,
        label: Int = 0,
        nodeAvailableOffline: Boolean = false,
        isSelected: Boolean = false,
        isMarkedSensitive: Boolean = false,
        isSensitiveInherited: Boolean = false,
        isIncomingShare: Boolean = false,
        accessPermission: AccessPermission = AccessPermission.UNKNOWN,
        canBeMovedToRubbishBin: Boolean = false,
    ) = DocumentUiEntity(
        id = id,
        name = name,
        size = size,
        thumbnail = thumbnail,
        icon = icon,
        fileTypeInfo = fileTypeInfo,
        isFavourite = isFavourite,
        isExported = isExported,
        isTakenDown = isTakenDown,
        hasVersions = hasVersions,
        modificationTime = modificationTime,
        label = label,
        nodeAvailableOffline = nodeAvailableOffline,
        isSelected = isSelected,
        isMarkedSensitive = isMarkedSensitive,
        isSensitiveInherited = isSensitiveInherited,
        isIncomingShare = isIncomingShare,
        accessPermission = accessPermission,
        canBeMovedToRubbishBin = canBeMovedToRubbishBin
    )
}