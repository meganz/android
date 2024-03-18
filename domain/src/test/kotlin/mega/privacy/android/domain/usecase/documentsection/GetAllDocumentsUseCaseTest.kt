package mega.privacy.android.domain.usecase.documentsection

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.repository.DocumentSectionRepository
import mega.privacy.android.domain.usecase.AddNodeType
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetAllDocumentsUseCaseTest {
    private lateinit var underTest: GetAllDocumentsUseCase

    private val getCloudSortOrder = mock<GetCloudSortOrder>()
    private val documentSectionRepository = mock<DocumentSectionRepository>()
    private val addTypedNode = mock<AddNodeType>()

    val order = SortOrder.ORDER_MODIFICATION_DESC

    @BeforeAll
    fun setUp() {
        underTest = GetAllDocumentsUseCase(
            getCloudSortOrder = getCloudSortOrder,
            documentSectionRepository = documentSectionRepository,
            addTypedNode = addTypedNode
        )
    }

    @BeforeEach
    fun resetMock() {
        reset(
            documentSectionRepository,
            getCloudSortOrder,
            addTypedNode
        )
    }

    @Test
    fun `test that the list of documents is empty`() = runTest {
        whenever(getCloudSortOrder()).thenReturn(order)
        whenever(documentSectionRepository.getAllDocuments(order)).thenReturn(emptyList())
        assertThat(underTest()).isEmpty()
    }

    @Test
    fun `test that the list of documents is not empty`() = runTest {
        val fileNode = mock<FileNode>()
        whenever(addTypedNode(any())).thenReturn(mock<TypedFileNode>())
        whenever(documentSectionRepository.getAllDocuments(order)).thenReturn(
            listOf(fileNode, fileNode)
        )
        whenever(getCloudSortOrder()).thenReturn(order)
        assertThat(underTest()).isNotEmpty()
    }
}