package mega.privacy.android.domain.usecase.mediaplayer.videoplayer

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.TypedVideoNode
import mega.privacy.android.domain.repository.MediaPlayerRepository
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetVideoNodesByHandlesUseCaseTest {
    lateinit var underTest: GetVideoNodesByHandlesUseCase
    private val mediaPlayerRepository = mock<MediaPlayerRepository>()

    private val handles = listOf(123456L)

    @BeforeAll
    fun setUp() {
        underTest = GetVideoNodesByHandlesUseCase(
            mediaPlayerRepository = mediaPlayerRepository,
        )
    }

    @BeforeEach
    fun resetMock() {
        reset(mediaPlayerRepository)
    }

    @AfterAll
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test that videos is not empty`() = runTest {
        val list = listOf(mock<TypedVideoNode>())
        whenever(mediaPlayerRepository.getVideoNodesByHandles(handles)).thenReturn(list)

        assertThat(underTest(handles)).isNotEmpty()
    }

    @Test
    fun `test that videos is empty`() = runTest {
        whenever(mediaPlayerRepository.getVideoNodesByHandles(handles)).thenReturn(emptyList())

        assertThat(underTest(handles)).isEmpty()
    }
}