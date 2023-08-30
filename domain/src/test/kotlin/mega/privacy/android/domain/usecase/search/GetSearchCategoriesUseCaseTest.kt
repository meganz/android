package mega.privacy.android.domain.usecase.search

import com.google.common.truth.Truth
import mega.privacy.android.domain.entity.search.SearchCategory
import mega.privacy.android.domain.repository.SearchRepository
import org.junit.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetSearchCategoriesUseCaseTest {

    private val searchRepository = mock<SearchRepository>()
    private val underTest = GetSearchCategoriesUseCase(searchRepository)

    @Test
    fun `test that GetSearchCategoriesUseCase returns list of search categories`() {
        val expected = listOf(
            SearchCategory.AUDIO,
            SearchCategory.DOCUMENTS
        )
        whenever(searchRepository.getSearchCategories()).thenReturn(expected)
        val actual = underTest()
        Truth.assertThat(actual).isEqualTo(expected)
    }
}