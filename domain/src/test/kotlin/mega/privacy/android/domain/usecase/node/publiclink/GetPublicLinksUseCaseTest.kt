package mega.privacy.android.domain.usecase.node.publiclink

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.repository.filemanagement.ShareRepository
import mega.privacy.android.domain.usecase.AddNodeType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GetPublicLinksUseCaseTest {
    private val shareRepository = mock<ShareRepository>()
    private val addNodeType = mock<AddNodeType>()

    private val underTest = GetPublicLinksUseCase(
        shareRepository = shareRepository,
        addNodeType = addNodeType,
    )

    @BeforeEach
    fun resetMocks() {
        reset(shareRepository, addNodeType)
    }

    @Test
    fun `test that invoke returns the typed links for the requested sort order`() = runTest {
        val untyped = listOf<FileNode>(mock(), mock())
        val typed = listOf<TypedFileNode>(mock(), mock())
        whenever(shareRepository.getPublicLinks(SortOrder.ORDER_DEFAULT_ASC)).thenReturn(untyped)
        whenever(addNodeType(untyped[0])).thenReturn(typed[0])
        whenever(addNodeType(untyped[1])).thenReturn(typed[1])

        assertThat(underTest(SortOrder.ORDER_DEFAULT_ASC)).containsExactlyElementsIn(typed).inOrder()
    }

    @Test
    fun `test that invoke skips a node that cannot be typed`() = runTest {
        val untyped = listOf<FileNode>(mock(), mock())
        val typed = mock<TypedFileNode>()
        whenever(shareRepository.getPublicLinks(SortOrder.ORDER_DEFAULT_ASC)).thenReturn(untyped)
        whenever(addNodeType(untyped[0])).thenThrow(IllegalStateException("Invalid type"))
        whenever(addNodeType(untyped[1])).thenReturn(typed)

        assertThat(underTest(SortOrder.ORDER_DEFAULT_ASC)).containsExactly(typed)
    }

    @Test
    fun `test that invoke rethrows cancellation instead of typing the remaining links`() = runTest {
        val untyped = listOf<FileNode>(mock(), mock())
        whenever(shareRepository.getPublicLinks(SortOrder.ORDER_DEFAULT_ASC)).thenReturn(untyped)
        whenever(addNodeType(untyped[0])).thenThrow(CancellationException("cancelled"))

        assertThrows<CancellationException> { underTest(SortOrder.ORDER_DEFAULT_ASC) }

        verify(addNodeType, never()).invoke(untyped[1])
    }

    @Test
    fun `test that invoke returns an empty list when there are no links`() = runTest {
        whenever(shareRepository.getPublicLinks(SortOrder.ORDER_DEFAULT_ASC)).thenReturn(emptyList())

        assertThat(underTest(SortOrder.ORDER_DEFAULT_ASC)).isEmpty()
        verifyNoInteractions(addNodeType)
    }
}
