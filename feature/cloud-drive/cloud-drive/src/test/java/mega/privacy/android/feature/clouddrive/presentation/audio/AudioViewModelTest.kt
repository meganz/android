package mega.privacy.android.feature.clouddrive.presentation.audio

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.Offline
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.NodeUpdate
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.entity.search.SearchCategory
import mega.privacy.android.domain.entity.search.SearchParameters
import mega.privacy.android.domain.entity.search.SearchTarget
import mega.privacy.android.domain.usecase.SetCloudSortOrder
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.node.hiddennode.MonitorHiddenNodesEnabledUseCase
import mega.privacy.android.domain.usecase.node.sort.MonitorSortCloudOrderUseCase
import mega.privacy.android.domain.usecase.offline.MonitorOfflineNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.search.SearchUseCase
import mega.privacy.android.domain.usecase.setting.MonitorShowHiddenItemsUseCase
import mega.privacy.android.domain.usecase.viewtype.MonitorViewType
import mega.privacy.android.domain.usecase.viewtype.SetViewType
import mega.privacy.android.feature.clouddrive.presentation.audio.model.AudioAction
import mega.privacy.android.feature.clouddrive.presentation.audio.model.AudioUiState
import mega.privacy.android.feature_flags.AppFeatures
import mega.privacy.android.shared.nodes.mapper.NodeSortConfigurationUiMapper
import mega.privacy.android.shared.nodes.mapper.NodeViewItemMapper
import mega.privacy.android.shared.nodes.model.NodeSortConfiguration
import mega.privacy.android.shared.nodes.model.NodeViewItem
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.clearInvocations
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(CoroutineMainDispatcherExtension::class)
class AudioViewModelTest {
    private lateinit var underTest: AudioViewModel

    private val searchUseCase: SearchUseCase = mock()
    private val setViewTypeUseCase: SetViewType = mock()
    private val monitorViewTypeUseCase: MonitorViewType = mock()
    private val nodeViewItemMapper: NodeViewItemMapper = mock()
    private val setCloudSortOrderUseCase: SetCloudSortOrder = mock()
    private val nodeSortConfigurationUiMapper: NodeSortConfigurationUiMapper = mock()
    private val monitorSortCloudOrderUseCase: MonitorSortCloudOrderUseCase = mock()
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase = mock()
    private val monitorHiddenNodesEnabledUseCase: MonitorHiddenNodesEnabledUseCase = mock()
    private val monitorShowHiddenItemsUseCase: MonitorShowHiddenItemsUseCase = mock()
    private val monitorNodeUpdatesUseCase: MonitorNodeUpdatesUseCase = mock()
    private val monitorOfflineNodeUpdatesUseCase: MonitorOfflineNodeUpdatesUseCase = mock()

    private val sortOrderFlow = MutableStateFlow(SortOrder.ORDER_MODIFICATION_DESC)

    @BeforeEach
    fun setUpMain() {
        underTest = AudioViewModel(
            searchUseCase = searchUseCase,
            setViewTypeUseCase = setViewTypeUseCase,
            monitorViewTypeUseCase = monitorViewTypeUseCase,
            nodeViewItemMapper = nodeViewItemMapper,
            setCloudSortOrderUseCase = setCloudSortOrderUseCase,
            nodeSortConfigurationUiMapper = nodeSortConfigurationUiMapper,
            monitorSortCloudOrderUseCase = monitorSortCloudOrderUseCase,
            getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
            monitorHiddenNodesEnabledUseCase = monitorHiddenNodesEnabledUseCase,
            monitorShowHiddenItemsUseCase = monitorShowHiddenItemsUseCase,
            monitorNodeUpdatesUseCase = monitorNodeUpdatesUseCase,
            monitorOfflineNodeUpdatesUseCase = monitorOfflineNodeUpdatesUseCase,
        )
    }

    @AfterEach
    fun resetMocks() {
        reset(
            searchUseCase,
            setViewTypeUseCase,
            monitorViewTypeUseCase,
            nodeViewItemMapper,
            setCloudSortOrderUseCase,
            nodeSortConfigurationUiMapper,
            monitorSortCloudOrderUseCase,
            getFeatureFlagValueUseCase,
            monitorHiddenNodesEnabledUseCase,
            monitorShowHiddenItemsUseCase,
            monitorNodeUpdatesUseCase,
            monitorOfflineNodeUpdatesUseCase,
        )
    }

