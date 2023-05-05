package test.mega.privacy.android.app.domain.usecase

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.domain.usecase.DefaultGetRubbishBinChildren
import mega.privacy.android.app.domain.usecase.GetRubbishBinChildren
import mega.privacy.android.app.domain.usecase.GetRubbishBinFolder
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.usecase.AddNodeType
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class DefaultGetRubbishBinChildrenTest {

    private lateinit var underTest: GetRubbishBinChildren
    private val nodeRepository: NodeRepository = mock()
    private val getCloudSortOrder: GetCloudSortOrder = mock()
    private val getRubbishBinFolder: GetRubbishBinFolder = mock()
    private val addNodeType: AddNodeType = mock()

    @Before
    fun setUp() {
        underTest = DefaultGetRubbishBinChildren(
            nodeRepository = nodeRepository,
            getCloudSortOrder = getCloudSortOrder,
            getRubbishBinFolder = getRubbishBinFolder,
            addNodeType = addNodeType
        )
    }

    @Test
    fun `test that invoke with -1L invoke getRubbishBinNode`() = runTest {
        whenever(nodeRepository.getInvalidHandle()).thenReturn(-1L)
        underTest(-1L)
        verify(getRubbishBinFolder).invoke()
    }

    @Test
    fun `test that invoke with -1L give null returns empty response`() = runTest {
        val parentHandle = -1L
        whenever(nodeRepository.getInvalidHandle()).thenReturn(parentHandle)
        underTest(parentHandle)
        whenever(getRubbishBinFolder()).thenReturn(null)
        val list = underTest(-1L)
        Truth.assertThat(list).isEmpty()
    }

    @Test
    fun `test that invoke without -1L give any list response`() = runTest {
        whenever(nodeRepository.getInvalidHandle()).thenReturn(-1L)
        val nodeId = NodeId(0L)
        val sortOrder = SortOrder.ORDER_DEFAULT_ASC
        whenever(getCloudSortOrder()).thenReturn(sortOrder)
        whenever(nodeRepository.getNodeChildren(nodeId, sortOrder)).thenReturn(
            listOf(
                mock(),
                mock()
            )
        )
        val list = underTest(0L)
        Truth.assertThat(list).isNotEmpty()
    }

    @Test
    fun `test that invoke without -1L give any empty list response`() = runTest {
        whenever(nodeRepository.getInvalidHandle()).thenReturn(-1L)
        val nodeId = NodeId(0L)
        val sortOrder = SortOrder.ORDER_DEFAULT_ASC
        whenever(getCloudSortOrder()).thenReturn(sortOrder)
        whenever(nodeRepository.getNodeChildren(nodeId, sortOrder)).thenReturn(
            emptyList()
        )
        val list = underTest(0L)
        Truth.assertThat(list).isEmpty()
    }
}