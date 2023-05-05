package test.mega.privacy.android.app.domain.usecase

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.domain.usecase.GetFileBrowserChildrenUseCase
import mega.privacy.android.app.domain.usecase.GetNodeByHandle
import mega.privacy.android.app.domain.usecase.GetRootFolder
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.usecase.AddNodeType
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import nz.mega.sdk.MegaNode
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class GetFileBrowserChildrenUseCaseTest {
    private lateinit var underTest: GetFileBrowserChildrenUseCase

    private val getNodeByHandle: GetNodeByHandle = mock()
    private val getRootFolder: GetRootFolder = mock()
    private val getCloudSortOrder: GetCloudSortOrder = mock()
    private val nodeRepository: NodeRepository = mock()
    private val addNodeType: AddNodeType = mock()

    @Before
    fun setUp() {
        underTest = GetFileBrowserChildrenUseCase(
            getNodeByHandle = getNodeByHandle,
            getRootFolder = getRootFolder,
            getCloudSortOrder = getCloudSortOrder,
            nodeRepository = nodeRepository,
            addNodeType = addNodeType,
        )
    }

    @Test
    fun `test that when parent handle is -1 then it will invoke getRootFolder once`() = runTest {
        whenever(nodeRepository.getInvalidHandle()).thenReturn(-1L)
        underTest(-1)
        verify(getRootFolder, times(1)).invoke()
    }

    @Test
    fun `test that when parent handle is -1 then it will and rootFolder returns null megaNode returns empty list`() =
        runTest {
            whenever(nodeRepository.getInvalidHandle()).thenReturn(-1L)
            whenever(getRootFolder()).thenReturn(null)
            val list = underTest(-1)
            Truth.assertThat(list).isEmpty()
        }

    @Test
    fun `test that when parent handle is not -1 then it will invoke getNodeByHandle returns null megaNode returns empty list`() =
        runTest {
            whenever(nodeRepository.getInvalidHandle()).thenReturn(-1L)
            val handle = 1234L
            whenever(getNodeByHandle(handle)).thenReturn(null)
            val list = underTest(handle)
            Truth.assertThat(list).isEmpty()
        }

    @Test
    fun `test that when parent handle is not -1 then it will invoke getNodeByHandle returns megaNode megaNode returns some list`() =
        runTest {
            val handle = 1234L
            val megaNode: MegaNode = mock()
            whenever(nodeRepository.getInvalidHandle()).thenReturn(-1L)
            whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_DEFAULT_ASC)
            whenever(megaNode.handle).thenReturn(handle)
            whenever(getNodeByHandle(handle)).thenReturn(megaNode)
            whenever(
                nodeRepository.getNodeChildren(
                    nodeId = NodeId(handle),
                    getCloudSortOrder()
                )
            ).thenReturn(listOf(mock()))
            val list = underTest(handle)
            Truth.assertThat(list).isNotEmpty()
        }
}