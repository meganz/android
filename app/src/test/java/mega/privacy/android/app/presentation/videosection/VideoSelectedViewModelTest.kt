package mega.privacy.android.app.presentation.videosection

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.presentation.data.NodeUIItem
import mega.privacy.android.app.presentation.videosection.VideoSelectedViewModel
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.node.UnTypedNode
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import mega.privacy.android.domain.usecase.GetParentNodeUseCase
import mega.privacy.android.domain.usecase.GetRootNodeUseCase
import mega.privacy.android.domain.usecase.filebrowser.GetFileBrowserNodeChildrenUseCase
import mega.privacy.android.domain.usecase.viewtype.MonitorViewType
import mega.privacy.android.domain.usecase.viewtype.SetViewType
import mega.privacy.android.legacy.core.ui.model.SearchWidgetState
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import org.mockito.kotlin.wheneverBlocking
import kotlin.time.Duration.Companion.seconds

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(CoroutineMainDispatcherExtension::class)
class VideoSelectedViewModelTest {
    private lateinit var underTest: VideoSelectedViewModel

    private val getRootNodeUseCase = mock<GetRootNodeUseCase>()
    private val getCloudSortOrder = mock<GetCloudSortOrder>()
    private val getParentNodeUseCase = mock<GetParentNodeUseCase>()
    private val getFileBrowserNodeChildrenUseCase = mock<GetFileBrowserNodeChildrenUseCase>()
    private val setViewType = mock<SetViewType>()
    private val monitorViewType = mock<MonitorViewType>()

    private val fakeMonitorViewTypeFlow = MutableSharedFlow<ViewType>()

    private val testNode = mock<TypedFileNode> {
        on { name }.thenReturn("testNode")
        on { type }.thenReturn(VideoFileTypeInfo("", "", 0.seconds))
    }
    private val testFolderNode = mock<TypedFolderNode> {
        on { name }.thenReturn("testNode")
        on { id }.thenReturn(NodeId(1L))
    }
    private val nodeId = mock<NodeId> {
        on { longValue }.thenReturn(2L)
    }
    private val uiItem: NodeUIItem<TypedNode> = NodeUIItem(testNode, isSelected = false)
    private val uiFolderItem: NodeUIItem<TypedNode> = NodeUIItem(testFolderNode, isSelected = false)
    private val sortOrder = SortOrder.ORDER_DEFAULT_ASC

    private val unTypedNode: UnTypedNode = mock<FolderNode> {
        on { name }.thenReturn("parent name")
        on { id }.thenReturn(nodeId)
    }

    @BeforeEach
    fun setUp() {
        wheneverBlocking { monitorViewType() }.thenReturn(fakeMonitorViewTypeFlow)
        wheneverBlocking { getFileBrowserNodeChildrenUseCase(any()) }.thenReturn(emptyList())
        wheneverBlocking { getCloudSortOrder() }.thenReturn(SortOrder.ORDER_NONE)
        initUnderTest()
    }

    private fun initUnderTest() {
        underTest = VideoSelectedViewModel(
            getRootNodeUseCase = getRootNodeUseCase,
            getCloudSortOrder = getCloudSortOrder,
            getParentNodeUseCase = getParentNodeUseCase,
            getFileBrowserNodeChildrenUseCase = getFileBrowserNodeChildrenUseCase,
            setViewType = setViewType,
            monitorViewType = monitorViewType
        )
    }

    @AfterEach
    fun resetMocks() {
        reset(
            getRootNodeUseCase,
            getCloudSortOrder,
            getParentNodeUseCase,
            getFileBrowserNodeChildrenUseCase,
            setViewType,
            monitorViewType
        )
    }

