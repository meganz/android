@file:OptIn(ExperimentalCoroutinesApi::class)

package mega.privacy.android.app.presentation.documentsection

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.domain.usecase.GetNodeByHandle
import mega.privacy.android.app.presentation.documentsection.DocumentSectionViewModel
import mega.privacy.android.app.presentation.documentsection.model.DocumentUiEntity
import mega.privacy.android.app.presentation.documentsection.model.DocumentUiEntityMapper
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.Offline
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.account.AccountDetail
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeUpdate
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import mega.privacy.android.domain.usecase.GetFileUrlByNodeHandleUseCase
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.IsHiddenNodesOnboardedUseCase
import mega.privacy.android.domain.usecase.UpdateNodeSensitiveUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.documentsection.GetAllDocumentsUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.file.GetFingerprintUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiHttpServerIsRunningUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiHttpServerStartUseCase
import mega.privacy.android.domain.usecase.network.IsConnectedToInternetUseCase
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.offline.MonitorOfflineNodeUpdatesUseCase
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
import org.mockito.kotlin.wheneverBlocking
import mega.privacy.android.app.TimberJUnit5Extension

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
    private val getNodeByIdUseCase = mock<GetNodeByIdUseCase>()
    private val getFingerprintUseCase = mock<GetFingerprintUseCase>()
    private val megaApiHttpServerIsRunningUseCase = mock<MegaApiHttpServerIsRunningUseCase>()
    private val megaApiHttpServerStartUseCase = mock<MegaApiHttpServerStartUseCase>()
    private val getFileUrlByNodeHandleUseCase = mock<GetFileUrlByNodeHandleUseCase>()
    private val isConnectedToInternetUseCase = mock<IsConnectedToInternetUseCase>()
    private val updateNodeSensitiveUseCase = mock<UpdateNodeSensitiveUseCase>()
    private val monitorShowHiddenItemsUseCase = mock<MonitorShowHiddenItemsUseCase> {
        on {
            invoke()
        }.thenReturn(flowOf(false))
    }
    private val getFeatureFlagValueUseCase = mock<GetFeatureFlagValueUseCase>()

    private val monitorAccountDetailUseCase = mock<MonitorAccountDetailUseCase> {
        on {
            invoke()
        }.thenReturn(flowOf(AccountDetail()))
    }
    private val isHiddenNodesOnboardedUseCase = mock<IsHiddenNodesOnboardedUseCase> {
        onBlocking {
            invoke()
        }.thenReturn(false)
    }


    private val expectedDocument =
        mock<DocumentUiEntity> { on { name }.thenReturn("document name") }

    @BeforeEach
    fun setUp() {
        wheneverBlocking { monitorNodeUpdatesUseCase() }.thenReturn(fakeMonitorNodeUpdatesFlow)
        wheneverBlocking { monitorOfflineNodeUpdatesUseCase() }.thenReturn(
            fakeMonitorOfflineNodeUpdatesFlow
        )
        wheneverBlocking { monitorViewType() }.thenReturn(fakeMonitorViewTypeFlow)
        wheneverBlocking { getCloudSortOrder() }.thenReturn(SortOrder.ORDER_NONE)
        wheneverBlocking { getFeatureFlagValueUseCase(any()) }.thenReturn(false)
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
            getNodeByIdUseCase = getNodeByIdUseCase,
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
            getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
        )
    }

    @AfterEach
    fun resetMocks() {
        reset(
            getAllDocumentsUseCase,
            documentUiEntityMapper,
            getCloudSortOrder,
            monitorNodeUpdatesUseCase,
            monitorOfflineNodeUpdatesUseCase,
            monitorViewType,
            setViewType,
            getNodeByHandle,
            getNodeByIdUseCase,
            getFingerprintUseCase,
            megaApiHttpServerIsRunningUseCase,
            megaApiHttpServerStartUseCase,
            getFileUrlByNodeHandleUseCase,
            isConnectedToInternetUseCase
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
            assertThat(initial.selectedDocumentHandles).isEmpty()
            assertThat(initial.searchMode).isFalse()
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
        whenever(getAllDocumentsUseCase()).thenReturn(listOf(mock(), mock()))
        whenever(documentUiEntityMapper(any())).thenReturn(expectedDocument)
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

            underTest.uiState.drop(1).test {
                fakeMonitorNodeUpdatesFlow.emit(NodeUpdate(emptyMap()))
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

            underTest.uiState.drop(1).test {
                fakeMonitorOfflineNodeUpdatesFlow.emit(emptyList())
                val actual = awaitItem()
                assertThat(actual.sortOrder).isEqualTo(SortOrder.ORDER_MODIFICATION_DESC)
                assertThat(actual.allDocuments.size).isEqualTo(2)
                assertThat(actual.isLoading).isEqualTo(false)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that the result returned correctly when search query is not empty`() = runTest {
        val expectedDocumentNode = mock<TypedFileNode> { on { name }.thenReturn("document name") }
        val documentNode = mock<TypedFileNode> { on { name }.thenReturn("name") }
        val expectedDocument = mock<DocumentUiEntity> { on { name }.thenReturn("document name") }
        val document = mock<DocumentUiEntity> { on { name }.thenReturn("name") }

        whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_MODIFICATION_DESC)
        whenever(getAllDocumentsUseCase()).thenReturn(listOf(expectedDocumentNode, documentNode))
        whenever(documentUiEntityMapper(documentNode)).thenReturn(document)
        whenever(documentUiEntityMapper(expectedDocumentNode)).thenReturn(expectedDocument)

        initUnderTest()

        underTest.refreshDocumentNodes()

        underTest.uiState.test {
            assertThat(awaitItem().allDocuments.size).isEqualTo(2)
            underTest.searchQuery("document")
            val actual = awaitItem()
            assertThat(actual.allDocuments.size).isEqualTo(1)
            assertThat(actual.scrollToTop).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that the searchMode is correctly updated`() = runTest {
        whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_MODIFICATION_DESC)
        whenever(getAllDocumentsUseCase()).thenReturn(emptyList())

        initUnderTest()

        underTest.uiState.drop(1).test {
            underTest.searchReady()
            assertThat(awaitItem().searchMode).isTrue()

            underTest.exitSearch()
            assertThat(awaitItem().searchMode).isFalse()
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
        getDocumentUiEntity(it.id)
    }

    @Test
    fun `test that the selected item is updated by 1 when long clicked`() = runTest {
        initDocumentReturnWithSpecificDocument()
        initUnderTest()

        underTest.refreshDocumentNodes()
        underTest.onItemSelected(documentList[0], 0)

        underTest.uiState.test {
            val actual = awaitItem()
            assertThat(actual.selectedDocumentHandles.size).isEqualTo(1)
            assertThat(actual.actionMode).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    private suspend fun initDocumentReturnWithSpecificDocument() {
        whenever(getAllDocumentsUseCase()).thenReturn(nodeList)
        whenever(documentUiEntityMapper(nodeList[0])).thenReturn(documentList[0])
        whenever(documentUiEntityMapper(nodeList[1])).thenReturn(documentList[1])
        whenever(documentUiEntityMapper(nodeList[2])).thenReturn(documentList[2])
        whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_MODIFICATION_DESC)
    }

    private fun getDocumentUiEntity(documentId: NodeId) = mock<DocumentUiEntity> {
        on { id }.thenReturn(documentId)
        on { name }.thenReturn("name")
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
                    assertThat(it.selectedDocumentHandles.size).isEqualTo(1)
                    assertThat(it.actionMode).isTrue()
                }
                underTest.onItemSelected(documentList[1], 1)
                assertThat(awaitItem().selectedDocumentHandles.size).isEqualTo(2)
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
            assertThat(actual.selectedDocumentHandles.size).isEqualTo(3)
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
                    assertThat(it.selectedDocumentHandles).isEmpty()
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
            assertThat(actual.selectedDocumentHandles.size).isEqualTo(1)
            assertThat(actual.actionMode).isTrue()
            assertThat(underTest.getSelectedMegaNode().size).isEqualTo(1)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that getSelectedNodes returns correctly by the item is clicked`() = runTest {
        initDocumentReturnWithSpecificDocument()
        whenever(getNodeByIdUseCase(documentList[0].id)).thenReturn(mock())
        initUnderTest()

        underTest.refreshDocumentNodes()
        underTest.onItemSelected(documentList[0], 0)

        underTest.uiState.test {
            val actual = awaitItem()
            assertThat(actual.selectedDocumentHandles.size).isEqualTo(1)
            assertThat(actual.actionMode).isTrue()
            assertThat(underTest.getSelectedNodes().size).isEqualTo(1)
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
}