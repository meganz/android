package mega.privacy.android.feature.chat.giphy

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.chat.Giphy
import mega.privacy.android.domain.usecase.chat.giphy.GetTrendingGiphysUseCase
import mega.privacy.android.domain.usecase.chat.giphy.SearchGiphysUseCase
import mega.privacy.android.feature.chat.giphy.GiphyPickerViewModel.Companion.PAGE_SIZE
import mega.privacy.android.feature.chat.giphy.model.GiphyEmptyState
import mega.privacy.android.feature.chat.giphy.model.GiphyPickerUiState
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.times
import org.mockito.kotlin.verify

@ExtendWith(CoroutineMainDispatcherExtension::class)
class GiphyPickerViewModelTest {
    private lateinit var underTest: GiphyPickerViewModel

    private val getTrendingGiphysUseCase = mock<GetTrendingGiphysUseCase>()
    private val searchGiphysUseCase = mock<SearchGiphysUseCase>()

    private val firstTrendingPage = gifs(from = 0, count = PAGE_SIZE)
    private val secondTrendingPage = gifs(from = PAGE_SIZE, count = 3)

    @BeforeEach
    fun setUp() {
        getTrendingGiphysUseCase.stub {
            onBlocking { invoke(PAGE_SIZE, 0) } doReturn firstTrendingPage
            onBlocking { invoke(PAGE_SIZE, PAGE_SIZE) } doReturn secondTrendingPage
        }
        underTest = GiphyPickerViewModel(
            getTrendingGiphysUseCase = getTrendingGiphysUseCase,
            searchGiphysUseCase = searchGiphysUseCase,
        )
    }

    @AfterEach
    fun tearDown() {
        reset(
            getTrendingGiphysUseCase,
            searchGiphysUseCase,
        )
    }

    @Test
    fun `test that initial state is loading`() = runTest {
        assertThat(underTest.uiState.value.isLoading).isTrue()
        assertThat(underTest.uiState.value.gifs).isEmpty()
    }

    @Test
    fun `test that uiState emits trending gifs when screen opens`() = runTest {
        underTest.uiState.test {
            val state = awaitLoadedState()

            assertThat(state.gifs).containsExactlyElementsIn(firstTrendingPage).inOrder()
            assertThat(state.isEndOfList).isFalse()
            assertThat(state.emptyState).isNull()
        }
        verify(searchGiphysUseCase, never()).invoke(any(), any(), any())
    }

    @Test
    fun `test that onLoadMore appends the next page`() = runTest {
        underTest.uiState.test {
            awaitLoadedState()

            underTest.onLoadMore()
            val state = awaitLoadedState { it.gifs.size > PAGE_SIZE }

            assertThat(state.gifs)
                .containsExactlyElementsIn(firstTrendingPage + secondTrendingPage)
                .inOrder()
        }
        verify(getTrendingGiphysUseCase).invoke(PAGE_SIZE, PAGE_SIZE)
    }

    @Test
    fun `test that isEndOfList is true when a page has fewer gifs than the page size`() = runTest {
        underTest.uiState.test {
            awaitLoadedState()

            underTest.onLoadMore()
            val state = awaitLoadedState { it.gifs.size > PAGE_SIZE }

            assertThat(state.isEndOfList).isTrue()
        }
    }

    @Test
    fun `test that onLoadMore does not request more gifs when end of list is reached`() = runTest {
        underTest.uiState.test {
            awaitLoadedState()
            underTest.onLoadMore()
            awaitLoadedState { it.isEndOfList }

            underTest.onLoadMore()
            expectNoEvents()
        }
        verify(getTrendingGiphysUseCase, times(2)).invoke(any(), any())
    }

