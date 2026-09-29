package mega.privacy.android.domain.usecase.chat.giphy

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.chat.Giphy
import mega.privacy.android.domain.repository.chat.GiphyRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetTrendingGiphysUseCaseTest {

    private lateinit var underTest: GetTrendingGiphysUseCase

    private val giphyRepository = mock<GiphyRepository>()

    @BeforeAll
    fun setUp() {
        underTest = GetTrendingGiphysUseCase(
            giphyRepository = giphyRepository,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(giphyRepository)
    }

    @Test
    fun `test that invoke returns trending giphys from repository`() = runTest {
        val expected = listOf(mock<Giphy>(), mock<Giphy>())
        whenever(giphyRepository.getTrendingGiphys(limit = 25, offset = 50)).thenReturn(expected)

        val actual = underTest(limit = 25, offset = 50)

        assertThat(actual).isEqualTo(expected)
    }
}
