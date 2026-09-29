package mega.privacy.android.data.repository.chat

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.gateway.api.GiphyApi
import mega.privacy.android.data.mapper.chat.giphy.GiphyDataMapper
import mega.privacy.android.data.model.giphy.GiphyData
import mega.privacy.android.data.model.giphy.GiphyResponse
import mega.privacy.android.domain.entity.chat.Giphy
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DefaultGiphyRepositoryTest {

    private lateinit var underTest: DefaultGiphyRepository

    private val giphyApi = mock<GiphyApi>()
    private val giphyDataMapper = mock<GiphyDataMapper>()

    private val giphyData = listOf(mock<GiphyData>())
    private val giphys = listOf(mock<Giphy>())

    @BeforeAll
    fun setUp() {
        underTest = DefaultGiphyRepository(
            giphyApi = giphyApi,
            giphyDataMapper = giphyDataMapper,
            ioDispatcher = UnconfinedTestDispatcher(),
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(giphyApi, giphyDataMapper)
    }

    @Test
    fun `test that getTrendingGiphys returns mapped giphys`() = runTest {
        whenever(giphyApi.getTrending(limit = 25, offset = 50))
            .thenReturn(GiphyResponse(data = giphyData))
        whenever(giphyDataMapper(giphyData)).thenReturn(giphys)

        assertThat(underTest.getTrendingGiphys(limit = 25, offset = 50)).isEqualTo(giphys)
    }

    @Test
    fun `test that getTrendingGiphys returns empty list when body is null`() = runTest {
        whenever(giphyApi.getTrending(limit = 25, offset = 0)).thenReturn(null)
        whenever(giphyDataMapper(emptyList<GiphyData>())).thenReturn(emptyList())

        assertThat(underTest.getTrendingGiphys(limit = 25, offset = 0)).isEmpty()
    }

    @Test
    fun `test that searchGiphys returns mapped giphys`() = runTest {
        whenever(giphyApi.search(query = "cat", limit = 25, offset = 0))
            .thenReturn(GiphyResponse(data = giphyData))
        whenever(giphyDataMapper(giphyData)).thenReturn(giphys)

        assertThat(underTest.searchGiphys(query = "cat", limit = 25, offset = 0)).isEqualTo(giphys)
    }

    @Test
    fun `test that searchGiphys returns empty list when data is null`() = runTest {
        whenever(giphyApi.search(query = "cat", limit = 25, offset = 0))
            .thenReturn(GiphyResponse(data = null))
        whenever(giphyDataMapper(emptyList<GiphyData>())).thenReturn(emptyList())

        assertThat(underTest.searchGiphys(query = "cat", limit = 25, offset = 0)).isEmpty()
    }
}