    private fun setupStubs(
        domainNodes: List<TypedNode> = emptyList(),
        searchRevampEnabled: Boolean = false,
    ) {
        val viewItems = domainNodes.map {
            NodeViewItem(it)
        }
        whenever(monitorNodeUpdatesUseCase()).thenReturn(
            flow {
                emit(NodeUpdate(emptyMap()))
                awaitCancellation()
            }
        )
        whenever(monitorOfflineNodeUpdatesUseCase()).thenReturn(
            flow {
                emit(emptyList())
                awaitCancellation()
            }
        )

        setCloudSortOrderUseCase.stub {
            onBlocking { invoke(any()) }.thenAnswer { invocation -> sortOrderFlow.tryEmit(invocation.arguments[0] as SortOrder) }
        }

        whenever(monitorSortCloudOrderUseCase()).thenReturn(
            sortOrderFlow
        )
        whenever(nodeSortConfigurationUiMapper(any(), any())).thenReturn(
            NodeSortConfiguration.default
        )
        whenever(monitorHiddenNodesEnabledUseCase()).thenReturn(flowOf(false))
        whenever(monitorShowHiddenItemsUseCase()).thenReturn(flowOf(false))
        searchUseCase.stub {
            onBlocking {
                invoke(
                    parentHandle = any(),
                    nodeSourceType = any(),
                    searchParameters = any(),
                    isSingleActivityEnabled = any(),
                )
            } doReturn domainNodes
        }
        nodeViewItemMapper.stub {
            onBlocking {
                invoke(
                    nodeList = any(),
                    nodeSourceType = any(),
                    isPublicNodes = any(),
                    showPublicLinkCreationTime = any(),
                    highlightedNodeId = anyOrNull(),
                    highlightedNames = anyOrNull(),
                    isContactVerificationOn = any(),
                )
            } doReturn viewItems
        }
        getFeatureFlagValueUseCase.stub {
            onBlocking { invoke(AppFeatures.SearchRevamp) } doReturn searchRevampEnabled
        }
        whenever(monitorViewTypeUseCase()).thenReturn(flowOf(ViewType.LIST))
    }

    @Test
    fun `test that uiState loads items when monitors emit`() =
        runTest {
            setupStubs()

            underTest.uiState
                .filterIsInstance<AudioUiState.Data>()
                .test {
                    val data = awaitItem()
                    assertThat(data.items).isEmpty()
                    assertThat(data.currentViewType).isEqualTo(ViewType.LIST)
                    assertThat(data.selectedSortOrder).isEqualTo(SortOrder.ORDER_MODIFICATION_DESC)
                    assertThat(data.isSearchRevampEnabled).isFalse()
                }
        }

    @Test
    fun `test that uiState maps search results into items`() = runTest {
        val fileNode = mock<TypedFileNode> {
            on { id } doReturn NodeId(42L)
        }
        setupStubs(domainNodes = listOf(fileNode))

        underTest.uiState
            .filterIsInstance<AudioUiState.Data>()
            .test {
                val loaded = awaitItem()
                assertThat(loaded.items).hasSize(1)
                assertThat(loaded.items[0].node.id).isEqualTo(NodeId(42L))
            }
    }

    @Test
    fun `test that processAction ItemClicked then OpenedFileNodeHandled updates openedFileNode`() =
        runTest {
            setupStubs()
            val fileNode = mock<TypedFileNode> {
                on { id } doReturn NodeId(7L)
            }
            underTest.processAction(AudioAction.ItemClicked(fileNode))
            advanceUntilIdle()

            underTest.uiState
                .filterIsInstance<AudioUiState.Data>()
                .test {
                    val opened = awaitItem()
                    assertThat(opened.openedFileNode).isEqualTo(fileNode)

                    underTest.processAction(AudioAction.OpenedFileNodeHandled)
                    val cleared = awaitItem()
                    assertThat(cleared.openedFileNode).isNull()
                }
        }

