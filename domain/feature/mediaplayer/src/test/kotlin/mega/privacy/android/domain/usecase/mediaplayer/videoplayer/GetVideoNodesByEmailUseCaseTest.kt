package mega.privacy.android.domain.usecase.mediaplayer.videoplayer

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.TypedVideoNode
import mega.privacy.android.domain.repository.MediaPlayerRepository
import mega.privacy.android.domain.usecase.mediaplayer.videoplayer.GetVideoNodesByEmailUseCase
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
class GetVideoNodesByEmailUseCaseTest {
    lateinit var underTest: GetVideoNodesByEmailUseCase
    private val mediaPlayerRepository = mock<MediaPlayerRepository>()

    private val email = "abc@example.com"

    @BeforeAll
    fun setUp() {
        underTest = GetVideoNodesByEmailUseCase(
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
        whenever(mediaPlayerRepository.getVideoNodesByEmail(email)).thenReturn(list)

        assertThat(underTest(email)).isNotEmpty()
    }

    @Test
    fun `test that videos is empty`() = runTest {
        whenever(mediaPlayerRepository.getVideoNodesByEmail(email)).thenReturn(emptyList())

        assertThat(underTest(email)).isEmpty()
    }
}