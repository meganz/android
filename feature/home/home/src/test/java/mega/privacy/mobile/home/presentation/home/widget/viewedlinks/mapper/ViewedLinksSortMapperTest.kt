package mega.privacy.mobile.home.presentation.home.widget.viewedlinks.mapper

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.node.SortDirection
import mega.privacy.android.domain.entity.viewedlinks.ViewedLinksSortField
import mega.privacy.android.shared.nodes.model.NodeSortOption
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ViewedLinksSortMapperTest {
    private lateinit var underTest: ViewedLinksSortMapper

    @BeforeEach
    fun setUp() {
        underTest = ViewedLinksSortMapper()
    }

    @Test
    fun `test that NodeSortOption Name maps to ViewedLinksSortField Name`() {
        assertThat(underTest(NodeSortOption.Name)).isEqualTo(ViewedLinksSortField.Name)
    }

    @Test
    fun `test that NodeSortOption LastAccessed maps to ViewedLinksSortField LastAccessed`() {
        assertThat(underTest(NodeSortOption.LastAccessed)).isEqualTo(ViewedLinksSortField.LastAccessed)
    }

    @Test
    fun `test that other NodeSortOption values fall back to LastAccessed`() {
        val fallbacks = listOf(
            NodeSortOption.Favourite,
            NodeSortOption.Label,
            NodeSortOption.Created,
            NodeSortOption.Modified,
            NodeSortOption.Size,
            NodeSortOption.ShareCreated,
            NodeSortOption.LinkCreated,
        )

        fallbacks.forEach { option ->
            assertThat(underTest(option)).isEqualTo(ViewedLinksSortField.LastAccessed)
        }
    }

    @Test
    fun `test that ViewedLinksSortField Name maps to NodeSortOption Name`() {
        val result = underTest(ViewedLinksSortField.Name, SortDirection.Ascending)

        assertThat(result.sortOption).isEqualTo(NodeSortOption.Name)
    }

    @Test
    fun `test that ViewedLinksSortField LastAccessed maps to NodeSortOption LastAccessed`() {
        val result = underTest(ViewedLinksSortField.LastAccessed, SortDirection.Descending)

        assertThat(result.sortOption).isEqualTo(NodeSortOption.LastAccessed)
    }

    @Test
    fun `test that direction is passed through unchanged`() {
        val ascending = underTest(ViewedLinksSortField.Name, SortDirection.Ascending)
        val descending = underTest(ViewedLinksSortField.Name, SortDirection.Descending)

        assertThat(ascending.sortDirection).isEqualTo(SortDirection.Ascending)
        assertThat(descending.sortDirection).isEqualTo(SortDirection.Descending)
    }
}
