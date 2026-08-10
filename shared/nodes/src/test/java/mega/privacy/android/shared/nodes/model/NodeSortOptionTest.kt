package mega.privacy.android.shared.nodes.model

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.node.SortDirection
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NodeSortOptionTest {

    @ParameterizedTest(name = "{0}.defaultSortDirection is {1}")
    @MethodSource("provideNodeSortOptionDefaultDirections")
    fun `test that defaultSortDirection matches product design`(
        option: NodeSortOption,
        expected: SortDirection,
    ) {
        assertThat(option.defaultSortDirection).isEqualTo(expected)
        val asSortOptionItem: SortOptionItem = option
        assertThat(asSortOptionItem.defaultSortDirection).isEqualTo(expected)
    }

    @Test
    fun `test that NodeSortConfiguration default uses Name defaultSortDirection`() {
        val default = NodeSortConfiguration.default
        assertThat(default.sortOption).isEqualTo(NodeSortOption.Name)
        assertThat(default.sortDirection).isEqualTo(NodeSortOption.Name.defaultSortDirection)
    }

    companion object {
        @JvmStatic
        fun provideNodeSortOptionDefaultDirections(): Stream<Arguments> = Stream.of(
            Arguments.of(NodeSortOption.Name, SortDirection.Ascending),
            Arguments.of(NodeSortOption.Favourite, SortDirection.Ascending),
            Arguments.of(NodeSortOption.Label, SortDirection.Ascending),
            Arguments.of(NodeSortOption.Created, SortDirection.Descending),
            Arguments.of(NodeSortOption.Modified, SortDirection.Descending),
            Arguments.of(NodeSortOption.Size, SortDirection.Descending),
            Arguments.of(NodeSortOption.ShareCreated, SortDirection.Descending),
            Arguments.of(NodeSortOption.LinkCreated, SortDirection.Descending),
        )
    }
}
