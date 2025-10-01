package mega.privacy.android.app.presentation.shares.links

import android.view.MenuItem
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.StateEventWithContentConsumed
import de.palm.composestateevents.StateEventWithContentTriggered
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.presentation.clouddrive.OptionItems
import mega.privacy.android.app.presentation.data.NodeUIItem
import mega.privacy.android.app.presentation.mapper.HandleOptionClickMapper
import mega.privacy.android.app.presentation.mapper.OptionsItemInfo
import mega.privacy.android.app.presentation.shares.links.model.LinksUiState
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.publiclink.PublicLinkFile
import mega.privacy.android.domain.entity.node.publiclink.PublicLinkFolder
import mega.privacy.android.domain.entity.node.publiclink.PublicLinkNode
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.domain.usecase.CheckNodeCanBeMovedToTargetNode
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import mega.privacy.android.domain.usecase.GetLinksSortOrder
import mega.privacy.android.domain.usecase.mediaplayer.videoplayer.GetNodeAccessUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.node.IsNodeInRubbishBinUseCase
import mega.privacy.android.domain.usecase.node.MonitorFolderNodeDeleteUpdatesUseCase
import mega.privacy.android.domain.usecase.node.publiclink.MonitorPublicLinksUseCase
import mega.privacy.android.domain.usecase.rubbishbin.GetRubbishBinFolderUseCase
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class LinksViewModelTest {
    private lateinit var underTest: LinksViewModel
    private lateinit var monitorLinksChannel: Channel<List<PublicLinkNode>>
    private val monitorPublicLinksUseCase =
        mock<MonitorPublicLinksUseCase> {
            on { invoke() }.thenAnswer {
                monitorLinksChannel = Channel()
                monitorLinksChannel.consumeAsFlow()
            }
        }
    private val getCloudSortOrder: GetCloudSortOrder = mock()
    private val getLinksSortOrder: GetLinksSortOrder = mock()
    private val monitorConnectivityUseCase: MonitorConnectivityUseCase = mock()
    private val handleOptionClickMapper: HandleOptionClickMapper = mock()
    private val monitorFolderNodeDeleteUpdatesUseCase: MonitorFolderNodeDeleteUpdatesUseCase =
        mock()
    private val isNodeInRubbishBinUseCase: IsNodeInRubbishBinUseCase = mock()
    private val checkNodeCanBeMovedToTargetNode: CheckNodeCanBeMovedToTargetNode = mock()
    private val getRubbishBinFolderUseCase: GetRubbishBinFolderUseCase = mock()
    private val getNodeAccessUseCase: GetNodeAccessUseCase = mock()

    @BeforeAll
    internal fun initialise() {
        runBlocking {
            stubCommon()
        }
    }

    @BeforeEach
    internal fun setUp() {
        underTest = LinksViewModel(
            monitorPublicLinksUseCase = monitorPublicLinksUseCase,
            monitorFolderNodeDeleteUpdatesUseCase = monitorFolderNodeDeleteUpdatesUseCase,
            getCloudSortOrder = getCloudSortOrder,
            getLinksSortOrder = getLinksSortOrder,
            monitorConnectivityUseCase = monitorConnectivityUseCase,
            handleOptionClickMapper = handleOptionClickMapper,
            isNodeInRubbishBinUseCase = isNodeInRubbishBinUseCase,
            checkNodeCanBeMovedToTargetNode = checkNodeCanBeMovedToTargetNode,
            getRubbishBinFolderUseCase = getRubbishBinFolderUseCase,
            getNodeAccessUseCase = getNodeAccessUseCase
        )
    }

    private suspend fun stubCommon() {
        whenever(getLinksSortOrder()).thenReturn(SortOrder.ORDER_NONE)
        whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_NONE)
        whenever(monitorConnectivityUseCase()).thenReturn(emptyFlow())
        whenever(monitorFolderNodeDeleteUpdatesUseCase()).thenReturn(emptyFlow())
        whenever(isNodeInRubbishBinUseCase(NodeId(any()))).thenReturn(false)
    }

    @AfterAll
    internal fun tearDown() {
        reset(
            monitorPublicLinksUseCase,
            monitorFolderNodeDeleteUpdatesUseCase,
            getCloudSortOrder,
            getLinksSortOrder,
            monitorConnectivityUseCase,
            handleOptionClickMapper,
            isNodeInRubbishBinUseCase,
            checkNodeCanBeMovedToTargetNode,
            getRubbishBinFolderUseCase,
            getNodeAccessUseCase
        )
    }

    @Test
    internal fun `test that public links are returned`() = runTest {
        val publicLinkNodes = listOf<PublicLinkFolder>(mock(), mock())
        monitorLinksChannel.send(publicLinkNodes)
        underTest.state.test {
            val expected = awaitItem()
            assertThat(expected.nodesList).hasSize(2)
        }
    }

    @Test
    fun `test that calling openFolderByHandle opens the folder`() = runTest {
        val childLinkNodes = listOf<PublicLinkFolder>(mock())
        val flow = flow {
            emit(childLinkNodes)
            awaitCancellation()
        }
        val publicLinkNodes = listOf<PublicLinkFolder>(mock {
            on { id }.thenReturn(NodeId(12))
            on { children }.thenReturn(flow)
        })

        monitorLinksChannel.send(publicLinkNodes)
        underTest.openFolderByHandle(12)

        underTest.state.test {
            val expected = awaitItem()
            assertThat(expected.nodesList).hasSize(1)
            assertThat(expected.parentNode?.id?.longValue).isEqualTo(12)
        }
    }

    @Test
    internal fun `test that updates from the root are ignored while children are displayed`() =
        runTest {
            val childLinkNodes = listOf<PublicLinkFolder>(mock())
            val flow = flow {
                emit(childLinkNodes)
                awaitCancellation()
            }
            val publicLinkNodes = listOf<PublicLinkFolder>(mock {
                on { id }.thenReturn(NodeId(12))
                on { children }.thenReturn(flow)
            })

            monitorLinksChannel.send(publicLinkNodes)
            underTest.openFolderByHandle(12)

            underTest.state.test {
                val expected = awaitItem()
                assertThat(expected.nodesList).hasSize(1)
                assertThat(monitorLinksChannel.isClosedForSend).isTrue()
                assertThat(cancelAndConsumeRemainingEvents()).isEmpty()
            }
        }

    @Test
    internal fun `test that calling performBackNavigation returns the children of the parent folder`() =
        runTest {
            val childLinkNodes = listOf<PublicLinkFolder>(mock())
            val flow = flow {
                emit(childLinkNodes)
                awaitCancellation()
            }
            val publicLinkNodes = listOf<PublicLinkFolder>(mock {
                on { id }.thenReturn(NodeId(12))
                on { children }.thenReturn(flow)
            })

            monitorLinksChannel.send(publicLinkNodes)
            underTest.openFolderByHandle(12)

            underTest.state.test {
                assertThat(awaitItem()).isInstanceOf(LinksUiState::class.java)
            }
            underTest.performBackNavigation()
            underTest.state.test {
                val expected = awaitItem()
                assertThat(expected.parentNode?.id).isEqualTo(NodeId(12))
                assertThat(expected.nodesList).hasSize(1)
            }
        }

    @Test
    internal fun `test that calling performBackNavigation on the first level returns all public nodes`() =
        runTest {
            val publicLinkNodes = listOf<PublicLinkFolder>(mock())

            val currentFolder = mock<PublicLinkFolder> {
                on { parent }.thenReturn(null)
                on { id }.thenReturn(NodeId(12))
                on { children }.thenReturn(emptyFlow())
            }
            underTest.onItemClicked(
                NodeUIItem(
                    currentFolder,
                    isSelected = true,
                    isInvisible = false
                )
            )
            underTest.state.test {
                assertThat(awaitItem()).isInstanceOf(LinksUiState::class.java)
            }

            underTest.performBackNavigation()
            monitorLinksChannel.send(publicLinkNodes)
            underTest.state.test {
                val expected = awaitItem()
                assertThat(expected.parentNode).isNull()
                assertThat(expected.nodesList).hasSize(1)
            }
        }

    @Test
    fun `test that the selected node handle count is decremented when one of the selected nodes is clicked`() =
        runTest {
            val nodesListItem1 = mock<PublicLinkFile>()
            val nodesListItem2 = mock<PublicLinkFolder>()
            whenever(nodesListItem1.id.longValue).thenReturn(1L)
            whenever(nodesListItem2.id.longValue).thenReturn(2L)
            val publicLinkNodes = listOf<PublicLinkNode>(nodesListItem1, nodesListItem2)
            monitorLinksChannel.send(publicLinkNodes)
            whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_NONE)

            underTest.refreshLinkNodes()
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
                assertThat(state.selectedFolderNodes).isEqualTo(0)
                assertThat(state.selectedFileNodes).isEqualTo(0)
                assertThat(state.selectedNodes.size).isEqualTo(0)
            }
        }

    @Test
    fun `test that the selected node handle count is incremented when a node is long clicked`() =
        runTest {
            val nodesListItem1 = mock<PublicLinkFolder>()
            val nodesListItem2 = mock<PublicLinkFile>()
            whenever(nodesListItem1.id.longValue).thenReturn(1L)
            whenever(nodesListItem2.id.longValue).thenReturn(2L)

            val publicLinkNodes = listOf<PublicLinkNode>(nodesListItem1, nodesListItem2)
            monitorLinksChannel.send(publicLinkNodes)
            underTest.refreshLinkNodes()
            underTest.onLongItemClicked(
                NodeUIItem(
                    nodesListItem1,
                    isSelected = false,
                    isInvisible = false
                )
            )
            underTest.state.test {
                val state = awaitItem()
                assertThat(state.selectedFolderNodes).isEqualTo(1)
                assertThat(state.selectedFileNodes).isEqualTo(0)
                assertThat(state.selectedNodes.size).isEqualTo(1)
            }
        }

    @Test
    fun `test that the selected node handle count is incremented when the selected node is clicked`() =
        runTest {
            val nodesListItem1 = mock<PublicLinkFile>()
            val nodesListItem2 = mock<PublicLinkFolder>()
            whenever(nodesListItem1.id.longValue).thenReturn(1L)
            whenever(nodesListItem2.id.longValue).thenReturn(2L)

            val publicLinkNodes = listOf<PublicLinkNode>(nodesListItem1, nodesListItem2)
            monitorLinksChannel.send(publicLinkNodes)

            underTest.refreshLinkNodes()
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
                assertThat(state.selectedFolderNodes).isEqualTo(1)
                assertThat(state.selectedFileNodes).isEqualTo(1)
                assertThat(state.selectedNodes.size).isEqualTo(2)
            }
        }

    @Test
    fun `test that the sizes of both selected node handles and the nodes are equal when selecting all nodes`() =
        runTest {
            val publicLinkNodes = listOf<PublicLinkFolder>(mock(), mock())
            monitorLinksChannel.send(publicLinkNodes)
            underTest.refreshLinkNodes()
            underTest.selectAllNodes()
            assertThat(underTest.state.value.nodesList.size)
                .isEqualTo(underTest.state.value.selectedNodes.size)
        }

    @Test
    fun `test that the selected node handles is empty when clearing all nodes`() = runTest {
        underTest.clearAllNodesSelection()
        underTest.state.test {
            val state = awaitItem()
            assertThat(state.selectedNodes).isEmpty()
        }
    }

    @Test
    fun `test that handle option click mapper is invoked when selecting an option item`() =
        runTest {
            val menuItem: MenuItem = mock()
            whenever(handleOptionClickMapper.invoke(menuItem, emptyList())).thenReturn(
                OptionsItemInfo(
                    optionClickedType = OptionItems.MOVE_CLICKED,
                    selectedNode = emptyList(),
                    selectedMegaNode = emptyList()
                )
            )
            underTest.onOptionItemClicked(item = menuItem)
            verify(handleOptionClickMapper).invoke(menuItem, emptyList())
        }

    @Test
    fun `test that download event is updated when on download option click is invoked and both download option and feature flag are true`() =
        runTest {
            onDownloadOptionClick()
            underTest.state.test {
                val state = awaitItem()
                assertThat(state.downloadEvent)
                    .isInstanceOf(StateEventWithContentTriggered::class.java)
                assertThat((state.downloadEvent as StateEventWithContentTriggered).content)
                    .isInstanceOf(TransferTriggerEvent.StartDownloadNode::class.java)
            }
        }

    @Test
    fun `test that download event is cleared when the download event is consumed`() =
        runTest {
            onDownloadOptionClick()
            underTest.consumeDownloadEvent()
            underTest.state.test {
                val state = awaitItem()
                assertThat(state.downloadEvent)
                    .isInstanceOf(StateEventWithContentConsumed::class.java)
            }
        }

    private suspend fun onDownloadOptionClick() {
        val menuItem = mock<MenuItem>()
        val optionsItemInfo =
            OptionsItemInfo(OptionItems.DOWNLOAD_CLICKED, emptyList(), emptyList())
        whenever(handleOptionClickMapper(eq(menuItem), any())).thenReturn(optionsItemInfo)
        underTest.onOptionItemClicked(menuItem)
    }

    @Test
    fun `test that download event is updated when on available offline option click is invoked`() =
        runTest {
            val triggered = TransferTriggerEvent.StartDownloadForOffline(
                node = mock(),
                withStartMessage = false
            )
            underTest.onDownloadFileTriggered(triggered)
            underTest.state.test {
                val state = awaitItem()
                assertThat(state.downloadEvent).isInstanceOf(StateEventWithContentTriggered::class.java)
                assertThat((state.downloadEvent as StateEventWithContentTriggered).content)
                    .isInstanceOf(TransferTriggerEvent.StartDownloadForOffline::class.java)
                assertThat((state.downloadEvent.content as TransferTriggerEvent.StartDownloadForOffline).withStartMessage)
                    .isFalse()
            }
        }

    @Test
    fun `test that download event is updated when on download option click is invoked`() =
        runTest {
            val triggered = TransferTriggerEvent.StartDownloadNode(
                nodes = listOf(mock()),
                withStartMessage = false
            )
            underTest.onDownloadFileTriggered(triggered)
            underTest.state.test {
                val state = awaitItem()
                assertThat(state.downloadEvent).isInstanceOf(StateEventWithContentTriggered::class.java)
                assertThat((state.downloadEvent as StateEventWithContentTriggered).content)
                    .isInstanceOf(TransferTriggerEvent.StartDownloadNode::class.java)
                assertThat((state.downloadEvent.content as TransferTriggerEvent.StartDownloadNode).withStartMessage)
                    .isFalse()
            }
        }

    @Test
    fun `test that download event is updated when on download for preview option click is invoked`() =
        runTest {
            val triggered =
                TransferTriggerEvent.StartDownloadForPreview(node = mock(), isOpenWith = false)
            underTest.onDownloadFileTriggered(triggered)
            underTest.state.test {
                val state = awaitItem()
                assertThat(state.downloadEvent).isInstanceOf(StateEventWithContentTriggered::class.java)
                assertThat((state.downloadEvent as StateEventWithContentTriggered).content)
                    .isInstanceOf(TransferTriggerEvent.StartDownloadForPreview::class.java)
            }
        }

    @ParameterizedTest
    @EnumSource(AccessPermission::class)
    fun `test that the correct access permission for an account is set`(accessPermission: AccessPermission) =
        runTest {
            val node = mock<PublicLinkFolder> { on { id } doReturn NodeId(1L) }
            whenever(
                getNodeAccessUseCase(nodeId = node.id)
            ) doReturn accessPermission

            val publicLinkNodes = listOf<PublicLinkNode>(node)
            monitorLinksChannel.send(publicLinkNodes)
            underTest.refreshLinkNodes()

            underTest.state.test {
                assertThat(
                    expectMostRecentItem().nodesList.first().accessPermission
                ).isEqualTo(accessPermission)
            }
        }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that the correct value is set indicating whether the node can be moved to the rubbish bin`(
        canBeMovedToRubbishBin: Boolean,
    ) = runTest {
        val node = mock<PublicLinkFolder> { on { id } doReturn NodeId(1L) }
        val rubbishBinNode = if (canBeMovedToRubbishBin) {
            mock<FileNode>()
        } else null
        whenever(getRubbishBinFolderUseCase()) doReturn rubbishBinNode
        whenever(
            checkNodeCanBeMovedToTargetNode(
                nodeId = any(),
                targetNodeId = any()
            )
        ) doReturn canBeMovedToRubbishBin
        whenever(
            getNodeAccessUseCase(nodeId = any())
        ) doReturn AccessPermission.UNKNOWN

        val publicLinkNodes = listOf<PublicLinkNode>(node)
        monitorLinksChannel.send(publicLinkNodes)
        underTest.refreshLinkNodes()

        underTest.state.test {
            assertThat(
                expectMostRecentItem().nodesList.first().canBeMovedToRubbishBin
            ).isEqualTo(canBeMovedToRubbishBin)
        }
    }
}
