package mega.privacy.android.feature.cloudexplorer.presentation.search

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.usecase.search.ClearRecentSearchesUseCase
import mega.privacy.android.domain.usecase.search.MonitorRecentSearchesUseCase
import mega.privacy.android.domain.usecase.search.SaveRecentSearchUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ExplorerSearchViewModelTest {
    private val monitorRecentSearchesUseCase = mock<MonitorRecentSearchesUseCase>()
    private val saveRecentSearchUseCase = mock<SaveRecentSearchUseCase>()
    private val clearRecentSearchesUseCase = mock<ClearRecentSearchesUseCase>()

    @BeforeEach
    fun setUp() {
        whenever(monitorRecentSearchesUseCase()) doReturn emptyFlow()
    }

    @AfterEach
    fun tearDown() {
        reset(monitorRecentSearchesUseCase, saveRecentSearchUseCase, clearRecentSearchesUseCase)
    }

    private fun createViewModel() = ExplorerSearchViewModel(
        monitorRecentSearchesUseCase = monitorRecentSearchesUseCase,
        saveRecentSearchUseCase = saveRecentSearchUseCase,
        clearRecentSearchesUseCase = clearRecentSearchesUseCase,
    )

    @Test
    fun `test that debouncedQuery emits the latest query after the debounce window`() = runTest {
        val underTest = createViewModel()

        underTest.uiState.test {
            underTest.onQueryChanged("d")
            underTest.onQueryChanged("do")
            underTest.onQueryChanged(QUERY)
            advanceUntilIdle()

            val state = expectMostRecentItem() as ExplorerSearchUiState.Data
            assertThat(state.debouncedQuery).isEqualTo(QUERY)
        }
    }

    @Test
    fun `test that uiState exposes the recent searches from the use case`() = runTest {
        whenever(monitorRecentSearchesUseCase()) doReturn flowOf(RECENT_SEARCHES)
        val underTest = createViewModel()

        underTest.uiState.test {
            advanceUntilIdle()
            val state = expectMostRecentItem() as ExplorerSearchUiState.Data
            assertThat(state.recentSearches).containsExactlyElementsIn(RECENT_SEARCHES)
        }
    }

    @Test
    fun `test that saveRecentSearch saves a non-blank query to the use case`() = runTest {
        val underTest = createViewModel()

        underTest.saveRecentSearch(QUERY)
        advanceUntilIdle()

        verify(saveRecentSearchUseCase).invoke(QUERY)
    }

    @Test
    fun `test that saveRecentSearch ignores a blank query`() = runTest {
        val underTest = createViewModel()

        underTest.saveRecentSearch("   ")
        advanceUntilIdle()

        verify(saveRecentSearchUseCase, never()).invoke(any())
    }

    @Test
    fun `test that debouncedQuery is null while the search is closed`() = runTest {
        whenever(monitorRecentSearchesUseCase()) doReturn flowOf(emptyList())
        val underTest = createViewModel()

        underTest.uiState.test {
            underTest.onQueryChanged(QUERY)
            advanceUntilIdle()
            underTest.onQueryChanged(null)
            advanceUntilIdle()

            val state = expectMostRecentItem() as ExplorerSearchUiState.Data
            assertThat(state.debouncedQuery).isNull()
        }
    }

    @Test
    fun `test that clearRecentSearches delegates to the use case`() = runTest {
        val underTest = createViewModel()

        underTest.clearRecentSearches()
        advanceUntilIdle()

        verify(clearRecentSearchesUseCase).invoke()
    }

    companion object {
        private const val QUERY = "doc"
        private val RECENT_SEARCHES = listOf("alpha", "beta")

        private val testDispatcher = StandardTestDispatcher()

        @JvmField
        @RegisterExtension
        val extension = CoroutineMainDispatcherExtension(testDispatcher)
    }
}