    @Test
    fun `test that the initial state is returned`() = runTest {
        initUnderTest()
        underTest.state.test {
            val initial = awaitItem()
            assertThat(initial.currentViewType).isEqualTo(ViewType.LIST)
            assertThat(initial.sortOrder).isEqualTo(SortOrder.ORDER_NONE)
            assertThat(initial.isLoading).isFalse()
            assertThat(initial.nodesList).isEmpty()
            assertThat(initial.currentFolderHandle).isEqualTo(-1L)
            assertThat(initial.openedFolderNodeHandles).isEmpty()
            assertThat(initial.topBarTitle).isNull()
            assertThat(initial.query).isNull()
            assertThat(initial.searchState).isEqualTo(SearchWidgetState.COLLAPSED)
            assertThat(initial.selectedNodeHandles).isEmpty()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that the nodes return correctly after init`() = runTest {
        val sortOrder = SortOrder.ORDER_DEFAULT_ASC

        whenever(getFileBrowserNodeChildrenUseCase(any())).thenReturn(listOf(testNode))
        whenever(getCloudSortOrder()).thenReturn(sortOrder)

        initUnderTest()

        underTest.state.test {
            val actual = awaitItem()
            assertThat(actual.nodesList).isNotEmpty()
            assertThat(actual.nodesList.size).isEqualTo(1)
            assertThat(actual.isLoading).isFalse()
            assertThat(actual.topBarTitle).isNull()
            assertThat(actual.currentFolderHandle).isEqualTo(-1L)
            assertThat(actual.openedFolderNodeHandles).isEmpty()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that the state is updated correctly after a file is clicked`() = runTest {
        whenever(getFileBrowserNodeChildrenUseCase(any())).thenReturn(listOf(testNode))
        whenever(getCloudSortOrder()).thenReturn(sortOrder)

        initUnderTest()
        underTest.itemClicked(uiItem)

        underTest.state.test {
            val actual = awaitItem()
            assertThat(actual.nodesList).isNotEmpty()
            assertThat(actual.nodesList.size).isEqualTo(1)
            assertThat(actual.nodesList[0].isSelected).isTrue()
            assertThat(actual.selectedNodeHandles).isNotEmpty()
            assertThat(actual.selectedNodeHandles.size).isEqualTo(1)
        }
    }

    @Test
    fun `test that the state is updated correctly after a folder is clicked`() = runTest {
        whenever(getFileBrowserNodeChildrenUseCase(any())).thenReturn(listOf(testFolderNode))
        whenever(getCloudSortOrder()).thenReturn(sortOrder)

        initUnderTest()
        underTest.itemClicked(uiFolderItem)

        underTest.state.test {
            val actual = awaitItem()
            assertThat(actual.nodesList).isNotEmpty()
            assertThat(actual.nodesList.size).isEqualTo(1)
            assertThat(actual.isLoading).isFalse()
            assertThat(actual.sortOrder).isEqualTo(SortOrder.ORDER_DEFAULT_ASC)
            assertThat(actual.topBarTitle).isEqualTo(testFolderNode.name)
            assertThat(actual.currentFolderHandle).isEqualTo(testFolderNode.id.longValue)
            assertThat(actual.openedFolderNodeHandles).isNotEmpty()
            assertThat(actual.openedFolderNodeHandles.size).isEqualTo(1)
        }
    }

    @Test
    fun `test that the state is updated correctly after back to parent folder`() = runTest {
        whenever(getFileBrowserNodeChildrenUseCase(any())).thenReturn(listOf(testNode))
        whenever(getParentNodeUseCase(NodeId(-1))).thenReturn(unTypedNode)
        whenever(getRootNodeUseCase()).thenReturn(null)
        whenever(getCloudSortOrder()).thenReturn(sortOrder)

        initUnderTest()
        underTest.backToParentFolder()

        underTest.state.test {
            val actual = awaitItem()
            assertThat(actual.nodesList).isNotEmpty()
            assertThat(actual.nodesList.size).isEqualTo(1)
            assertThat(actual.isLoading).isFalse()
            assertThat(actual.sortOrder).isEqualTo(SortOrder.ORDER_DEFAULT_ASC)
            assertThat(actual.topBarTitle).isEqualTo(unTypedNode.name)
            assertThat(actual.currentFolderHandle).isEqualTo(nodeId.longValue)
        }
    }

    @Test
    fun `test that the state is updated correctly after back to root folder`() = runTest {
        whenever(getFileBrowserNodeChildrenUseCase(any())).thenReturn(listOf(testNode))
        whenever(getParentNodeUseCase(NodeId(-1))).thenReturn(unTypedNode)
        whenever(getRootNodeUseCase()).thenReturn(unTypedNode)
        whenever(getCloudSortOrder()).thenReturn(sortOrder)

        initUnderTest()
        underTest.backToParentFolder()

        underTest.state.test {
            val actual = awaitItem()
            assertThat(actual.nodesList).isNotEmpty()
            assertThat(actual.nodesList.size).isEqualTo(1)
            assertThat(actual.isLoading).isFalse()
            assertThat(actual.sortOrder).isEqualTo(SortOrder.ORDER_DEFAULT_ASC)
            assertThat(actual.topBarTitle).isNull()
            assertThat(actual.currentFolderHandle).isEqualTo(-1)
        }
    }

    @Test
    fun `test that the state is updated correctly after select all`() = runTest {
        whenever(getFileBrowserNodeChildrenUseCase(any())).thenReturn(listOf(testNode))
        whenever(getCloudSortOrder()).thenReturn(sortOrder)

        initUnderTest()
        underTest.selectAllVideos()

        underTest.state.test {
            val actual = awaitItem()
            assertThat(actual.nodesList).isNotEmpty()
            assertThat(actual.nodesList.size).isEqualTo(1)
            assertThat(actual.nodesList[0].isSelected).isTrue()
            assertThat(actual.selectedNodeHandles).isNotEmpty()
            assertThat(actual.selectedNodeHandles.size).isEqualTo(1)
        }
    }

    @Test
    fun `test that the state is updated correctly after clear all`() = runTest {
        whenever(getFileBrowserNodeChildrenUseCase(any())).thenReturn(listOf(testNode))
        whenever(getCloudSortOrder()).thenReturn(sortOrder)

        initUnderTest()
        underTest.selectAllVideos()
        underTest.clearAllSelectedVideos()

        underTest.state.test {
            val actual = awaitItem()
            assertThat(actual.nodesList).isNotEmpty()
            assertThat(actual.nodesList.size).isEqualTo(1)
            assertThat(actual.nodesList[0].isSelected).isFalse()
            assertThat(actual.selectedNodeHandles).isEmpty()
        }
    }

    @Test
    fun `test that the state is updated correctly after order changed`() = runTest {
        val newSortOrder = SortOrder.ORDER_FAV_ASC
        whenever(getFileBrowserNodeChildrenUseCase(any())).thenReturn(listOf(testNode))
        whenever(getCloudSortOrder()).thenReturn(newSortOrder)

        initUnderTest()
        underTest.refreshWhenOrderChanged()

        underTest.state.test {
            val actual = awaitItem()
            assertThat(actual.nodesList).isNotEmpty()
            assertThat(actual.sortOrder).isEqualTo(newSortOrder)
        }
    }

    @Test
    fun `test that the state is updated correctly after searchWidgetStateUpdate is called`() =
        runTest {
            initUnderTest()
            underTest.searchWidgetStateUpdate()

            underTest.state.test {
                val actual = awaitItem()
                assertThat(actual.searchState).isEqualTo(SearchWidgetState.EXPANDED)
            }
        }

    @Test
    fun `test that the state is updated correctly after searchQuery is called`() = runTest {
        val testNode1 = mock<TypedFileNode> {
            on { name }.thenReturn("123")
            on { type }.thenReturn(VideoFileTypeInfo("", "", 0.seconds))
        }
        val testNode2 = mock<TypedFileNode> {
            on { name }.thenReturn("abc")
            on { type }.thenReturn(VideoFileTypeInfo("", "", 0.seconds))
        }
        whenever(getFileBrowserNodeChildrenUseCase(any())).thenReturn(
            listOf(testNode1, testNode2)
        )
        whenever(getCloudSortOrder()).thenReturn(sortOrder)

        initUnderTest()
        underTest.searchQuery(testNode2.name)

        underTest.state.test {
            val actual = awaitItem()
            assertThat(actual.nodesList).isNotEmpty()
            assertThat(actual.nodesList.size).isEqualTo(1)
            assertThat(actual.nodesList[0].name).isEqualTo(testNode2.name)
        }
    }
}