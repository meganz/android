package mega.privacy.android.domain.usecase

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.offline.OfflineFileInformation
import mega.privacy.android.domain.entity.offline.OfflineNodeInformation
import mega.privacy.android.domain.entity.offline.OtherOfflineNodeInformation
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.usecase.offline.GetOfflineFileInformationUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetOfflineNodesByParentIdUseCaseTest {

    private lateinit var underTest: GetOfflineNodesByParentIdUseCase

    private val nodeRepository = mock<NodeRepository>()
    private val getOfflineFileInformationUseCase = mock<GetOfflineFileInformationUseCase>()
    private val sortOfflineInfoUseCase = mock<SortOfflineInfoUseCase>()

    @BeforeEach
    fun setUp() {
        underTest = GetOfflineNodesByParentIdUseCase(
            nodeRepository = nodeRepository,
            getOfflineFileInformationUseCase = getOfflineFileInformationUseCase,
            sortOfflineInfoUseCase = sortOfflineInfoUseCase,
        )
    }

    @AfterEach
    fun resetMocks() {
        reset(nodeRepository, getOfflineFileInformationUseCase, sortOfflineInfoUseCase)
    }

    @Test
    fun `test that offline nodes are retrieved and sorted when parentId is provided`() = runTest {
        val parentId = 1
        val mockOfflineNodes = listOf(
            createMockOfflineNodeInformation(id = 1, name = "file1.txt"),
            createMockOfflineNodeInformation(id = 2, name = "file2.txt"),
        )
        val mockOfflineFileInfo = listOf(
            createMockOfflineFileInformation(name = "file1.txt"),
            createMockOfflineFileInformation(name = "file2.txt"),
        )
        val expectedSortedResult = listOf(
            createMockOfflineFileInformation(name = "file2.txt"),
            createMockOfflineFileInformation(name = "file1.txt"),
        )

        whenever(nodeRepository.getOfflineNodesByParentId(parentId)).thenReturn(mockOfflineNodes)
        whenever(getOfflineFileInformationUseCase(mockOfflineNodes[0], false)).thenReturn(
            mockOfflineFileInfo[0]
        )
        whenever(getOfflineFileInformationUseCase(mockOfflineNodes[1], false)).thenReturn(
            mockOfflineFileInfo[1]
        )
        whenever(sortOfflineInfoUseCase(mockOfflineFileInfo)).thenReturn(expectedSortedResult)

        val result = underTest(parentId)

        assertThat(result).isEqualTo(expectedSortedResult)
    }

    @Test
    fun `test that offline nodes are retrieved by search query when searchQuery is provided`() =
        runTest {
            val parentId = 1
            val searchQuery = "test"
            val mockOfflineNodes = listOf(
                createMockOfflineNodeInformation(id = 1, name = "test_file.txt"),
            )
            val mockOfflineFileInfo = listOf(
                createMockOfflineFileInformation(name = "test_file.txt"),
            )
            val expectedSortedResult = listOf(
                createMockOfflineFileInformation(name = "test_file.txt"),
            )

            whenever(nodeRepository.getOfflineNodesByQuery(searchQuery, parentId)).thenReturn(
                mockOfflineNodes
            )
            whenever(getOfflineFileInformationUseCase(mockOfflineNodes[0], false)).thenReturn(
                mockOfflineFileInfo[0]
            )
            whenever(sortOfflineInfoUseCase(mockOfflineFileInfo)).thenReturn(expectedSortedResult)

            val result = underTest(parentId, searchQuery)

            assertThat(result).isEqualTo(expectedSortedResult)
        }

    @Test
    fun `test that empty list is returned when no offline nodes are found`() = runTest {
        val parentId = 1
        val emptyOfflineNodes = emptyList<OfflineNodeInformation>()
        val emptyOfflineFileInfo = emptyList<OfflineFileInformation>()

        whenever(nodeRepository.getOfflineNodesByParentId(parentId)).thenReturn(emptyOfflineNodes)
        whenever(sortOfflineInfoUseCase(emptyOfflineFileInfo)).thenReturn(emptyOfflineFileInfo)

        val result = underTest(parentId)

        assertThat(result).isEmpty()
    }

    @Test
    fun `test that search query is ignored when it is blank`() = runTest {
        val parentId = 1
        val blankSearchQuery = "   "
        val mockOfflineNodes = listOf(
            createMockOfflineNodeInformation(id = 1, name = "file1.txt"),
        )
        val mockOfflineFileInfo = listOf(
            createMockOfflineFileInformation(name = "file1.txt"),
        )
        val expectedSortedResult = listOf(
            createMockOfflineFileInformation(name = "file1.txt"),
        )

        whenever(nodeRepository.getOfflineNodesByParentId(parentId)).thenReturn(mockOfflineNodes)
        whenever(getOfflineFileInformationUseCase(mockOfflineNodes[0], false)).thenReturn(
            mockOfflineFileInfo[0]
        )
        whenever(sortOfflineInfoUseCase(mockOfflineFileInfo)).thenReturn(expectedSortedResult)

        val result = underTest(parentId, blankSearchQuery)

        assertThat(result).isEqualTo(expectedSortedResult)
    }

    @Test
    fun `test that search query is ignored when it is null`() = runTest {
        val parentId = 1
        val nullSearchQuery: String? = null
        val mockOfflineNodes = listOf(
            createMockOfflineNodeInformation(id = 1, name = "file1.txt"),
        )
        val mockOfflineFileInfo = listOf(
            createMockOfflineFileInformation(name = "file1.txt"),
        )
        val expectedSortedResult = listOf(
            createMockOfflineFileInformation(name = "file1.txt"),
        )

        whenever(nodeRepository.getOfflineNodesByParentId(parentId)).thenReturn(mockOfflineNodes)
        whenever(getOfflineFileInformationUseCase(mockOfflineNodes[0], false)).thenReturn(
            mockOfflineFileInfo[0]
        )
        whenever(sortOfflineInfoUseCase(mockOfflineFileInfo)).thenReturn(expectedSortedResult)

        val result = underTest(parentId, nullSearchQuery)

        assertThat(result).isEqualTo(expectedSortedResult)
    }

    private fun createMockOfflineNodeInformation(
        id: Int = 1,
        name: String = "test_file.txt",
        path: String = "/test/path",
        handle: String = "123456789",
        isFolder: Boolean = false,
        lastModifiedTime: Long? = 1234567890L,
        parentId: Int = 0,
    ): OfflineNodeInformation = OtherOfflineNodeInformation(
        id = id,
        path = path,
        name = name,
        handle = handle,
        isFolder = isFolder,
        lastModifiedTime = lastModifiedTime,
        parentId = parentId,
    )

    private fun createMockOfflineFileInformation(
        name: String = "test_file.txt",
        totalSize: Long = 1024L,
    ): OfflineFileInformation = OfflineFileInformation(
        nodeInfo = createMockOfflineNodeInformation(name = name),
        totalSize = totalSize,
        folderInfo = null,
        fileTypeInfo = null,
        thumbnail = null,
        absolutePath = "/test/path/$name",
    )
}