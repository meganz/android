package mega.privacy.android.app.presentation.search.model

import app.cash.turbine.test
import com.google.common.truth.Truth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.domain.usecase.MonitorNodeUpdates
import mega.privacy.android.app.main.DrawerItem
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.search.SearchCategory
import mega.privacy.android.domain.usecase.GetInboxNodeUseCase
import mega.privacy.android.domain.usecase.GetParentNodeHandle
import mega.privacy.android.domain.usecase.GetRootNodeUseCase
import mega.privacy.android.domain.usecase.GetRubbishNodeUseCase
import mega.privacy.android.domain.usecase.favourites.IsAvailableOfflineUseCase
import mega.privacy.android.domain.usecase.node.GetNodeByHandleUseCase
import mega.privacy.android.domain.usecase.search.IncomingSharesTabSearchUseCase
import mega.privacy.android.domain.usecase.search.LinkSharesTabSearchUseCase
import mega.privacy.android.domain.usecase.search.OutgoingSharesTabSearchUseCase
import mega.privacy.android.domain.usecase.search.SearchInNodesUseCase
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import test.mega.privacy.android.app.presentation.shares.FakeMonitorUpdates
import java.util.stream.Stream

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SearchActivityViewModelTest {
    private lateinit var underTest: SearchActivityViewModel
    private val monitorNodeUpdates: MonitorNodeUpdates = FakeMonitorUpdates()
    private val incomingSharesTabSearchUseCase: IncomingSharesTabSearchUseCase = mock()
    private val outgoingSharesTabSearchUseCase: OutgoingSharesTabSearchUseCase = mock()
    private val linkSharesTabSearchUseCase: LinkSharesTabSearchUseCase = mock()
    private val searchInNodesUseCase: SearchInNodesUseCase = mock()
    private val getRootNodeUseCase: GetRootNodeUseCase = mock()
    private val getNodeByHandleUseCase: GetNodeByHandleUseCase = mock()
    private val getRubbishNodeUseCase: GetRubbishNodeUseCase = mock()
    private val getInboxNodeUseCase: GetInboxNodeUseCase = mock()
    private val getParentNodeHandle: GetParentNodeHandle = mock()
    private val isAvailableOfflineUseCase: IsAvailableOfflineUseCase = mock()

    @BeforeAll
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        underTest = SearchActivityViewModel(
            monitorNodeUpdates = monitorNodeUpdates,
            incomingSharesTabSearchUseCase = incomingSharesTabSearchUseCase,
            outgoingSharesTabSearchUseCase = outgoingSharesTabSearchUseCase,
            linkSharesTabSearchUseCase = linkSharesTabSearchUseCase,
            searchInNodesUseCase = searchInNodesUseCase,
            getRootNodeUseCase = getRootNodeUseCase,
            getNodeByHandleUseCase = getNodeByHandleUseCase,
            getRubbishNodeUseCase = getRubbishNodeUseCase,
            getInboxNodeUseCase = getInboxNodeUseCase,
            getParentNodeHandle = getParentNodeHandle,
            isAvailableOfflineUseCase = isAvailableOfflineUseCase
        )
    }

    @ParameterizedTest(name = "Test query {0} which returns list")
    @MethodSource("provideParams")
    fun `test when search a query with valid handle`(
        query: String,
        isFirstLevel: Boolean,
        currentTab: DrawerItem?,
        sharesTab: Int,
        currentHandle: Long,
    ) = runTest {
        underTest.updateSearchHandle(1)
        val node: Node = mock()
        val unTypedNode: FileNode = mock()
        val searchNodeId: NodeId = mock()
        whenever(getParentNodeHandle(currentHandle)).thenReturn(currentHandle)
        whenever(getRootNodeUseCase()).thenReturn(node)
        whenever(getNodeByHandleUseCase(underTest.state.value.parentHandle)).thenReturn(unTypedNode)
        whenever(getRubbishNodeUseCase()).thenReturn(unTypedNode)
        whenever(getInboxNodeUseCase()).thenReturn(unTypedNode)

        whenever(node.id).thenReturn(searchNodeId)
        whenever(unTypedNode.id).thenReturn(searchNodeId)

        val nodeHandles = listOf(1L, 2L)
        val nodeName = listOf("abc", "xyz")
        val typedFolderNodes = nodeHandles.mapIndexed { index, nodeId ->
            mock<TypedFolderNode> {
                on { id }.thenReturn(NodeId(nodeId))
                on { name }.thenReturn(nodeName[index])
            }
        }
        whenever(incomingSharesTabSearchUseCase(query)).thenReturn(typedFolderNodes)
        whenever(outgoingSharesTabSearchUseCase(query)).thenReturn(typedFolderNodes)
        whenever(linkSharesTabSearchUseCase(query, isFirstLevel)).thenReturn(typedFolderNodes)
        whenever(
            searchInNodesUseCase(
                nodeId = searchNodeId,
                query = query,
                searchCategory = SearchCategory.ALL
            )
        ).thenReturn(typedFolderNodes)
        whenever(isAvailableOfflineUseCase(any())).thenReturn(false)

        underTest.performSearch(
            query = query,
            isFirstLevel = isFirstLevel,
            currentTab = currentTab,
            sharesTab = sharesTab,
            currentHandle = currentHandle
        )
        underTest.state.test {
            val item = awaitItem()
            Truth.assertThat(item.searchItemList).hasSize(2)
        }
    }

    @ParameterizedTest(name = "Test query {0} which returns list")
    @MethodSource("provideParams")
    fun `test when search a query with in valid handle`(
        query: String,
        isFirstLevel: Boolean,
        currentTab: DrawerItem?,
        sharesTab: Int,
        currentHandle: Long,
    ) = runTest {
        underTest.updateSearchHandle(-1)
        val node: Node = mock()
        val unTypedNode: FileNode = mock()
        val searchNodeId: NodeId = mock()
        whenever(getParentNodeHandle(currentHandle)).thenReturn(currentHandle)
        whenever(getRootNodeUseCase()).thenReturn(node)
        whenever(getNodeByHandleUseCase(underTest.state.value.parentHandle)).thenReturn(unTypedNode)
        whenever(getRubbishNodeUseCase()).thenReturn(unTypedNode)
        whenever(getInboxNodeUseCase()).thenReturn(unTypedNode)

        whenever(node.id).thenReturn(searchNodeId)
        whenever(unTypedNode.id).thenReturn(searchNodeId)

        val nodeHandles = listOf(1L, 2L)
        val nodeName = listOf("abc", "xyz")
        val typedFolderNodes = nodeHandles.mapIndexed { index, nodeId ->
            mock<TypedFolderNode> {
                on { id }.thenReturn(NodeId(nodeId))
                on { name }.thenReturn(nodeName[index])
            }
        }
        whenever(incomingSharesTabSearchUseCase(query)).thenReturn(typedFolderNodes)
        whenever(outgoingSharesTabSearchUseCase(query)).thenReturn(typedFolderNodes)
        whenever(linkSharesTabSearchUseCase(query, isFirstLevel)).thenReturn(typedFolderNodes)
        whenever(
            searchInNodesUseCase(
                nodeId = searchNodeId,
                query = query,
                searchCategory = SearchCategory.ALL
            )
        ).thenReturn(typedFolderNodes)
        underTest.performSearch(
            query = query,
            isFirstLevel = isFirstLevel,
            currentTab = currentTab,
            sharesTab = sharesTab,
            currentHandle = currentHandle
        )
        underTest.state.test {
            val item = awaitItem()
            Truth.assertThat(item.searchItemList).hasSize(2)
        }
    }

    @ParameterizedTest(name = "Test query {0} which returns list")
    @MethodSource("provideParams")
    fun `test when search a query with in valid handle for shares tab`(
        query: String,
        isFirstLevel: Boolean,
        currentTab: DrawerItem?,
        sharesTab: Int,
        currentHandle: Long,
    ) = runTest {
        underTest.updateSearchHandle(-1)
        val node: Node = mock()
        val unTypedNode: FileNode = mock()
        val searchNodeId: NodeId = mock()
        whenever(getParentNodeHandle(currentHandle)).thenReturn(currentHandle)
        whenever(getRootNodeUseCase()).thenReturn(node)
        whenever(getNodeByHandleUseCase(underTest.state.value.parentHandle)).thenReturn(unTypedNode)
        whenever(getRubbishNodeUseCase()).thenReturn(unTypedNode)
        whenever(getInboxNodeUseCase()).thenReturn(unTypedNode)

        whenever(node.id).thenReturn(searchNodeId)
        whenever(unTypedNode.id).thenReturn(searchNodeId)

        val nodeHandles = listOf(1L, 2L)
        val nodeName = listOf("abc", "xyz")
        val typedFolderNodes = nodeHandles.mapIndexed { index, nodeId ->
            mock<TypedFolderNode> {
                on { id }.thenReturn(NodeId(nodeId))
                on { name }.thenReturn(nodeName[index])
            }
        }
        whenever(incomingSharesTabSearchUseCase(query)).thenReturn(typedFolderNodes)
        whenever(outgoingSharesTabSearchUseCase(query)).thenReturn(typedFolderNodes)
        whenever(linkSharesTabSearchUseCase(query, isFirstLevel)).thenReturn(typedFolderNodes)
        whenever(
            searchInNodesUseCase(
                nodeId = searchNodeId,
                query = query,
                searchCategory = SearchCategory.ALL
            )
        ).thenReturn(typedFolderNodes)
        underTest.performSearch(
            query = query,
            isFirstLevel = isFirstLevel,
            currentTab = currentTab,
            sharesTab = sharesTab,
            currentHandle = currentHandle
        )
        underTest.state.test {
            val item = awaitItem()
            Truth.assertThat(item.searchItemList).hasSize(2)
        }
    }

    private fun provideParams(): Stream<Arguments> = Stream.of(
        Arguments.of("Query", false, null, 0, 1),
        Arguments.of("Query", false, DrawerItem.HOMEPAGE, -1, 1),
        Arguments.of("Query", false, DrawerItem.CLOUD_DRIVE, -1, 1),
        Arguments.of("Query", false, DrawerItem.SHARED_ITEMS, -1, 1),
        Arguments.of("Query", false, DrawerItem.SHARED_ITEMS, 0, 1),
        Arguments.of("Query", false, DrawerItem.SHARED_ITEMS, 1, 1),
        Arguments.of("Query", false, DrawerItem.SHARED_ITEMS, 2, 1),
        Arguments.of("Query", false, null, -1, 1),
        Arguments.of("Query", false, DrawerItem.RUBBISH_BIN, -1, 1),
        Arguments.of("Query", false, DrawerItem.INBOX, -1, 1)
    )

    @AfterAll
    fun tearDown() {
        Dispatchers.resetMain()
    }
}
