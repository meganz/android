package mega.privacy.android.app.presentation.search.mapper

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.search.SearchCategory
import mega.privacy.android.domain.entity.search.TypeFilterOption
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

/**
 * Test class for [TypeFilterToSearchMapper]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TypeFilterToSearchMapperTest {
    private lateinit var underTest: TypeFilterToSearchMapper

    @BeforeAll
    fun setUp() {
        underTest = TypeFilterToSearchMapper()
    }

    @ParameterizedTest(name = "when filter type is {0}, then the search category is {1}")
    @MethodSource("provideParametersForFilters")
    fun `test that the correct search category is returned for a selected type filter`(
        typeFilterOption: TypeFilterOption?,
        searchCategory: SearchCategory,
    ) {
        assertThat(underTest(typeFilterOption, NodeSourceType.HOME)).isEqualTo(searchCategory)
    }

    @ParameterizedTest(name = "when node source type is {0}, then the search category is {1}")
    @MethodSource("provideParametersForSourceType")
    fun `test that the correct search category is returned for a provided node source type`(
        nodeSourceType: NodeSourceType,
        searchCategory: SearchCategory,
    ) {
        assertThat(underTest(null, nodeSourceType)).isEqualTo(searchCategory)
    }

    private fun provideParametersForFilters() = Stream.of(
        Arguments.of(null, SearchCategory.ALL),
        Arguments.of(TypeFilterOption.Audio, SearchCategory.AUDIO),
        Arguments.of(TypeFilterOption.Video, SearchCategory.VIDEO),
        Arguments.of(TypeFilterOption.Documents, SearchCategory.DOCUMENTS),
        Arguments.of(TypeFilterOption.Images, SearchCategory.IMAGES),
        Arguments.of(TypeFilterOption.Pdf, SearchCategory.PDF),
        Arguments.of(TypeFilterOption.Folder, SearchCategory.FOLDER),
        Arguments.of(TypeFilterOption.Presentation, SearchCategory.PRESENTATION),
        Arguments.of(TypeFilterOption.Spreadsheet, SearchCategory.SPREADSHEET),
        Arguments.of(TypeFilterOption.Other, SearchCategory.OTHER),
    )

    private fun provideParametersForSourceType() = Stream.of(
        Arguments.of(NodeSourceType.FAVOURITES, SearchCategory.FAVOURITES),
        Arguments.of(NodeSourceType.AUDIO, SearchCategory.AUDIO),
        Arguments.of(NodeSourceType.DOCUMENTS, SearchCategory.ALL_DOCUMENTS),
    )
}
