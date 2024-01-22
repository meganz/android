package mega.privacy.android.app.presentation.shares.links

import android.view.MenuItem
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.StateEventWithContentConsumed
import de.palm.composestateevents.StateEventWithContentTriggered
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.app.presentation.clouddrive.OptionItems
import mega.privacy.android.app.presentation.data.NodeUIItem
import mega.privacy.android.app.presentation.mapper.HandleOptionClickMapper
import mega.privacy.android.app.presentation.mapper.OptionsItemInfo
import mega.privacy.android.app.presentation.shares.links.model.LinksUiState
import mega.privacy.android.app.presentation.transfers.startdownload.model.TransferTriggerEvent
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.publiclink.PublicLinkFile
import mega.privacy.android.domain.entity.node.publiclink.PublicLinkFolder
import mega.privacy.android.domain.entity.node.publiclink.PublicLinkNode
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import mega.privacy.android.domain.usecase.GetLinksSortOrder
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.node.publiclink.MonitorPublicLinksUseCase
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
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
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase = mock()

    @BeforeAll
    internal fun initialise() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        runBlocking {
            stubCommon()
        }
    }

    @BeforeEach
    internal fun setUp() {
        underTest = LinksViewModel(
            monitorPublicLinksUseCase = monitorPublicLinksUseCase,
            getCloudSortOrder = getCloudSortOrder,
            getLinksSortOrder = getLinksSortOrder,
            monitorConnectivityUseCase = monitorConnectivityUseCase,
            handleOptionClickMapper = handleOptionClickMapper,
            getFeatureFlagValueUseCase = getFeatureFlagValueUseCase
        )
    }

    private suspend fun stubCommon() {
        whenever(getLinksSortOrder()).thenReturn(SortOrder.ORDER_NONE)
        whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_NONE)
        whenever(monitorConnectivityUseCase()).thenReturn(emptyFlow())
        whenever(getFeatureFlagValueUseCase(any())).thenReturn(true)
    }

    @AfterAll
    internal fun tearDown() {
        Dispatchers.resetMain()
        reset(
            monitorPublicLinksUseCase,
            getCloudSortOrder,
            getLinksSortOrder,
            monitorConnectivityUseCase,
            handleOptionClickMapper,
            getFeatureFlagValueUseCase,
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
    internal fun `test that calling onItemClicked with folder node returns the children`() =
        runTest {
            val publicLinkNodes = listOf<PublicLinkFolder>(mock())
            val flow = flow {
                emit(publicLinkNodes)
                awaitCancellation()
            }
            val parentNode = mock<PublicLinkFolder> {
                on { id }.thenReturn(NodeId(12))
                on { children }.thenReturn(flow)
            }

            underTest.onItemClicked(
                NodeUIItem(
                    parentNode,
                    isSelected = true,
                    isInvisible = false
                )
            )

            underTest.state.test {
                val expected = awaitItem()
                assertThat(expected.parentNode?.id).isEqualTo(NodeId(12))
                assertThat(expected.nodesList).hasSize(1)
            }
        }

    @Test
    internal fun `test that updates from the root are ignored while children are displayed`() =
        runTest {
            val publicLinkNodes = listOf<PublicLinkFolder>(mock())
            val flow = flow {
                emit(publicLinkNodes)
                awaitCancellation()
            }
            val parentNode = mock<PublicLinkFolder> {
                on { children }.thenReturn(flow)
            }

            monitorLinksChannel.send(emptyList())
            underTest.state.test {
                assertThat(awaitItem().nodesList).isEmpty()
            }

            underTest.onItemClicked(
                NodeUIItem(
                    parentNode,
                    isSelected = true,
                    isInvisible = false
                )
            )

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
            val publicLinkNodes = listOf<PublicLinkFolder>(mock())
            val flow = flow {
                emit(publicLinkNodes)
                awaitCancellation()
            }
            val parentNode = mock<PublicLinkFolder> {
                on { parent }.thenReturn(null)
                on { children }.thenReturn(flow)
                on { id }.thenReturn(NodeId(12))
            }

            val currentFolder = mock<PublicLinkFolder> {
                on { parent }.thenReturn(parentNode)
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
                assertThat(state.selectedNodeHandles.size).isEqualTo(0)
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
                assertThat(state.selectedNodeHandles.size).isEqualTo(1)
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
                assertThat(state.selectedNodeHandles.size).isEqualTo(2)
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
                .isEqualTo(underTest.state.value.selectedNodeHandles.size)
        }

    @Test
    fun `test that the selected node handles is empty when clearing all nodes`() = runTest {
        underTest.clearAllNodes()
        underTest.state.test {
            val state = awaitItem()
            assertThat(state.selectedNodeHandles).isEmpty()
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
        whenever(getFeatureFlagValueUseCase(AppFeatures.DownloadWorker)).thenReturn(true)
        underTest.onOptionItemClicked(menuItem)
    }

}