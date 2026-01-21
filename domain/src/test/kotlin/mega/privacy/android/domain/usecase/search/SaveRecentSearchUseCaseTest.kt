package mega.privacy.android.domain.usecase.search

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.SearchRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SaveRecentSearchUseCaseTest {
    private lateinit var underTest: SaveRecentSearchUseCase
    private val searchRepository = mock<SearchRepository>()

    @BeforeAll
    fun setUp() {
        underTest = SaveRecentSearchUseCase(searchRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(searchRepository)
    }

    @Test
    fun `test that invoke calls repository saveRecentSearch`() = runTest {
        val query = "test query"
        underTest(query)
        verify(searchRepository).saveRecentSearch(query)
    }
}

