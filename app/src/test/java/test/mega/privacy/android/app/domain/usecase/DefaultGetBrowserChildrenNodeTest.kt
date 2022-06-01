package test.mega.privacy.android.app.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.domain.usecase.*
import nz.mega.sdk.MegaApiJava
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
        underTest(-1L)

        verify(getChildrenNode).invoke(result, MegaApiJava.ORDER_NONE)
    }

    @Test
    fun `test that -1L invoke getChildrenNode with result of getNodeByHandle`() = runTest {
        val result = mock<MegaNode> {}
        val parentHandle = 0L
        whenever(getNodeByHandle(parentHandle)).thenReturn(result)
        underTest(parentHandle)

        verify(getChildrenNode).invoke(result, MegaApiJava.ORDER_NONE)
    }

    @Test
    fun `test that underTest is invoked with value of get order sort management`() = runTest {
        val sortOrder = MegaApiJava.ORDER_DEFAULT_ASC
        whenever(getCloudSortOrder()).thenReturn(sortOrder)
        whenever(getRootFolder()).thenReturn(mock())
        underTest(-1L)

        verify(getChildrenNode).invoke(any(), eq(sortOrder))
    }
}