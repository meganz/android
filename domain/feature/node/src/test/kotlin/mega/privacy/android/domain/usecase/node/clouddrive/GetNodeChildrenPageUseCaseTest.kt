package mega.privacy.android.domain.usecase.node.clouddrive

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.FolderTypeData
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodePage
import mega.privacy.android.domain.entity.node.PageRequest
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.search.SensitivityFilterOption
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.usecase.GetFolderTypeDataUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetNodeChildrenPageUseCaseTest {

    private lateinit var underTest: GetNodeChildrenPageUseCase

    private val nodeRepository = mock<NodeRepository>()
    private val getFolderTypeDataUseCase = mock<GetFolderTypeDataUseCase>()

    private val nodeId = NodeId(1L)
    private val page = PageRequest(offset = 500, limit = 500)
    private val expected = NodePage<TypedNode>(nodes = listOf(mock()), nextOffset = 1000, hasMore = true)

    @BeforeAll
    fun setUp() {
        underTest = GetNodeChildrenPageUseCase(
            nodeRepository = nodeRepository,
            getFolderTypeDataUseCase = getFolderTypeDataUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(nodeRepository, getFolderTypeDataUseCase)
    }

    @Test
    fun `test that invoke returns the page from the repository`() = runTest {
        stubPage()

        val actual = underTest(
            nodeId = nodeId,
            sortOrder = SortOrder.ORDER_DEFAULT_ASC,
            excludeSensitives = false,
            page = page,
        )

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test that invoke passes the sort order, the page and the folder type data to the repository`() =
        runTest {
            val folderTypeData = mock<FolderTypeData>()
            stubPage()
            whenever(getFolderTypeDataUseCase()).thenReturn(folderTypeData)

            underTest(
                nodeId = nodeId,
                sortOrder = SortOrder.ORDER_MODIFICATION_DESC,
                excludeSensitives = false,
                page = page,
            )

            verify(nodeRepository).getTypedChildrenPage(
                nodeId = eq(nodeId),
                order = eq(SortOrder.ORDER_MODIFICATION_DESC),
                page = eq(page),
                folderTypeData = eq(folderTypeData),
                sensitivityFilter = isNull(),
            )
        }

    @Test
    fun `test that invoke excludes sensitive nodes through the SDK filter when requested`() = runTest {
        stubPage()

        underTest(
            nodeId = nodeId,
            sortOrder = SortOrder.ORDER_DEFAULT_ASC,
            excludeSensitives = true,
            page = page,
        )

        verify(nodeRepository).getTypedChildrenPage(
            nodeId = any(),
            order = any(),
            page = any(),
            folderTypeData = anyOrNull(),
            sensitivityFilter = eq(SensitivityFilterOption.NonSensitiveOnly),
        )
    }

    private suspend fun stubPage() {
        whenever(getFolderTypeDataUseCase()).thenReturn(mock())
        whenever(
            nodeRepository.getTypedChildrenPage(
                nodeId = any(),
                order = any(),
                page = any(),
                folderTypeData = anyOrNull(),
                sensitivityFilter = anyOrNull(),
            )
        ).thenReturn(expected)
    }
}