    @Test
    fun `test that onQueryChanged searches the query once it stops changing`() = runTest {
        val results = gifs(from = 100, count = 2)
        searchGiphysUseCase.stub {
            onBlocking { invoke("cat", PAGE_SIZE, 0) } doReturn results
        }

        underTest.uiState.test {
            awaitLoadedState()

            underTest.onQueryChanged("c")
            underTest.onQueryChanged("ca")
            underTest.onQueryChanged("cat")
            val state = awaitLoadedState { it.gifs == results }

            assertThat(state.query).isEqualTo("cat")
            assertThat(state.isEndOfList).isTrue()
        }
        verify(searchGiphysUseCase).invoke("cat", PAGE_SIZE, 0)
        verify(searchGiphysUseCase, never()).invoke(eq("c"), any(), any())
        verify(searchGiphysUseCase, never()).invoke(eq("ca"), any(), any())
    }

    @Test
    fun `test that onQueryChanged reuses loaded gifs when a previous query is searched again`() =
        runTest {
            val results = gifs(from = 100, count = 2)
            searchGiphysUseCase.stub {
                onBlocking { invoke("cat", PAGE_SIZE, 0) } doReturn results
            }

            underTest.uiState.test {
                awaitLoadedState()
                underTest.onQueryChanged("cat")
                awaitLoadedState { it.gifs == results }

                underTest.onQueryChanged("")
                awaitLoadedState { it.gifs == firstTrendingPage }

                underTest.onQueryChanged("cat")
                awaitLoadedState { it.gifs == results }
            }
            verify(searchGiphysUseCase, times(1)).invoke(any(), any(), any())
            verify(getTrendingGiphysUseCase, times(1)).invoke(any(), any())
        }

    @Test
    fun `test that onQueryChanged shows trending gifs when the query is blank`() = runTest {
        underTest.uiState.test {
            awaitLoadedState()

            underTest.onQueryChanged("   ")
            val state = awaitLoadedState { it.query == "   " }

            assertThat(state.gifs).containsExactlyElementsIn(firstTrendingPage).inOrder()
        }
        verify(searchGiphysUseCase, never()).invoke(anyOrNull(), any(), any())
    }

    @Test
    fun `test that emptyState is NoResults when a search returns no gifs`() = runTest {
        searchGiphysUseCase.stub {
            onBlocking { invoke("nothing", PAGE_SIZE, 0) } doReturn emptyList()
        }

        underTest.uiState.test {
            awaitLoadedState()

            underTest.onQueryChanged("nothing")
            val state = awaitLoadedState { it.emptyState != null }

            assertThat(state.emptyState).isEqualTo(GiphyEmptyState.NoResults)
            assertThat(state.gifs).isEmpty()
        }
    }

    @Test
    fun `test that emptyState is ServerDown when trending gifs fail to load`() = runTest {
        getTrendingGiphysUseCase.stub {
            onBlocking { invoke(PAGE_SIZE, 0) } doThrow RuntimeException()
        }

        underTest.uiState.test {
            val state = awaitLoadedState()

            assertThat(state.emptyState).isEqualTo(GiphyEmptyState.ServerDown)
        }
    }

    @Test
    fun `test that loaded gifs are kept when the next page fails to load`() = runTest {
        getTrendingGiphysUseCase.stub {
            onBlocking { invoke(PAGE_SIZE, PAGE_SIZE) } doThrow RuntimeException()
        }

        underTest.uiState.test {
            awaitLoadedState()

            underTest.onLoadMore()
            awaitItem()
            val state = awaitLoadedState()

            assertThat(state.gifs).containsExactlyElementsIn(firstTrendingPage).inOrder()
            assertThat(state.emptyState).isNull()
            assertThat(state.isEndOfList).isFalse()
        }
    }

    private suspend fun ReceiveTurbine<GiphyPickerUiState>.awaitLoadedState(
        predicate: (GiphyPickerUiState) -> Boolean = { true },
    ): GiphyPickerUiState {
        var item = awaitItem()
        while (item.isLoading || !predicate(item)) {
            item = awaitItem()
        }
        return item
    }

    private fun gifs(from: Int, count: Int) = (from until from + count).map {
        Giphy(
            mp4Src = "https://giphy.mega.nz/$it.mp4",
            webpSrc = "https://giphy.mega.nz/$it.webp",
            title = "GIF $it",
            mp4Size = 1000,
            webpSize = 1000,
            width = 200,
            height = 100,
        )
    }
}