    @Test
    fun `test that setCloudSortOrder maps configuration and invokes use case`() =
        runTest {
            setupStubs()
            val config = NodeSortConfiguration.default
            val sortOrder = SortOrder.ORDER_MODIFICATION_DESC
            whenever(nodeSortConfigurationUiMapper(config)).thenReturn(sortOrder)

            underTest.setCloudSortOrder(config)
            advanceUntilIdle()

            verify(nodeSortConfigurationUiMapper).invoke(config)
            verify(setCloudSortOrderUseCase).invoke(sortOrder)
        }

    @Test
    fun `test that uiState reflects isHiddenNodesEnabled when monitorHiddenNodesEnabledUseCase emits true`() =
        runTest {
            setupStubs()
            whenever(monitorHiddenNodesEnabledUseCase()).thenReturn(flowOf(true))

            underTest.uiState
                .filterIsInstance<AudioUiState.Data>()
                .test {
                    val loaded = awaitItem()
                    assertThat(loaded.isHiddenNodesEnabled).isTrue()
                }
        }

    @Test
    fun `test that uiState reflects showHiddenNodes when monitorShowHiddenItemsUseCase emits true`() =
        runTest {
            setupStubs()
            whenever(monitorShowHiddenItemsUseCase()).thenReturn(flowOf(true))

            underTest.uiState
                .filterIsInstance<AudioUiState.Data>()
                .test {
                    val loaded = awaitItem()
                    assertThat(loaded.showHiddenNodes).isTrue()
                }
        }

    @Test
    fun `test that audioItems are refreshed when monitorNodeUpdatesUseCase emits`() =
        runTest {
            val nodeUpdateFlow = MutableStateFlow(NodeUpdate(emptyMap()))
            val testNode = mock<FileNode> {
                on { id }.thenReturn(NodeId(1L))
            }
            val fileNode = mock<TypedFileNode> {
                on { id } doReturn NodeId(42L)
            }
            setupStubs(domainNodes = listOf(fileNode))
            monitorNodeUpdatesUseCase.stub {
                on { invoke() }.thenReturn(nodeUpdateFlow)
            }

            underTest.uiState
                .filterIsInstance<AudioUiState.Data>()
                .test {
                    assertThat(awaitItem().items).isNotEmpty()
                    clearInvocations(searchUseCase)

                    nodeUpdateFlow.emit(NodeUpdate(mapOf(testNode to emptyList())))
                    verify(searchUseCase).invoke(
                        NodeId(-1),
                        NodeSourceType.AUDIO,
                        SearchParameters(
                            query = "",
                            searchTarget = SearchTarget.ROOT_NODES,
                            searchCategory = SearchCategory.AUDIO,
                        ),
                        true
                    )
                    cancelAndIgnoreRemainingEvents()
                }
        }

    @Test
    fun `test that audioItems are refreshed when monitorOfflineNodeUpdatesUseCase emits`() =
        runTest {
            val offlineUpdatesFlow = MutableSharedFlow<List<Offline>>(extraBufferCapacity = 1)
            val testOffline = mock<Offline> {
                on { id }.thenReturn(1)
            }
            val fileNode = mock<TypedFileNode> {
                on { id } doReturn NodeId(42L)
            }
            setupStubs(domainNodes = listOf(fileNode))
            monitorOfflineNodeUpdatesUseCase.stub {
                on { invoke() }.thenReturn(offlineUpdatesFlow)
            }

            underTest.uiState
                .filterIsInstance<AudioUiState.Data>()
                .test {
                    assertThat(awaitItem().items).isNotEmpty()
                    clearInvocations(searchUseCase)

                    offlineUpdatesFlow.emit(listOf(testOffline))
                    verify(searchUseCase).invoke(
                        NodeId(-1),
                        NodeSourceType.AUDIO,
                        SearchParameters(
                            query = "",
                            searchTarget = SearchTarget.ROOT_NODES,
                            searchCategory = SearchCategory.AUDIO,
                        ),
                        true
                    )
                    cancelAndIgnoreRemainingEvents()
                }
        }
}
