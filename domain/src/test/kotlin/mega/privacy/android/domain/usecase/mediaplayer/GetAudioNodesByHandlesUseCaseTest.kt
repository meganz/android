package mega.privacy.android.domain.usecase.mediaplayer

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.TypedAudioNode
import mega.privacy.android.domain.repository.MediaPlayerRepository
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetAudioNodesByHandlesUseCaseTest {
    lateinit var underTest: GetAudioNodesByHandlesUseCase
    private val mediaPlayerRepository = mock<MediaPlayerRepository>()

    private val handles = listOf(123456L)

    @BeforeAll
    fun setUp() {
        underTest = GetAudioNodesByHandlesUseCase(
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
    fun `test that audios is not empty`() = runTest {
        val handles = listOf(123456L)
        val list = listOf(mock<TypedAudioNode>())
        whenever(mediaPlayerRepository.getAudioNodesByHandles(handles)).thenReturn(list)

        assertThat(underTest(handles)).isNotEmpty()
    }

    @Test
    fun `test that audios is empty`() = runTest {
        whenever(mediaPlayerRepository.getAudioNodesByHandles(handles)).thenReturn(emptyList())

        assertThat(underTest(handles)).isEmpty()
    }
}