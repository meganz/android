package mega.privacy.android.app.presentation.mapper

import android.view.MenuItem
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.R
import mega.privacy.android.app.domain.usecase.GetNodeByHandle
import mega.privacy.android.app.presentation.clouddrive.OptionItems
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import nz.mega.sdk.MegaNode
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HandleOptionClickMapperTest {

    private lateinit var underTest: HandleOptionClickMapper
    private val getNodeByHandle = mock<GetNodeByHandle>()
    private val getNodeByIdUseCase = mock<GetNodeByIdUseCase>()

    @BeforeEach
    fun setUp() {
        underTest = HandleOptionClickMapper(
            getNodeByHandle = getNodeByHandle,
            getNodeByIdUseCase = getNodeByIdUseCase
        )
    }

    @Test
    fun `test that cab_menu_add_favourites maps to ADD_TO_FAVOURITES_CLICKED`() = runTest {
        // Given
        val menuItem = mock<MenuItem> {
            on { itemId } doReturn R.id.cab_menu_add_favourites
        }
        val selectedNodeHandles = listOf(1L, 2L)
        val mockNode = mock<TypedFileNode>()
        val mockMegaNode = mock<MegaNode>()

        whenever(getNodeByIdUseCase(NodeId(1L))).thenReturn(mockNode)
        whenever(getNodeByIdUseCase(NodeId(2L))).thenReturn(mockNode)
        whenever(getNodeByHandle(1L)).thenReturn(mockMegaNode)
        whenever(getNodeByHandle(2L)).thenReturn(mockMegaNode)

        // When
        val result = underTest(menuItem, selectedNodeHandles)

        // Then
        assertThat(result.optionClickedType).isEqualTo(OptionItems.ADD_TO_FAVOURITES_CLICKED)
        assertThat(result.selectedNode).hasSize(2)
        assertThat(result.selectedMegaNode).hasSize(2)
    }

    @Test
    fun `test that cab_menu_remove_favourites maps to REMOVE_FROM_FAVOURITES_CLICKED`() = runTest {
        // Given
        val menuItem = mock<MenuItem> {
            on { itemId } doReturn R.id.cab_menu_remove_favourites
        }
        val selectedNodeHandles = listOf(1L)
        val mockNode = mock<TypedFileNode>()
        val mockMegaNode = mock<MegaNode>()

        whenever(getNodeByIdUseCase(NodeId(1L))).doReturn(mockNode)
        whenever(getNodeByHandle(1L)).doReturn(mockMegaNode)

        // When
        val result = underTest(menuItem, selectedNodeHandles)

        // Then
        assertThat(result.optionClickedType).isEqualTo(OptionItems.REMOVE_FROM_FAVOURITES_CLICKED)
        assertThat(result.selectedNode).hasSize(1)
        assertThat(result.selectedMegaNode).hasSize(1)
    }

    @Test
    fun `test that unknown menu item maps to CLEAR_ALL_CLICKED`() = runTest {
        // Given
        val menuItem = mock<MenuItem> {
            on { itemId } doReturn 999999 // Unknown menu item ID
        }
        val selectedNodeHandles = emptyList<Long>()

        // When
        val result = underTest(menuItem, selectedNodeHandles)

        // Then
        assertThat(result.optionClickedType).isEqualTo(OptionItems.CLEAR_ALL_CLICKED)
        assertThat(result.selectedNode).isEmpty()
        assertThat(result.selectedMegaNode).isEmpty()
    }

    @Test
    fun `test that existing menu items still map correctly`() = runTest {
        // Given
        val menuItem = mock<MenuItem> {
            on { itemId } doReturn R.id.cab_menu_download
        }
        val selectedNodeHandles = emptyList<Long>()

        // When
        val result = underTest(menuItem, selectedNodeHandles)

        // Then
        assertThat(result.optionClickedType).isEqualTo(OptionItems.DOWNLOAD_CLICKED)
    }

    @Test
    fun `test that null nodes are handled gracefully`() = runTest {
        // Given
        val menuItem = mock<MenuItem> {
            on { itemId } doReturn R.id.cab_menu_add_favourites
        }
        val selectedNodeHandles = listOf(1L, 2L)

        whenever(getNodeByIdUseCase(NodeId(1L))).doReturn(null)
        whenever(getNodeByIdUseCase(NodeId(2L))).doReturn(null)
        whenever(getNodeByHandle(1L)).doReturn(null)
        whenever(getNodeByHandle(2L)).doReturn(null)

        // When
        val result = underTest(menuItem, selectedNodeHandles)

        // Then
        assertThat(result.optionClickedType).isEqualTo(OptionItems.ADD_TO_FAVOURITES_CLICKED)
        assertThat(result.selectedNode).isEmpty() // Null nodes are filtered out
        assertThat(result.selectedMegaNode).isEmpty() // Null nodes are filtered out
    }
}
