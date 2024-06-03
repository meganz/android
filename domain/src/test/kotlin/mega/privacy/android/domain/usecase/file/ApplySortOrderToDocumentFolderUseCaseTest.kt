package mega.privacy.android.domain.usecase.file

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.document.DocumentEntity
import mega.privacy.android.domain.entity.document.DocumentFolder
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.usecase.GetOfflineSortOrder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ApplySortOrderToDocumentFolderUseCaseTest {

    private val getOfflineSortOrder: GetOfflineSortOrder =
        Mockito.mock(GetOfflineSortOrder::class.java)
    private val ioDispatcher = UnconfinedTestDispatcher()
    private val underTest = ApplySortOrderToDocumentFolderUseCase(getOfflineSortOrder, ioDispatcher)

    @Test
    fun `test that sort files and folders by name in descending order when sort order is default descending`() =
        runTest {
            val folder = DocumentFolder(entities)
            whenever(getOfflineSortOrder()).thenReturn(SortOrder.ORDER_DEFAULT_DESC)

            val (childFiles, childFolders) = underTest(folder)

            entities.filter { !it.isFolder }.sortedByDescending { item -> item.name }
            entities.filter { it.isFolder }.sortedByDescending { item -> item.name }
            assertEquals(
                entities.filter { !it.isFolder }.sortedByDescending { item -> item.name },
                childFiles
            )
            assertEquals(
                entities.filter { it.isFolder }.sortedByDescending { item -> item.name },
                childFolders
            )
        }

    @Test
    fun `test that sort files by last modified and folders by name when sort order is modification ascending`() =
        runTest {
            val folder = DocumentFolder(entities)
            whenever(getOfflineSortOrder()).thenReturn(SortOrder.ORDER_MODIFICATION_ASC)

            val (childFiles, childFolders) = underTest(folder)

            assertEquals(
                entities.filter { !it.isFolder }.sortedBy { item -> item.lastModified },
                childFiles
            )
            assertEquals(
                entities.filter { it.isFolder }.sortedBy { item -> item.name },
                childFolders
            )
        }

    @Test
    fun `test that sort files by size and folders by name when sort order is size descending`() =
        runTest {
            val folder = DocumentFolder(entities)
            whenever(getOfflineSortOrder()).thenReturn(SortOrder.ORDER_SIZE_DESC)

            val (childFiles, childFolders) = underTest(folder)

            assertEquals(
                entities.filter { !it.isFolder }.sortedByDescending { item -> item.size },
                childFiles
            )
            assertEquals(
                entities.filter { it.isFolder }.sortedBy { item -> item.name },
                childFolders
            )
        }

    @Test
    fun `test that sort files by size and folders by name when sort order is size ascending`() =
        runTest {
            val folder = DocumentFolder(entities)
            whenever(getOfflineSortOrder()).thenReturn(SortOrder.ORDER_SIZE_ASC)

            val (childFiles, childFolders) = underTest(folder)

            assertEquals(
                entities.filter { !it.isFolder }.sortedBy { item -> item.size },
                childFiles
            )
            assertEquals(
                entities.filter { it.isFolder }.sortedBy { item -> item.name },
                childFolders
            )
        }

    companion object {
        private val entities = mutableListOf(
            DocumentEntity(
                name = "A",
                isFolder = true,
                lastModified = 2,
                size = 2,
                numFolders = 2,
                numFiles = 2,
                uri = UriPath("uri")
            ),
            DocumentEntity(
                name = "B",
                isFolder = true,
                lastModified = 2,
                size = 2,
                numFolders = 2,
                numFiles = 2,
                uri = UriPath("uri")
            ),
            DocumentEntity(
                name = "C",
                isFolder = false,
                lastModified = 2,
                size = 2,
                numFolders = 2,
                numFiles = 2,
                uri = UriPath("uri")
            ),
            DocumentEntity(
                name = "D",
                isFolder = false,
                lastModified = 2,
                size = 2,
                numFolders = 2,
                numFiles = 2,
                uri = UriPath("uri")
            ),
        )
    }
}