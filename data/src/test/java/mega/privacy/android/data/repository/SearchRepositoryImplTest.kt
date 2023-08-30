package mega.privacy.android.data.repository

import com.google.common.truth.Truth
import mega.privacy.android.data.mapper.search.SearchCategoryMapper
import mega.privacy.android.domain.entity.search.SearchCategory
import mega.privacy.android.domain.repository.SearchRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SearchRepositoryImplTest {
    private lateinit var underTest: SearchRepository


    @BeforeAll
    fun setUp() {
        underTest = SearchRepositoryImpl(SearchCategoryMapper())
    }

    @Test
    fun `test that getSearchCategories returns list of search categories`() {
        val actual = underTest.getSearchCategories()
        Truth.assertThat(actual.sorted()).isEqualTo(SearchCategory.values().toList().sorted())
    }

}