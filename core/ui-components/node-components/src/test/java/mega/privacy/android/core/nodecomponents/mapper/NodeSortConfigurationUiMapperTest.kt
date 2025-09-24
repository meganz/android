package mega.privacy.android.core.nodecomponents.mapper

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.core.nodecomponents.model.NodeSortConfiguration
import mega.privacy.android.core.nodecomponents.model.NodeSortOption
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.SortDirection
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NodeSortConfigurationUiMapperTest {

    private val underTest = NodeSortConfigurationUiMapper()

    @ParameterizedTest(name = "SortOrder {0} maps to {1}")
    @MethodSource("provideSortOrderToConfigurationParams")
    fun `test that SortOrder maps to correct NodeSortConfiguration`(
        sortOrder: SortOrder,
        expectedConfiguration: NodeSortConfiguration,
    ) {
        assertThat(underTest(sortOrder)).isEqualTo(expectedConfiguration)
    }

    @ParameterizedTest(name = "NodeSortConfiguration {0} maps to {1}")
    @MethodSource("provideConfigurationToSortOrderParams")
    fun `test that NodeSortConfiguration maps to correct SortOrder`(
        configuration: NodeSortConfiguration,
        expectedSortOrder: SortOrder,
    ) {
        assertThat(underTest(configuration)).isEqualTo(expectedSortOrder)
    }

    @Test
    fun `test that unknown SortOrder returns default configuration`() {
        assertThat(underTest(SortOrder.ORDER_NONE))
            .isEqualTo(NodeSortConfiguration(NodeSortOption.Name, SortDirection.Ascending))
    }

    @Test
    fun `test that bidirectional mapping is consistent`() {
        val allSortOrders = listOf(
            SortOrder.ORDER_DEFAULT_ASC,
            SortOrder.ORDER_DEFAULT_DESC,
            SortOrder.ORDER_FAV_ASC,
            SortOrder.ORDER_FAV_DESC,
            SortOrder.ORDER_LABEL_ASC,
            SortOrder.ORDER_LABEL_DESC,
            SortOrder.ORDER_CREATION_ASC,
            SortOrder.ORDER_CREATION_DESC,
            SortOrder.ORDER_MODIFICATION_ASC,
            SortOrder.ORDER_MODIFICATION_DESC,
            SortOrder.ORDER_SIZE_ASC,
            SortOrder.ORDER_SIZE_DESC,
        )

        allSortOrders.forEach { sortOrder ->
            val configuration = underTest(sortOrder)
            val backToSortOrder = underTest(configuration)
            assertThat(backToSortOrder).isEqualTo(sortOrder)
        }
    }

    companion object {
        @JvmStatic
        fun provideSortOrderToConfigurationParams(): Stream<Arguments> = Stream.of(
            Arguments.of(
                SortOrder.ORDER_DEFAULT_ASC,
                NodeSortConfiguration(NodeSortOption.Name, SortDirection.Ascending)
            ),
            Arguments.of(
                SortOrder.ORDER_DEFAULT_DESC,
                NodeSortConfiguration(NodeSortOption.Name, SortDirection.Descending)
            ),
            Arguments.of(
                SortOrder.ORDER_FAV_ASC,
                NodeSortConfiguration(NodeSortOption.Favourite, SortDirection.Ascending)
            ),
            Arguments.of(
                SortOrder.ORDER_FAV_DESC,
                NodeSortConfiguration(NodeSortOption.Favourite, SortDirection.Descending)
            ),
            Arguments.of(
                SortOrder.ORDER_LABEL_ASC,
                NodeSortConfiguration(NodeSortOption.Label, SortDirection.Ascending)
            ),
            Arguments.of(
                SortOrder.ORDER_LABEL_DESC,
                NodeSortConfiguration(NodeSortOption.Label, SortDirection.Descending)
            ),
            Arguments.of(
                SortOrder.ORDER_CREATION_ASC,
                NodeSortConfiguration(NodeSortOption.Created, SortDirection.Ascending)
            ),
            Arguments.of(
                SortOrder.ORDER_CREATION_DESC,
                NodeSortConfiguration(NodeSortOption.Created, SortDirection.Descending)
            ),
            Arguments.of(
                SortOrder.ORDER_MODIFICATION_ASC,
                NodeSortConfiguration(NodeSortOption.Modified, SortDirection.Ascending)
            ),
            Arguments.of(
                SortOrder.ORDER_MODIFICATION_DESC,
                NodeSortConfiguration(NodeSortOption.Modified, SortDirection.Descending)
            ),
            Arguments.of(
                SortOrder.ORDER_SIZE_ASC,
                NodeSortConfiguration(NodeSortOption.Size, SortDirection.Ascending)
            ),
            Arguments.of(
                SortOrder.ORDER_SIZE_DESC,
                NodeSortConfiguration(NodeSortOption.Size, SortDirection.Descending)
            ),
        )

        @JvmStatic
        fun provideConfigurationToSortOrderParams(): Stream<Arguments> = Stream.of(
            Arguments.of(
                NodeSortConfiguration(NodeSortOption.Name, SortDirection.Ascending),
                SortOrder.ORDER_DEFAULT_ASC
            ),
            Arguments.of(
                NodeSortConfiguration(NodeSortOption.Name, SortDirection.Descending),
                SortOrder.ORDER_DEFAULT_DESC
            ),
            Arguments.of(
                NodeSortConfiguration(NodeSortOption.Favourite, SortDirection.Ascending),
                SortOrder.ORDER_FAV_ASC
            ),
            Arguments.of(
                NodeSortConfiguration(NodeSortOption.Favourite, SortDirection.Descending),
                SortOrder.ORDER_FAV_DESC
            ),
            Arguments.of(
                NodeSortConfiguration(NodeSortOption.Label, SortDirection.Ascending),
                SortOrder.ORDER_LABEL_ASC
            ),
            Arguments.of(
                NodeSortConfiguration(NodeSortOption.Label, SortDirection.Descending),
                SortOrder.ORDER_LABEL_DESC
            ),
            Arguments.of(
                NodeSortConfiguration(NodeSortOption.Created, SortDirection.Ascending),
                SortOrder.ORDER_CREATION_ASC
            ),
            Arguments.of(
                NodeSortConfiguration(NodeSortOption.Created, SortDirection.Descending),
                SortOrder.ORDER_CREATION_DESC
            ),
            Arguments.of(
                NodeSortConfiguration(NodeSortOption.Modified, SortDirection.Ascending),
                SortOrder.ORDER_MODIFICATION_ASC
            ),
            Arguments.of(
                NodeSortConfiguration(NodeSortOption.Modified, SortDirection.Descending),
                SortOrder.ORDER_MODIFICATION_DESC
            ),
            Arguments.of(
                NodeSortConfiguration(NodeSortOption.Size, SortDirection.Ascending),
                SortOrder.ORDER_SIZE_ASC
            ),
            Arguments.of(
                NodeSortConfiguration(NodeSortOption.Size, SortDirection.Descending),
                SortOrder.ORDER_SIZE_DESC
            ),
        )
    }
}
