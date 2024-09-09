package mega.privacy.android.domain.usecase.photos.mediadiscovery

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.AudioFileTypeInfo
import mega.privacy.android.domain.entity.FileTypeInfo
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.StaticImageFileTypeInfo
import mega.privacy.android.domain.entity.UnMappedFileTypeInfo
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import mega.privacy.android.domain.usecase.GetRootNodeUseCase
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class ShouldEnterMediaDiscoveryModeUseCaseTest {
    private lateinit var underTest: ShouldEnterMediaDiscoveryModeUseCase

    private val getRootNodeUseCase = mock<GetRootNodeUseCase>()
    private val getCloudSortOrder = mock<GetCloudSortOrder>()
    private val nodeRepository = mock<NodeRepository>()

    @Before
    fun setUp() {
        underTest = ShouldEnterMediaDiscoveryModeUseCase(
            getRootNodeUseCase = getRootNodeUseCase,
            getCloudSortOrder = getCloudSortOrder,
            nodeRepository = nodeRepository,
        )
    }

    @Test
    fun `test that media discovery cannot be entered parentHandle is invalid`() = runTest {
        val parentHandle = -1L
        val fileTypeInfo = StaticImageFileTypeInfo("", "")
        val nodeTypes: List<FileTypeInfo> = listOf(fileTypeInfo)
        whenever(nodeRepository.getInvalidHandle()).thenReturn(-1L)
        whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_DEFAULT_ASC)
        whenever(
            nodeRepository.getNodeChildrenFileTypes(
                nodeId = NodeId(parentHandle),
                getCloudSortOrder()
            )
        ).thenReturn(nodeTypes)
        val shouldEnter = underTest.invoke(parentHandle)
        assertThat(shouldEnter).isFalse()
    }

    @Test
    fun `test that media discovery can be entered when there are media nodes`() = runTest {
        val parentHandle = 1234L
        val fileTypeInfo = StaticImageFileTypeInfo("", "")
        val nodeTypes: List<FileTypeInfo> = listOf(fileTypeInfo)
        whenever(nodeRepository.getInvalidHandle()).thenReturn(-1L)
        whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_DEFAULT_ASC)
        whenever(
            nodeRepository.getNodeChildrenFileTypes(
                nodeId = NodeId(parentHandle),
                getCloudSortOrder()
            )
        ).thenReturn(nodeTypes)
        val shouldEnter = underTest.invoke(parentHandle)
        assertThat(shouldEnter).isTrue()
    }

    @Test
    fun `test that media discovery cannot be entered when there is no media node`() = runTest {
        val parentHandle = 1234L
        val fileTypeInfo = AudioFileTypeInfo("", "", mock())
        val nodeTypes: List<FileTypeInfo> = listOf(fileTypeInfo)
        whenever(nodeRepository.getInvalidHandle()).thenReturn(-1L)
        whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_DEFAULT_ASC)
        whenever(
            nodeRepository.getNodeChildrenFileTypes(
                nodeId = NodeId(parentHandle),
                getCloudSortOrder()
            )
        ).thenReturn(nodeTypes)
        val shouldEnter = underTest.invoke(parentHandle)
        assertThat(shouldEnter).isFalse()
    }

    @Test
    fun `test that media discovery cannot be entered when a folder node is found`() =
        runTest {
            val parentHandle = 1234L
            val fileTypeInfo = UnMappedFileTypeInfo("")
            val nodeTypes: List<FileTypeInfo> = listOf(fileTypeInfo)
            whenever(nodeRepository.getInvalidHandle()).thenReturn(-1L)
            whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_DEFAULT_ASC)
            whenever(
                nodeRepository.getNodeChildrenFileTypes(
                    nodeId = NodeId(parentHandle),
                    getCloudSortOrder()
                )
            ).thenReturn(nodeTypes)
            val shouldEnter = underTest.invoke(parentHandle)
            assertThat(shouldEnter).isFalse()
        }
}
