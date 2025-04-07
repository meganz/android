package mega.privacy.android.domain.usecase.favourites

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.favourite.FavouriteSortOrder
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.FolderNode
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class SortFavouritesUseCaseTest {

    private lateinit var underTest: SortFavouritesUseCase
    private val getFavouriteSortOrderUseCase = mock<GetFavouriteSortOrderUseCase>()

    @BeforeEach
    fun setUp() {
        underTest = SortFavouritesUseCase(getFavouriteSortOrderUseCase)
    }

    @Test
    fun `test that getFavouriteSortOrderUseCase is not called when order is provided`() = runTest {
        val providedOrder = FavouriteSortOrder.ModifiedDate(sortDescending = false)
        val nodes = listOf<FileNode>()

        underTest(nodes, providedOrder)

        verifyNoInteractions(getFavouriteSortOrderUseCase)
    }

    @Test
    fun `test that getFavouriteSortOrderUseCase is called when order is null`() = runTest {
        val defaultOrder = FavouriteSortOrder.Name(sortDescending = false)
        whenever(getFavouriteSortOrderUseCase()).thenReturn(defaultOrder)
        val nodes = listOf<FileNode>()

        underTest(nodes)

        verify(getFavouriteSortOrderUseCase).invoke()
    }

    @Test
    fun `test that file nodes are sorted by label descending`() = runTest {
        // FavouriteSortOrder.Label is always descending.
        val order = FavouriteSortOrder.Label
        val fileA = mock<FileNode> {
            on { name } doReturn "file1"
            on { label } doReturn 1
            on { modificationTime } doReturn 100L
            on { size } doReturn 50L

        }
        val fileB = mock<FileNode> {
            on { name } doReturn "file2"
            on { label } doReturn 2
            on { modificationTime } doReturn 200L
            on { size } doReturn 100L
        }

        val nodes = listOf(fileB, fileA)

        val result = underTest(nodes, order)

        assertThat(result).containsExactly(fileB, fileA).inOrder()
    }

    @Test
    fun `test that file nodes are sorted by modified date ascending`() = runTest {
        val order = FavouriteSortOrder.ModifiedDate(sortDescending = false)
        val fileOld = mock<FileNode> {
            on { name } doReturn "file1"
            on { label } doReturn 12
            on { modificationTime } doReturn 100L
            on { size } doReturn 50L
        }
        val fileNew = mock<FileNode> {
            on { name } doReturn "file2"
            on { label } doReturn 6
            on { modificationTime } doReturn 200L
            on { size } doReturn 100L
        }
        val nodes = listOf(fileNew, fileOld)

        val result = underTest(nodes, order)

        assertThat(result).containsExactly(fileOld, fileNew).inOrder()
    }

    @Test
    fun `test that file nodes are sorted by modified date descending`() = runTest {
        val order = FavouriteSortOrder.ModifiedDate(sortDescending = true)
        val fileOld = mock<FileNode> {
            on { name } doReturn "file1"
            on { label } doReturn 1
            on { modificationTime } doReturn 100L
            on { size } doReturn 50L
        }
        val fileNew = mock<FileNode> {
            on { name } doReturn "file2"
            on { label } doReturn 1
            on { modificationTime } doReturn 200L
            on { size } doReturn 100L
        }
        val nodes = listOf(fileOld, fileNew)

        val result = underTest(nodes, order)

        assertThat(result).containsExactly(fileNew, fileOld).inOrder()
    }

    @Test
    fun `test that file nodes are sorted by name ascending`() = runTest {
        val order = FavouriteSortOrder.Name(sortDescending = false)
        val fileAlpha = mock<FileNode> {
            on { name } doReturn "Alpha"
            on { label } doReturn 1
            on { modificationTime } doReturn 100L
            on { size } doReturn 50L
        }
        val fileBeta = mock<FileNode> {
            on { name } doReturn "Beta"
            on { label } doReturn 1
            on { modificationTime } doReturn 200L
            on { size } doReturn 100L
        }
        val nodes = listOf(fileBeta, fileAlpha)

        val result = underTest(nodes, order)

        assertThat(result).containsExactly(fileAlpha, fileBeta).inOrder()
    }


    @Test
    fun `test that file nodes are sorted by name descending`() = runTest {
        val order = FavouriteSortOrder.Name(sortDescending = true)
        val fileAlpha = mock<FileNode> {
            on { name } doReturn "Alpha"
            on { label } doReturn 2
            on { modificationTime } doReturn 100L
            on { size } doReturn 50L
        }
        val fileBeta = mock<FileNode> {
            on { name } doReturn "Beta"
            on { label } doReturn 2
            on { modificationTime } doReturn 200L
            on { size } doReturn 100L
        }
        val nodes = listOf(fileAlpha, fileBeta)

        val result = underTest(nodes, order)

        assertThat(result).containsExactly(fileBeta, fileAlpha).inOrder()
    }

    @Test
    fun `test that file nodes sorted by size ascending`() = runTest {
        val order = FavouriteSortOrder.Size(sortDescending = false)
        val smallFile = mock<FileNode> {
            on { name } doReturn "file1"
            on { label } doReturn 3
            on { modificationTime } doReturn 100L
            on { size } doReturn 50L
        }
        val bigFile = mock<FileNode> {
            on { name } doReturn "file2"
            on { label } doReturn 3
            on { modificationTime } doReturn 200L
            on { size } doReturn 100L
        }
        val nodes = listOf(bigFile, smallFile)

        val result = underTest(nodes, order)

        assertThat(result).containsExactly(smallFile, bigFile).inOrder()
    }


    @Test
    fun `test that folder nodes are sorted correctly`() = runTest {
        val order = FavouriteSortOrder.Label
        val folder = mock<FolderNode> {
            on { name } doReturn "folder"
            on { label } doReturn 1
        }
        val file = mock<FileNode> {
            on { name } doReturn "file"
            on { label } doReturn 1
            on { modificationTime } doReturn 100L
            on { size } doReturn 50L
        }
        val nodes = listOf(file, folder)

        val result = underTest(nodes, order)

        assertThat(result).containsExactly(folder, file).inOrder()
    }
}