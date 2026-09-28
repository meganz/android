package mega.privacy.android.domain.usecase.search

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.SearchRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MonitorRecentSearchesUseCaseTest {
    private lateinit var underTest: MonitorRecentSearchesUseCase
    private val searchRepository = mock<SearchRepository>()

    @BeforeAll
    fun setUp() {
        underTest = MonitorRecentSearchesUseCase(searchRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(searchRepository)
    }

    @Test
    fun `test that invoke returns flow from repository`() = runTest {
        val expected = listOf("query1", "query2", "query3")
        whenever(searchRepository.monitorRecentSearches()).thenReturn(flowOf(expected))

        underTest().test {
            assertThat(awaitItem()).isEqualTo(expected)
            cancelAndConsumeRemainingEvents()
        }
    }
}

