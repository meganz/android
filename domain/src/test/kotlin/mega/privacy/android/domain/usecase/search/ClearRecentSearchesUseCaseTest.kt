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
class ClearRecentSearchesUseCaseTest {
    private lateinit var underTest: ClearRecentSearchesUseCase
    private val searchRepository = mock<SearchRepository>()

    @BeforeAll
    fun setUp() {
        underTest = ClearRecentSearchesUseCase(searchRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(searchRepository)
    }

    @Test
    fun `test that invoke calls repository clearRecentSearches`() = runTest {
        underTest()
        verify(searchRepository).clearRecentSearches()
    }
}

