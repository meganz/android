package mega.privacy.android.domain.usecase.rubbishbin

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.FolderNode
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
class GetRubbishBinNodeChildrenUseCaseTest {

    private lateinit var underTest: GetRubbishBinNodeChildrenUseCase
    private val nodeRepository: NodeRepository = mock()
    private val getCloudSortOrder: GetCloudSortOrder = mock()
    private val getRubbishBinFolderUseCase: GetRubbishBinFolderUseCase = mock()
    private val addNodeType: AddNodeType = mock()

    @Before
    fun setUp() {
        underTest = GetRubbishBinNodeChildrenUseCase(
            nodeRepository = nodeRepository,
            getCloudSortOrder = getCloudSortOrder,
            getRubbishBinFolderUseCase = getRubbishBinFolderUseCase,
            addNodeType = addNodeType
        )
    }

    @Test
    fun `test that invoke with -1L invoke getRubbishBinNode`() = runTest {
        whenever(nodeRepository.getInvalidHandle()).thenReturn(-1L)
        underTest(-1L)
        verify(getRubbishBinFolderUseCase).invoke()
    }

    @Test
    fun `test that invoke with -1L give null returns empty response`() = runTest {
        val parentHandle = -1L
        whenever(nodeRepository.getInvalidHandle()).thenReturn(parentHandle)
        underTest(parentHandle)
        whenever(getRubbishBinFolderUseCase()).thenReturn(null)
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
            listOf<FolderNode>(
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