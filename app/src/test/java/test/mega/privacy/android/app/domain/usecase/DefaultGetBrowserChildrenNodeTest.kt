package test.mega.privacy.android.app.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.domain.usecase.DefaultGetBrowserChildrenNode
import mega.privacy.android.app.domain.usecase.GetBrowserChildrenNode
import mega.privacy.android.app.domain.usecase.GetChildrenNode
import mega.privacy.android.app.domain.usecase.GetNodeByHandle
import mega.privacy.android.app.domain.usecase.GetRootFolder
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import nz.mega.sdk.MegaNode
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultGetBrowserChildrenNodeTest {

    private lateinit var underTest: GetBrowserChildrenNode
    private val getNodeByHandle = mock<GetNodeByHandle>()
    private val getChildrenNode = mock<GetChildrenNode>()
    private val getRootFolder = mock<GetRootFolder>()
    private val getCloudSortOrder = mock<GetCloudSortOrder>()

    @Before
    fun setUp() {
        underTest = DefaultGetBrowserChildrenNode(
            getNodeByHandle,
            getChildrenNode,
            getRootFolder,
            getCloudSortOrder
        )
    }

    @Test
    fun `test that invoke with -1L invoke getRootNode`() = runTest {
        underTest(-1L)

        verify(getRootFolder).invoke()
    }

    @Test
    fun `test that invoke with value except -1L invoke getNodeByHandle`() = runTest {
        val parentHandle = 0L
        underTest(parentHandle)

        verify(getNodeByHandle).invoke(parentHandle)
    }

    @Test
    fun `test that -1L invoke getChildrenNode with result of getRootNode`() = runTest {
        val result = mock<MegaNode> {}
        whenever(getRootFolder()).thenReturn(result)
        whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_DEFAULT_ASC.value)
        underTest(-1L)

        verify(getChildrenNode).invoke(eq(result), any())
    }

    @Test
    fun `test that -1L invoke getChildrenNode with result of getNodeByHandle`() = runTest {
        val result = mock<MegaNode> {}
        val parentHandle = 0L
        whenever(getNodeByHandle(parentHandle)).thenReturn(result)
        whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_DEFAULT_ASC.value)
        underTest(parentHandle)

        verify(getChildrenNode).invoke(eq(result), any())
    }

    @Test
    fun `test that underTest is invoked with value of get order sort management`() = runTest {
        val sortOrder = SortOrder.ORDER_DEFAULT_ASC.value
        whenever(getCloudSortOrder()).thenReturn(sortOrder)
        whenever(getRootFolder()).thenReturn(mock())
        underTest(-1L)

        verify(getChildrenNode).invoke(any(), eq(sortOrder))
    }
}